package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import intraer.fablegis.domain.builders.DocumentoBuilder;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.regras.RegrasDeCriacaoDoDocumento;
import org.springframework.stereotype.Component;

// Uma NPA é identificada por um texto livre (ex.: "NPA-AGO-01" ou "NPA 44-__/2026", conforme o setor que a emite):
// não usa assunto básico nem sequencial gerado. O "assunto" que aparece no cabeçalho é o título do documento.
@Component
public class CriacaoDeNpa implements RegrasDeCriacaoDoDocumento {

    static final int TAMANHO_MAXIMO_DA_IDENTIFICACAO = 120;

    @Override
    public Documento montar(DocumentoRequestCreateDto pedido, EspecieNormativa especie, Usuario autor) {
        String identificacao = pedido.identificacao() == null ? "" : pedido.identificacao().strip();
        if (identificacao.isEmpty()) {
            throw new InvalidInputException("Informe a identificação da NPA (ex.: NPA-AGO-01).");
        }
        if (identificacao.length() > TAMANHO_MAXIMO_DA_IDENTIFICACAO) {
            throw new InvalidInputException("A identificação da NPA deve ter no máximo "
                    + TAMANHO_MAXIMO_DA_IDENTIFICACAO + " caracteres.");
        }
        return novo(especie, identificacao, pedido.tituloDocumento(), autor);
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
