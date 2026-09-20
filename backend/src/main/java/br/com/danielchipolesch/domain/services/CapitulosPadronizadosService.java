package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.regras.EstruturaInicialDeNovoDocumento;
import br.com.danielchipolesch.domain.util.tiptap.ConteudoTipTapComOrientacao;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.ARTIGO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.CAPITULO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.SECAO_NORMATIVA;

// Capítulos padronizados da NSCA 5-3, Seção X (arts. 63 a 67): todo documento
// novo já nasce com eles na Parte Normativa, igual para toda espécie normativa.
// A numeração (Capítulo I, Seção I, Art. 1º...) não é gravada aqui -- é sempre
// calculada pela estrutura (ver NumeracaoService), então o autor insere os
// capítulos do assunto entre as Disposições Preliminares e as Gerais e tudo se
// renumera sozinho.
@Service
public class CapitulosPadronizadosService implements EstruturaInicialDeNovoDocumento {

    @Autowired
    ItemAnexoParteNormativaRepository itemAnexoParteNormativaRepository;

    @Autowired
    tools.jackson.databind.ObjectMapper objectMapper;

    private record No(ItemAnexoParteNormativaTipoEnum tipo, String titulo, String texto, List<No> filhos) {}

    private static No capitulo(String titulo, No... filhos) {
        return new No(CAPITULO, titulo, null, List.of(filhos));
    }

    private static No secao(String titulo, No... filhos) {
        return new No(SECAO_NORMATIVA, titulo, null, List.of(filhos));
    }

    private static No artigo(String texto) {
        return new No(ARTIGO, null, texto, List.of());
    }

    // Ordem fixa da norma: Preliminares é o primeiro capítulo; Gerais,
    // Transitórias e Finais são o antepenúltimo, o penúltimo e o último. Os
    // textos entre colchetes são orientação de preenchimento, não redação
    // definitiva -- o autor substitui (ou, nos capítulos de aplicação eventual,
    // exclui o capítulo inteiro).
    private static final List<No> ESTRUTURA = List.of(

            // Art. 63 (obrigatório; pelo menos finalidade e âmbito) e art. 64
            // (o âmbito descreve claramente a aplicabilidade da publicação).
            capitulo("DISPOSIÇÕES PRELIMINARES",
                    secao("Finalidade",
                            artigo("{Esta} {especie} tem por finalidade [descrever a finalidade da publicação].")),
                    secao("Âmbito",
                            artigo("{Esta} {especie} aplica-se a [descrever, de forma clara, a quem e a quais situações a publicação se aplica]."))),

            // Art. 65 (aplicação eventual).
            capitulo("DISPOSIÇÕES GERAIS",
                    artigo("[Capítulo de aplicação eventual: exclua-o se não for necessário. Registrar aqui disposições de caráter geral, "
                            + "matéria relacionada com assuntos tratados em mais de um capítulo ou que interessem à publicação como um todo.]")),

            // Art. 66 (aplicação eventual).
            capitulo("DISPOSIÇÕES TRANSITÓRIAS",
                    artigo("[Capítulo de aplicação eventual: exclua-o se não for necessário. Incluir providências condicionadas a eventos futuros, "
                            + "condições a serem cumpridas em prazos determinados e preceitos destinados a perderem vigência, "
                            + "à medida que suas exigências forem sendo atendidas.]")),

            // Art. 67 (obrigatório; seção sobre substituição de publicações e
            // outra para atribuição da solução de casos não previstos).
            capitulo("DISPOSIÇÕES FINAIS",
                    secao("Substituição de publicações",
                            artigo("[Indicar a publicação substituída por esta ou informar que esta publicação não substitui outra.]")),
                    secao("Casos não previstos",
                            artigo("Os casos não previstos {nesta} {especie} serão solucionados por [indicar a autoridade ou o órgão responsável]."))));

    // Nos textos fora dos colchetes, "publicação" vira a espécie do documento
    // criado (ICA: "Esta instrução…"; MCA: "Este manual…"). O substantivo sai do
    // nome cadastrado da espécie, sem o "do Comando da Aeronáutica" comum a
    // todas; o gênero (Esta/Este, nesta/neste) não dá pra deduzir do nome, então
    // fica nas listas abaixo. Espécie fora delas (cadastrada depois) cai em
    // "publicação", que é o termo genérico da própria NSCA 5-3.
    private static final Set<String> ESPECIES_FEMININAS = Set.of("DCA", "ICA", "NSCA", "OCA", "TCA");
    private static final Set<String> ESPECIES_MASCULINAS = Set.of("FCA", "MCA", "PCA", "RCA", "RICA", "ROCA");
    private static final String SUFIXO_NOME_ESPECIE = " do Comando da Aeronáutica";

    private record Denominacao(boolean feminino, String substantivo) {

        static Denominacao de(EspecieNormativa especie) {
            String sigla = especie.getSigla();
            String nome = especie.getNome();
            boolean conhecida = sigla != null && nome != null
                    && (ESPECIES_FEMININAS.contains(sigla) || ESPECIES_MASCULINAS.contains(sigla));
            if (!conhecida) {
                return new Denominacao(true, "publicação");
            }
            return new Denominacao(
                    ESPECIES_FEMININAS.contains(sigla),
                    nome.replace(SUFIXO_NOME_ESPECIE, "").toLowerCase(Locale.of("pt", "BR")));
        }

        String aplicarEm(String texto) {
            return texto
                    .replace("{Esta}", feminino ? "Esta" : "Este")
                    .replace("{nesta}", feminino ? "nesta" : "neste")
                    .replace("{especie}", substantivo);
        }
    }

    @Override
    public void criarEm(Documento documento) {
        persistir(ESTRUTURA, documento, null, Denominacao.de(documento.getEspecieNormativa()));
    }

    private void persistir(List<No> nos, Documento documento, ItemAnexoParteNormativa pai, Denominacao denominacao) {
        int ordem = 1;
        for (No no : nos) {
            var item = new ItemAnexoParteNormativa();
            item.setDocumento(documento);
            item.setParent(pai);
            item.setTipo(no.tipo());
            item.setElementOrder(ordem++);
            if (no.tipo() == ARTIGO) {
                String texto = denominacao.aplicarEm(no.texto());
                item.setConteudo(ConteudoTipTapComOrientacao.de(texto, objectMapper));
                item.setFullTextContent(texto);
            } else {
                item.setTitulo(no.titulo());
                item.setFullTextContent(no.titulo());
            }
            persistir(no.filhos(), documento, itemAnexoParteNormativaRepository.save(item), denominacao);
        }
    }

}
