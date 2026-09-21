package br.com.danielchipolesch.domain.regras.comunicacaooficialpadronizada;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import br.com.danielchipolesch.domain.builders.DocumentoBuilder;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.InvalidInputException;
import br.com.danielchipolesch.domain.regras.RegrasDeCriacaoDoDocumento;
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
