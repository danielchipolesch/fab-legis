package intraer.fablegis.domain.util.tiptap;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

// Como uma tabela do editor sai no PDF (XslFoContentRenderer) e no HTML exportado (TipTapHtmlSerializer): as duas leituras vêm
// da mesma GradeDeTabela, então os cenários valem para os dois (docs/exportacao-pdf.md, "Tabelas").
class TabelaTipTapTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String p(String texto, String align) {
        String attrs = align == null ? "" : ",\"attrs\":{\"textAlign\":\"" + align + "\"}";
        return "{\"type\":\"paragraph\"" + attrs + ",\"content\":[{\"type\":\"text\",\"text\":\"" + texto + "\"}]}";
    }

    private static String celula(String tipo, String texto, String attrs, String align) {
        String a = attrs == null ? "" : ",\"attrs\":{" + attrs + "}";
        return "{\"type\":\"" + tipo + "\"" + a + ",\"content\":[" + p(texto, align) + "]}";
    }

    private static String linha(String... celulas) {
        return "{\"type\":\"tableRow\",\"content\":[" + String.join(",", celulas) + "]}";
    }

    private static TipTapNode doc(String... linhas) throws Exception {
        String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"table\",\"content\":[" + String.join(",", linhas) + "]}]}";
        return MAPPER.readValue(json, TipTapNode.class);
    }

    private static String pdf(TipTapNode doc) {
        return new XslFoContentRenderer().renderDocContent(doc);
    }

    private static int conta(String texto, String trecho) {
        return texto.split(java.util.regex.Pattern.quote(trecho), -1).length - 1;
    }

    // ─── Colunas ─────────────────────────────────────────────────────────────

    @Test
    void asColunasVemDaGradeEMesmoComCelulasMescladasNaPrimeiraLinha() throws Exception {
        // 1ª linha: uma célula de colspan 3; 2ª linha: três células. São 3 colunas, não 1.
        var d = doc(
                linha(celula("tableHeader", "Titulo", "\"colspan\":3", null)),
                linha(celula("tableCell", "a", null, null), celula("tableCell", "b", null, null), celula("tableCell", "c", null, null)));

        assertThat(conta(pdf(d), "<fo:table-column")).isEqualTo(3);
    }

    @Test
    void celulaCobertaPorRowspanNaoContaComoColunaNova() throws Exception {
        var d = doc(
                linha(celula("tableCell", "a", "\"rowspan\":2", null), celula("tableCell", "b", null, null)),
                linha(celula("tableCell", "c", null, null)));

        assertThat(conta(pdf(d), "<fo:table-column")).isEqualTo(2);
    }

    @Test
    void larguraArrastadaNoEditorViraProporcaoDaColunaNoPdfENoHtml() throws Exception {
        var d = doc(linha(
                celula("tableCell", "a", "\"colwidth\":[100]", null),
                celula("tableCell", "b", "\"colwidth\":[300]", null)));

        String fo = pdf(d);
        assertThat(fo).contains("proportional-column-width(100.0)").contains("proportional-column-width(300.0)");

        String html = TipTapHtmlSerializer.toHtml(d);
        assertThat(html).contains("<colgroup>").contains("width:25.00%").contains("width:75.00%");
    }

    @Test
    void semLarguraArrastadaAsColunasSaoIguaisESemColgroupNoHtml() throws Exception {
        var d = doc(linha(celula("tableCell", "a", null, null), celula("tableCell", "b", null, null)));

        assertThat(pdf(d)).contains("proportional-column-width(1.0)");
        assertThat(TipTapHtmlSerializer.toHtml(d)).doesNotContain("<colgroup>");
    }

    @Test
    void colunaSemLarguraRecebeAMediaDasArrastadas() throws Exception {
        var d = doc(linha(
                celula("tableCell", "a", "\"colwidth\":[100]", null),
                celula("tableCell", "b", null, null),
                celula("tableCell", "c", "\"colwidth\":[300]", null)));

        assertThat(pdf(d)).contains("proportional-column-width(200.0)");
    }

    // ─── Alinhamento ─────────────────────────────────────────────────────────

    @Test
    void semAlinhamentoEscolhidoCabecalhoCentralizaECorpoJustifica() throws Exception {
        var d = doc(
                linha(celula("tableHeader", "H", null, null)),
                linha(celula("tableCell", "x", null, null)));

        String fo = pdf(d);
        assertThat(fo).contains("text-align=\"center\"").contains("text-align=\"justify\"");
    }

    @Test
    void alinhamentoEscolhidoNoParagrafoDaCelulaVale() throws Exception {
        var d = doc(
                linha(celula("tableHeader", "H", null, "left")),
                linha(celula("tableCell", "x", null, "right")));

        String fo = pdf(d);
        assertThat(fo).contains("text-align=\"left\"").contains("text-align=\"right\"")
                .doesNotContain("text-align=\"center\"").doesNotContain("text-align=\"justify\"");
    }

    // ─── Cabeçalho repetido e linhas inteiras ────────────────────────────────

    @Test
    void cabecalhoDoInicioRepeteEmCadaPaginaENoHtmlVaiNoThead() throws Exception {
        var d = doc(
                linha(celula("tableHeader", "A", null, null), celula("tableHeader", "B", null, null)),
                linha(celula("tableCell", "1", null, null), celula("tableCell", "2", null, null)));

        String fo = pdf(d);
        assertThat(fo).contains("<fo:table-header>");
        assertThat(fo.indexOf("<fo:table-header>")).isLessThan(fo.indexOf("<fo:table-body>"));

        String html = TipTapHtmlSerializer.toHtml(d);
        assertThat(html).contains("<thead>").contains("<tbody>").contains("<th scope=\"col\">");
    }

    @Test
    void tabelaSoDeCabecalhoNaoTemLinhaDeCorpoEPortantoNaoRepeteCabecalho() throws Exception {
        var d = doc(linha(celula("tableHeader", "A", null, null)));

        assertThat(pdf(d)).doesNotContain("<fo:table-header>");
    }

    @Test
    void cabecalhoComRowspanQueAtravessaOLimiteNaoRepete() throws Exception {
        var d = doc(
                linha(celula("tableHeader", "A", "\"rowspan\":2", null), celula("tableHeader", "B", null, null)),
                linha(celula("tableHeader", "C", null, null)),
                linha(celula("tableCell", "1", null, null), celula("tableCell", "2", null, null)));

        // As duas primeiras linhas formam um bloco de cabeçalho fechado (o rowspan não passa da 2ª): repete.
        assertThat(pdf(d)).contains("<fo:table-header>");
        var d2 = doc(
                linha(celula("tableHeader", "A", "\"rowspan\":3", null), celula("tableHeader", "B", null, null)),
                linha(celula("tableHeader", "C", null, null)),
                linha(celula("tableCell", "1", null, null)));
        // Aqui o rowspan de A atravessa o corpo: repetir o cabeçalho partiria a célula.
        assertThat(pdf(d2)).doesNotContain("<fo:table-header>");
    }

    @Test
    void cadaLinhaDoPdfFicaInteiraNaPagina() throws Exception {
        var d = doc(linha(celula("tableCell", "a", null, null)), linha(celula("tableCell", "b", null, null)));

        String fo = pdf(d);
        assertThat(conta(fo, "<fo:table-row keep-together.within-page=\"always\">")).isEqualTo(2);
    }

    // ─── HTML ────────────────────────────────────────────────────────────────

    @Test
    void celulaDeCabecalhoNoCorpoViraCabecalhoDeLinha() throws Exception {
        var d = doc(
                linha(celula("tableHeader", "A", null, null), celula("tableHeader", "B", null, null)),
                linha(celula("tableHeader", "linha 1", null, null), celula("tableCell", "x", null, null)));

        String html = TipTapHtmlSerializer.toHtml(d);
        assertThat(html).contains("scope=\"row\"").contains("scope=\"col\"");
    }

    @Test
    void spansSaemNoHtml() throws Exception {
        var d = doc(linha(celula("tableCell", "a", "\"colspan\":2,\"rowspan\":2", null)));

        assertThat(TipTapHtmlSerializer.toHtml(d)).contains("colspan=\"2\"").contains("rowspan=\"2\"");
    }

    // ─── Legenda: título numerado como as figuras, e fonte ───────────────────

    private static TipTapNode docComTabela(String attrsDaTabela, String... linhas) throws Exception {
        String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"table\",\"attrs\":{" + attrsDaTabela + "},\"content\":["
                + String.join(",", linhas) + "]}]}";
        return MAPPER.readValue(json, TipTapNode.class);
    }

    private static String umaCelula() {
        return linha(celula("tableCell", "x", null, null));
    }

    @Test
    void soATabelaComTituloEhNumeradaEOContadorSegueNoMesmoRenderizador() throws Exception {
        var renderizador = new XslFoContentRenderer();
        var comTitulo = docComTabela("\"titulo\":\"Efetivo por posto\",\"fonte\":\"DIRAP\"", umaCelula());
        var semTitulo = docComTabela("\"titulo\":\"  \",\"fonte\":\"\"", umaCelula());
        var outraComTitulo = docComTabela("\"titulo\":\"Meios aéreos\"", umaCelula());

        String primeira = renderizador.renderDocContent(comTitulo);
        String semLegenda = renderizador.renderDocContent(semTitulo);
        String segunda = renderizador.renderDocContent(outraComTitulo);

        assertThat(primeira).contains("Tabela 1 — Efetivo por posto").contains("Fonte: DIRAP");
        assertThat(semLegenda).doesNotContain("Tabela ").doesNotContain("Fonte:");
        assertThat(segunda).contains("Tabela 2 — Meios aéreos");
    }

    @Test
    void fonteSozinhaApareceSemNumero() throws Exception {
        String fo = pdf(docComTabela("\"fonte\":\"DIRAP\"", umaCelula()));

        assertThat(fo).contains("Fonte: DIRAP").doesNotContain("Tabela ");
    }

    @Test
    void legendaNoHtmlVemEmDivDocTabelaComTituloAcimaEFonteAbaixo() throws Exception {
        String html = TipTapHtmlSerializer.toHtml(docComTabela("\"titulo\":\"Efetivo\",\"fonte\":\"DIRAP\"", umaCelula()));

        assertThat(html).startsWith("<div class=\"doc-tabela\"><p class=\"tabela-titulo\">Efetivo</p><table")
                .endsWith("<p class=\"tabela-fonte\">Fonte: DIRAP</p></div>");
    }

    @Test
    void tituloEFonteSaemComHtmlEscapadoNoHtmlENoPdf() throws Exception {
        var d = docComTabela("\"titulo\":\"A <b> & B\",\"fonte\":\"x<y\"", umaCelula());

        assertThat(TipTapHtmlSerializer.toHtml(d)).contains("A &lt;b&gt; &amp; B").contains("Fonte: x&lt;y");
        assertThat(pdf(d)).contains("A &lt;b&gt; &amp; B").contains("Fonte: x&lt;y");
    }

    // ─── Cor e estilo ────────────────────────────────────────────────────────

    @Test
    void corDaTabelaMudaOFundoDoCabecalhoNoPdfENoDataCorDoHtml() throws Exception {
        var d = docComTabela("\"cor\":\"verde\"", linha(celula("tableHeader", "H", null, null)), umaCelula());

        assertThat(pdf(d)).contains("background-color=\"#e3f0e6\"").contains("1pt solid #8fa896");
        assertThat(TipTapHtmlSerializer.toHtml(d)).contains("data-cor=\"verde\"");
    }

    @Test
    void semCorEscolhidaOuComCorDesconhecidaUsaAAzul() throws Exception {
        var d = docComTabela("\"cor\":\"laranja\"", linha(celula("tableHeader", "H", null, null)), umaCelula());

        assertThat(pdf(d)).contains("background-color=\"#e8edf6\"");
        assertThat(TipTapHtmlSerializer.toHtml(d)).contains("data-cor=\"azul\"").contains("data-estilo=\"padrao\"");
    }

    @Test
    void zebradoPintaALinhaParDoCorpoMasNaoOCabecalhoNemAPrimeira() throws Exception {
        var d = docComTabela("\"estilo\":\"zebrado\"",
                linha(celula("tableHeader", "H", null, null)),
                linha(celula("tableCell", "1", null, null)),
                linha(celula("tableCell", "2", null, null)),
                linha(celula("tableCell", "3", null, null)));

        // corpo: 1 (sem), 2 (zebra), 3 (sem)
        assertThat(conta(pdf(d), "background-color=\"#f4f6fa\"")).isEqualTo(1);
    }

    @Test
    void compactoReduzLetraEMargemEAbertoTiraAsLinhasVerticaisNoPdf() throws Exception {
        String compacto = pdf(docComTabela("\"estilo\":\"compacto\"", umaCelula()));
        assertThat(compacto).contains("padding=\"1pt 3pt\"").contains("font-size=\"8.5pt\"");

        String aberto = pdf(docComTabela("\"estilo\":\"aberto\"", umaCelula()));
        assertThat(aberto).contains("border-top=\"1pt solid #999999\"").contains("border-bottom=\"1pt solid #999999\"")
                .doesNotContain(" border=\"1pt");
    }

    @Test
    void fundoDaCelulaVenceACorDoCabecalhoEAlinhamentoVerticalVaiParaOFoEOHtml() throws Exception {
        var d = doc(linha(celula("tableHeader", "H", "\"fundo\":\"#FFD600\",\"alinhamentoVertical\":\"middle\"", null)),
                umaCelula());

        String fo = pdf(d);
        assertThat(fo).contains("background-color=\"#ffd600\"").contains("display-align=\"center\"");
        assertThat(TipTapHtmlSerializer.toHtml(d)).contains("style=\"background-color:#ffd600;vertical-align:middle;\"");
    }

    @Test
    void fundoQueNaoEhHexadecimalEDescartadoSemVazarParaOMarcador() throws Exception {
        var d = doc(linha(celula("tableCell", "x", "\"fundo\":\"red\\\" onload=\\\"x\",\"alinhamentoVertical\":\"baseline\"", null)));

        String fo = pdf(d);
        assertThat(fo).doesNotContain("red").doesNotContain("onload").contains("display-align=\"before\"");
        assertThat(TipTapHtmlSerializer.toHtml(d)).doesNotContain("onload").doesNotContain("style=");
    }
}
