package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Estrutura padrão de novos documentos (NSCA 5-3, arts. 63 a 67): o texto de orientação entre
// colchetes já nasce em vermelho -- só ele, só no modelo. Depois o texto é do autor, que mantém ou
// muda a cor pelo editor; nada é destacado em tempo de renderização (docs/dominio.md).
class CapitulosPadronizadosServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CapitulosPadronizadosService service = new CapitulosPadronizadosService();
    private final List<ItemAnexoParteNormativa> salvos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        var repositorio = Mockito.mock(ItemAnexoParteNormativaRepository.class);
        when(repositorio.save(any())).thenAnswer(inv -> {
            ItemAnexoParteNormativa item = inv.getArgument(0);
            salvos.add(item);
            return item;
        });
        ReflectionTestUtils.setField(service, "itemAnexoParteNormativaRepository", repositorio);
        ReflectionTestUtils.setField(service, "objectMapper", MAPPER);

        var especie = new EspecieNormativa();
        especie.setSigla("ICA");
        especie.setNome("Instrução do Comando da Aeronáutica");
        var doc = new Documento();
        doc.setEspecieNormativa(especie);
        service.criarEm(doc);
    }

    private List<ItemAnexoParteNormativa> artigos() {
        return salvos.stream().filter(i -> i.getTipo() == ItemAnexoParteNormativaTipoEnum.ARTIGO).toList();
    }

    private static List<JsonNode> trechos(ItemAnexoParteNormativa artigo) {
        return MAPPER.readTree(artigo.getConteudo()).get("content").get(0).get("content").valueStream().toList();
    }

    private static boolean vermelho(JsonNode trecho) {
        var marks = trecho.get("marks");
        return marks != null && marks.size() == 1
                && "textStyle".equals(marks.get(0).get("type").asString())
                && "#FF0000".equals(marks.get(0).get("attrs").get("color").asString());
    }

    @Test
    void oTextoEntreColchetesNasceEmVermelhoComOsColchetes() {
        var finalidade = artigos().stream()
                .filter(a -> a.getFullTextContent().contains("tem por finalidade")).findFirst().orElseThrow();

        var trechos = trechos(finalidade);

        assertThat(trechos).hasSize(3);
        assertThat(trechos.get(0).get("text").asString()).isEqualTo("Esta instrução tem por finalidade ");
        assertThat(vermelho(trechos.get(0))).isFalse();
        assertThat(trechos.get(1).get("text").asString()).isEqualTo("[descrever a finalidade da publicação]");
        assertThat(vermelho(trechos.get(1))).isTrue();
        assertThat(trechos.get(2).get("text").asString()).isEqualTo(".");
        assertThat(vermelho(trechos.get(2))).isFalse();
    }

    @Test
    void soOTrechoEntreColchetesEVermelhoNuncaOTextoNormal() {
        for (var artigo : artigos()) {
            for (var trecho : trechos(artigo)) {
                String texto = trecho.get("text").asString();
                boolean entreColchetes = texto.startsWith("[") && texto.endsWith("]");
                assertThat(vermelho(trecho)).as("trecho \"%s\"", texto).isEqualTo(entreColchetes);
            }
        }
    }

    @Test
    void artigoInteiroDeOrientacaoFicaTodoVermelho() {
        var substituicao = artigos().stream()
                .filter(a -> a.getFullTextContent().startsWith("[Indicar a publicação substituída")).findFirst().orElseThrow();

        var trechos = trechos(substituicao);

        assertThat(trechos).hasSize(1);
        assertThat(vermelho(trechos.get(0))).isTrue();
    }

    @Test
    void oTextoSimplesGravadoParaBuscaNaoTemMarcacao() {
        for (var artigo : artigos()) {
            assertThat(artigo.getFullTextContent()).doesNotContain("textStyle").doesNotContain("#FF0000");
        }
    }
}
