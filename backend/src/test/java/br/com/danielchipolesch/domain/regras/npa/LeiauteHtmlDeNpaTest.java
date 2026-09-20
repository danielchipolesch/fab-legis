package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.npaDtos.AssinaturaDaNpaDto;
import br.com.danielchipolesch.application.dtos.npaDtos.CamposDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// HTML da NPA: o mesmo conteúdo do PDF (cabeçalho, corpo numerado pelo caminho, fecho, assinaturas), diagramado para a
// tela, com o texto alinhado à esquerda (NSCA 5-3) e sem "n/total". Ver docs/exportacao-pdf.md.
class LeiauteHtmlDeNpaTest {

    private static final AtomicLong IDS = new AtomicLong(1);

    private LeiauteHtmlDeNpa leiaute;

    @BeforeEach
    void preparar() {
        var campos = mock(CamposDeNpa.class);
        when(campos.camposParaLeiaute(anyLong())).thenReturn(new CamposDaNpaDto(
                "DIVISÃO DE SUPORTE OPERACIONAL", "Brasília",
                List.of(new AssinaturaDaNpaDto("Elaborado por", List.of("FULANO DE TAL", "Major Aviador")),
                        new AssinaturaDaNpaDto("Aprovo", List.of("BELTRANO")))));
        leiaute = new LeiauteHtmlDeNpa(new ObjectMapper(), null, new NumeracaoDeNpa(), campos);
    }

    private static Documento documento(SituacaoBcaEnum bca) {
        var especie = new EspecieNormativa();
        especie.setSigla("NPA");
        var om = new OrganizacaoMilitar();
        om.setNome("Grupo de Apoio");
        var doc = new Documento();
        doc.setId(1L);
        doc.setEspecieNormativa(especie);
        doc.setIdentificacao("NPA-AGO-01");
        doc.setTituloDocumento("Funcionamento <da> Divisão");
        doc.setOm(om);
        doc.setSituacaoBca(bca);
        doc.setSituacaoLocal(SituacaoLocalEnum.SEM_ETAPA);
        doc.setDtAprovacao(Timestamp.valueOf("2026-03-12 10:00:00"));
        return doc;
    }

    private static String conteudo(String texto) {
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\""
                + texto + "\"}]}]}";
    }

    private static ItemAnexoParteNormativaResponseDto item(ItemAnexoParteNormativaTipoEnum tipo, String titulo, String texto,
                                                           ItemAnexoParteNormativaResponseDto... filhos) {
        return new ItemAnexoParteNormativaResponseDto(IDS.getAndIncrement(), null, tipo, 1, titulo,
                texto != null ? conteudo(texto) : null, null, ElementoEmendaStatusEnum.INALTERADO,
                null, null, null, null, null, null, false, null, null, List.of(filhos));
    }

    private static List<ItemAnexoParteNormativaResponseDto> estrutura() {
        return List.of(
                item(CAPITULO, "Disposições Preliminares", null,
                        item(SECAO_NORMATIVA, "Referências", null,
                                item(PARAGRAFO, null, "Constituem referências:",
                                        item(ALINEA, null, "a Constituição Federal;"),
                                        item(ALINEA, null, "o Decreto 12.002.")))),
                item(CAPITULO, "Disposições Finais", null, item(PARAGRAFO, null, "Casos omissos.")));
    }

    private String html(Documento doc, List<AnexoResponseDto> anexos) {
        return leiaute.gerarHtml(doc, List.of(), estrutura(), anexos);
    }

    private String html() {
        return html(documento(SituacaoBcaEnum.NAO_PUBLICADO), List.of());
    }

    @Test
    void oCabecalhoTrazTodosOsCamposDoLayout() {
        var html = html();

        for (var rotulo : List.of("DATAS", "EMISSÃO", "EFETIVAÇÃO", "DISTRIBUIÇÃO", "ASSUNTO", "ANEXOS")) {
            assertThat(html).as(rotulo).contains(rotulo);
        }
        assertThat(html).contains("COMANDO DA AERONÁUTICA").contains("GRUPO DE APOIO")
                .contains("DIVISÃO DE SUPORTE OPERACIONAL").contains("NPA-AGO-01").contains("OSTENSIVA")
                .contains("12/03/2026");
    }

    @Test
    void oTextoDoCabecalhoEEscapado() {
        assertThat(html()).contains("Funcionamento &lt;da&gt; Divisão").doesNotContain("Funcionamento <da>");
    }

    @Test
    void osElementosSaoNumeradosPeloCaminho() {
        var html = html();

        assertThat(html).contains(">1&nbsp;&nbsp;DISPOSIÇÕES PRELIMINARES<")
                .contains("<span class=\"num\">1.1</span>&nbsp;&nbsp;<u>Referências</u>")
                .contains("<span class=\"num\">1.1.1</span>&nbsp;&nbsp;<p>Constituem referências:</p>")
                .contains("<span class=\"num\">a)</span>&nbsp;&nbsp;<p>a Constituição Federal;</p>")
                .contains("<span class=\"num\">b)</span>&nbsp;&nbsp;<p>o Decreto 12.002.</p>")
                .contains(">2&nbsp;&nbsp;DISPOSIÇÕES FINAIS<")
                .contains("<span class=\"num\">2.1</span>&nbsp;&nbsp;<p>Casos omissos.</p>");
        assertThat(html).doesNotContain("CAPÍTULO").doesNotContain("Art.");
    }

    @Test
    void oNumeroEOTextoFicamNaMesmaLinha() {
        // O <p> do TipTap é inline dentro do parágrafo e da alínea, senão o número ficaria sozinho numa linha.
        assertThat(html()).contains(".paragrafo p, .alinea p { display: inline; margin: 0; }");
    }

    @Test
    void oTextoEAlinhadoAEsquerdaComoNosDemaisHtmlDaNsca() {
        assertThat(html()).contains("text-align: left").doesNotContain("text-align: justify");
    }

    @Test
    void naoTemPortariaCapaSumarioNemNumeroDePagina() {
        var html = html(documento(SituacaoBcaEnum.PUBLICADO), List.of());

        assertThat(html).doesNotContain("PORTARIA").doesNotContain("SUMÁRIO").doesNotContain("Continuação");
    }

    @Test
    void oFechoTemLocalDataEAssinaturasEmTextoLivre() {
        var html = html();

        assertThat(html).contains("Brasília, 12 de março de 2026")
                .contains(">Elaborado por<").contains(">FULANO DE TAL<").contains(">Major Aviador<").contains(">Aprovo<");
        assertThat(html.indexOf("Elaborado por")).isLessThan(html.indexOf("Aprovo"));
    }

    @Test
    void aPublicacaoNoBoletimInternoSoApareceDepoisDePublicada() {
        assertThat(html()).doesNotContain("Publicada no").contains("A ser preenchida na publicação");

        var doc = documento(SituacaoBcaEnum.PUBLICADO);
        doc.setBcaReferencia("15");
        doc.setDtBcaReferencia(Timestamp.valueOf("2026-04-02 08:00:00"));
        var html = html(doc, List.of());

        assertThat(html).contains("(Publicada no Boletim Interno Ostensivo nº 15, de 2 de abril de 2026)")
                .doesNotContain("A ser preenchida");
    }

    @Test
    void oSeloRevogadoSoApareceNaNpaRevogada() {
        assertThat(html(documento(SituacaoBcaEnum.REVOGADO), List.of())).contains(">REVOGADO<");
        assertThat(html(documento(SituacaoBcaEnum.PUBLICADO), List.of())).doesNotContain(">REVOGADO<");
    }

    @Test
    void osAnexosSaoListadosNoCabecalhoERotuladosComLetras() {
        var html = html(documento(SituacaoBcaEnum.NAO_PUBLICADO),
                List.of(new AnexoResponseDto(1L, "Organograma", null, 1), new AnexoResponseDto(2L, "Fluxograma", null, 2)));

        assertThat(html).contains("A - Organograma; e B - Fluxograma")
                .contains(">ANEXO A<").contains(">ANEXO B<").doesNotContain("ANEXO II");
    }

    @Test
    void oTextoEmVermelhoDoModeloContinuaVermelho() {
        var vermelho = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"[preencher]\","
                + "\"marks\":[{\"type\":\"textStyle\",\"attrs\":{\"color\":\"#FF0000\"}}]}]}]}";
        var par = new ItemAnexoParteNormativaResponseDto(IDS.getAndIncrement(), null, PARAGRAFO, 1, null, vermelho, null,
                ElementoEmendaStatusEnum.INALTERADO, null, null, null, null, null, null, false, null, null, List.of());

        var html = leiaute.gerarHtml(documento(SituacaoBcaEnum.NAO_PUBLICADO), List.of(),
                List.of(item(CAPITULO, "Finais", null, par)), List.of());

        assertThat(html).containsIgnoringCase("#FF0000").contains("[preencher]");
    }
}
