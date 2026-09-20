package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.regras.EstruturaInicialDeNovoDocumento;
import br.com.danielchipolesch.domain.util.tiptap.ConteudoTipTapComOrientacao;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;

// Estrutura com que uma NPA nasce (layout do Anexo XII da NSCA 5-3): três capítulos, com as seções fixas.
//   1 DISPOSIÇÕES PRELIMINARES: 1.1 Finalidade, 1.2 Âmbito, 1.3 Referências
//   2 DISPOSIÇÕES GERAIS:       2.1 Conceituações
//   3 DISPOSIÇÕES FINAIS:       3.1 (parágrafo direto)
// A alínea só existe depois de um parágrafo, por isso a seção Referências nasce com um parágrafo introdutório
// ("Constituem referências:") seguido da alínea. A numeração não é gravada: é sempre calculada (NumeracaoDeNpa).
// Os textos entre colchetes são orientação de preenchimento e nascem em vermelho (ConteudoTipTapComOrientacao).
@Component
public class EstruturaInicialDeNpa implements EstruturaInicialDeNovoDocumento {

    private final ItemAnexoParteNormativaRepository itemRepository;
    private final ObjectMapper objectMapper;

    public EstruturaInicialDeNpa(ItemAnexoParteNormativaRepository itemRepository, ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.objectMapper = objectMapper;
    }

    private record No(ItemAnexoParteNormativaTipoEnum tipo, String titulo, String texto, List<No> filhos) {}

    private static No capitulo(String titulo, No... filhos) {
        return new No(CAPITULO, titulo, null, List.of(filhos));
    }

    private static No secao(String titulo, No... filhos) {
        return new No(SECAO_NORMATIVA, titulo, null, List.of(filhos));
    }

    private static No paragrafo(String texto, No... alineas) {
        return new No(PARAGRAFO, null, texto, List.of(alineas));
    }

    private static No alinea(String texto) {
        return new No(ALINEA, null, texto, List.of());
    }

    static final List<No> ESTRUTURA = List.of(
            capitulo("DISPOSIÇÕES PRELIMINARES",
                    secao("Finalidade",
                            paragrafo("[Descrever a finalidade desta NPA.]")),
                    secao("Âmbito",
                            paragrafo("[Descrever a quem e a quais situações esta NPA se aplica.]")),
                    secao("Referências",
                            paragrafo("Constituem referências:",
                                    alinea("[Indicar a norma, publicação ou documento de referência.]")))),
            capitulo("DISPOSIÇÕES GERAIS",
                    secao("Conceituações",
                            paragrafo("[Definir os termos e as siglas usados nesta NPA, se necessário.]"))),
            capitulo("DISPOSIÇÕES FINAIS",
                    paragrafo("[Registrar as disposições finais, como a substituição de outra NPA e a solução dos casos não previstos.]")));

    @Override
    public void criarEm(Documento documento) {
        persistir(ESTRUTURA, documento, null);
    }

    private void persistir(List<No> nos, Documento documento, ItemAnexoParteNormativa pai) {
        int ordem = 1;
        for (No no : nos) {
            var item = new ItemAnexoParteNormativa();
            item.setDocumento(documento);
            item.setParent(pai);
            item.setTipo(no.tipo());
            item.setElementOrder(ordem++);
            if (no.texto() != null) {
                item.setConteudo(ConteudoTipTapComOrientacao.de(no.texto(), objectMapper));
                item.setFullTextContent(no.texto());
            } else {
                item.setTitulo(no.titulo());
                item.setFullTextContent(no.titulo());
            }
            persistir(no.filhos(), documento, itemRepository.save(item));
        }
    }
}
