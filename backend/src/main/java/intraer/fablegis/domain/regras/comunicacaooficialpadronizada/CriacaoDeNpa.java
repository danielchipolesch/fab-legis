package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import intraer.fablegis.domain.builders.DocumentoBuilder;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import intraer.fablegis.domain.regras.RegrasDeCriacaoDoDocumento;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Uma NPA é identificada por um texto livre (ex.: "NPA-AGO-01" ou "NPA 44-__/2026", conforme o setor que a emite):
// não usa assunto básico nem sequencial gerado. O "assunto" que aparece no cabeçalho é o título do documento.
@Component
public class CriacaoDeNpa implements RegrasDeCriacaoDoDocumento {

    static final int TAMANHO_MAXIMO_DA_IDENTIFICACAO = 120;

    @Override
    public Documento montar(DocumentoRequestCreateDto pedido, EspecieNormativa especie, Usuario autor) {
        return novo(especie, identificacaoValida(pedido.identificacao()), pedido.tituloDocumento(), autor);
    }

    // O texto livre pode ser corrigido (nos metadados do editor) enquanto a NPA ainda está em Rascunho ou Minuta -- depois
    // disso ela já circulou (revisão, publicação) com esse código, e ele identifica o que foi publicado.
    @Override
    public Optional<String> novaIdentificacao(Documento documento, String pedida) {
        if (pedida == null) return Optional.empty();
        String nova = identificacaoValida(pedida);
        if (nova.equals(documento.getIdentificacao())) return Optional.empty();
        if (documento.getSituacaoLocal() != SituacaoLocalEnum.RASCUNHO && documento.getSituacaoLocal() != SituacaoLocalEnum.MINUTA) {
            throw new StatusCannotBeUpdatedException(
                    "A identificação da NPA só pode ser alterada enquanto o documento está em Rascunho ou Minuta.");
        }
        return Optional.of(nova);
    }

    private static String identificacaoValida(String informada) {
        String identificacao = informada == null ? "" : informada.strip();
        if (identificacao.isEmpty()) {
            throw new InvalidInputException("Informe a identificação da NPA (ex.: NPA-AGO-01).");
        }
        if (identificacao.length() > TAMANHO_MAXIMO_DA_IDENTIFICACAO) {
            throw new InvalidInputException("A identificação da NPA deve ter no máximo "
                    + TAMANHO_MAXIMO_DA_IDENTIFICACAO + " caracteres.");
        }
        return identificacao;
    }

    @Override
    public Documento montarCopiaDe(Documento original, Usuario autor) {
        return novo(original.getEspecieNormativa(), original.getIdentificacao(), original.getTituloDocumento(), autor);
    }

    private Documento novo(EspecieNormativa especie, String identificacao, String titulo, Usuario autor) {
        return new DocumentoBuilder()
                .especieNormativa(especie)
                .identificacao(identificacao)
                .tituloDocumento(titulo)
                .situacaoLocal(SituacaoLocalEnum.RASCUNHO)
                .autor(autor)
                .om(autor.getOm())
                .build();
    }
}
