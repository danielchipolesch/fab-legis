package br.com.danielchipolesch.domain.util.tiptap;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

// O texto entre colchetes (campo do modelo a preencher) aparece em vermelho no PDF (FO) e no HTML --
// docs/exportacao-pdf.md. A prévia do editor tem a mesma regra em frontend/src/utils/textoModelo.js.
class TextoEntreColchetesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static TipTapNode paragrafo(String textoJson) throws Exception {
        return MAPPER.readValue("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[" + textoJson + "]}]}",
                TipTapNode.class);
    }

    private static String texto(String texto) {
        return "{\"type\":\"text\",\"text\":\"" + texto + "\"}";
    }

    private static String textoComMarca(String texto, String marcaJson) {
        return "{\"type\":\"text\",\"text\":\"" + texto + "\",\"marks\":[" + marcaJson + "]}";
    }

    // ─── Divisão do texto ────────────────────────────────────────────────────────

    @Test
    void divideOTextoMarcandoOsTrechosEntreColchetes() {
        var trechos = TextoEntreColchetes.dividir("Esta ICA tem por finalidade [descrever a finalidade].");

        assertThat(trechos).extracting(TextoEntreColchetes.Trecho::texto)
                .containsExactly("Esta ICA tem por finalidade ", "[descrever a finalidade]", ".");
        assertThat(trechos).extracting(TextoEntreColchetes.Trecho::entreColchetes)
                .containsExactly(false, true, false);
    }

    @Test
    void variosTrechosNoMesmoTexto() {
        var trechos = TextoEntreColchetes.dividir("[a] e [b]");

        assertThat(trechos).extracting(TextoEntreColchetes.Trecho::entreColchetes).containsExactly(true, false, true);
    }

    @Test
    void semColchetesUmUnicoTrechoNormal() {
        assertThat(TextoEntreColchetes.dividir("texto normal")).containsExactly(
                new TextoEntreColchetes.Trecho("texto normal", false));
    }

    @Test
    void colcheteSemParNaoEDestacado() {
        assertThat(TextoEntreColchetes.dividir("abre [ e nunca fecha"))
                .allMatch(t -> !t.entreColchetes());
        assertThat(TextoEntreColchetes.dividir("só fecha ]")).allMatch(t -> !t.entreColchetes());
    }

    @Test
    void textoVazioOuNuloNaoTemTrechos() {
        assertThat(TextoEntreColchetes.dividir("")).isEmpty();
        assertThat(TextoEntreColchetes.dividir(null)).isEmpty();
    }

    // ─── HTML ────────────────────────────────────────────────────────────────────

    @Test
    void noHtmlOTrechoEntreColchetesFicaEmVermelho() throws Exception {
        var html = TipTapHtmlSerializer.toHtml(paragrafo(texto("Finalidade [descrever a finalidade].")));

        assertThat(html).contains("<span style=\"color:#FF0000\">[descrever a finalidade]</span>");
        assertThat(html).startsWith("<p>Finalidade <span").endsWith("</span>.</p>");
    }

    @Test
    void noHtmlTextoSemColchetesNaoMuda() throws Exception {
        assertThat(TipTapHtmlSerializer.toHtml(paragrafo(texto("Texto normal.")))).isEqualTo("<p>Texto normal.</p>");
    }

    @Test
    void noHtmlAFormatacaoDoTrechoEPreservada() throws Exception {
        var html = TipTapHtmlSerializer.toHtml(paragrafo(textoComMarca("[indicar]", "{\"type\":\"bold\"}")));

        assertThat(html).contains("<strong><span style=\"color:#FF0000\">[indicar]</span></strong>");
    }

    @Test
    void noHtmlCorEscolhidaPeloAutorNaoESobrescrita() throws Exception {
        var html = TipTapHtmlSerializer.toHtml(paragrafo(textoComMarca("[azul]",
                "{\"type\":\"textStyle\",\"attrs\":{\"color\":\"#0000FF\"}}")));

        assertThat(html).contains("color:#0000FF").doesNotContain("#FF0000");
    }

    // ─── PDF (XSL-FO) ────────────────────────────────────────────────────────────

    @Test
    void noPdfOTrechoEntreColchetesFicaEmVermelho() throws Exception {
        var fo = new XslFoContentRenderer().renderInlineContent(
                paragrafo(texto("Finalidade [descrever a finalidade].")));

        assertThat(fo).contains("<fo:inline color=\"#FF0000\">[descrever a finalidade]</fo:inline>");
        assertThat(fo).startsWith("Finalidade <fo:inline").endsWith("</fo:inline>.");
    }

    @Test
    void noPdfTextoSemColchetesNaoMuda() throws Exception {
        assertThat(new XslFoContentRenderer().renderInlineContent(paragrafo(texto("Texto normal."))))
                .isEqualTo("Texto normal.");
    }

    @Test
    void noPdfCorEscolhidaPeloAutorNaoESobrescrita() throws Exception {
        var fo = new XslFoContentRenderer().renderInlineContent(paragrafo(textoComMarca("[azul]",
                "{\"type\":\"textStyle\",\"attrs\":{\"color\":\"#0000FF\"}}")));

        assertThat(fo).contains("#0000FF").doesNotContain("#FF0000");
    }
}
