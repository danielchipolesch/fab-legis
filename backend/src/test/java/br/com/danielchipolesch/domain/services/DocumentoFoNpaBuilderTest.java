package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.npaDtos.CamposDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.regras.TipoDeRegras;
import br.com.danielchipolesch.domain.regras.npa.CamposDeNpa;
import br.com.danielchipolesch.domain.regras.npa.NumeracaoDeNpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// PDF da NPA (docs/exportacao-pdf.md): moldura em todas as páginas, tabela de cabeçalho, "n/total" nas páginas 2+,
// numeração pelo caminho (1, 1.1, 1.1.1.1), fecho com "Local, data" e assinaturas em texto livre. Sem portaria,
// capa nem sumário. Sem Spring nem banco: o FOP renderiza a árvore de áreas em XML para conferir o texto.
class DocumentoFoNpaBuilderTest {

    private static final AtomicLong IDS = new AtomicLong(1);

    private DocumentoFoNpaBuilder builder;

    @BeforeEach
    void preparar() {
        var campos = mock(CamposDeNpa.class);
        when(campos.camposParaLeiaute(anyLong())).thenReturn(new CamposDaNpaDto(
                "DIVISÃO DE SUPORTE OPERACIONAL", "Brasília",
                List.of(), null,
                List.of("FULANO DE TAL", "Major Aviador"), List.of("BELTRANO", "Coronel Aviador")));
        builder = new DocumentoFoNpaBuilder(new ObjectMapper(), null, new NumeracaoDeNpa(), campos);
    }

    private static Documento documento(SituacaoBcaEnum bca, SituacaoLocalEnum local) {
        var especie = new EspecieNormativa();
        especie.setSigla("NPA");
        especie.setTipoDeRegras(TipoDeRegras.NPA);
        var om = new OrganizacaoMilitar();
        om.setNome("Grupo de Apoio");
        var doc = new Documento();
        doc.setId(1L);
        doc.setEspecieNormativa(especie);
        doc.setIdentificacao("NPA-AGO-01");
        doc.setTituloDocumento("Funcionamento da Divisão");
        doc.setOm(om);
        doc.setSituacaoBca(bca);
        doc.setSituacaoLocal(local);
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

    private static List<ItemAnexoParteNormativaResponseDto> estruturaPequena() {
        return List.of(
                item(CAPITULO, "DISPOSIÇÕES PRELIMINARES", null,
                        item(SECAO_NORMATIVA, "Finalidade", null,
                                item(PARAGRAFO, null, "Estabelecer os procedimentos de funcionamento.")),
                        item(SECAO_NORMATIVA, "Referências", null,
                                item(PARAGRAFO, null, "Constituem referências:",
                                        item(ALINEA, null, "a Constituição Federal;"),
                                        item(ALINEA, null, "o Decreto 12.002.")))),
                item(CAPITULO, "DISPOSIÇÕES FINAIS", null,
                        item(PARAGRAFO, null, "Os casos omissos serão resolvidos pelo chefe.")));
    }

    private String fo(Documento doc, List<ItemAnexoParteNormativaResponseDto> normativos, List<AnexoResponseDto> anexos) {
        return builder.gerarFo(doc, List.of(), normativos, anexos);
    }

    private static String arvoreDeAreas(String fo) throws Exception {
        var saida = new java.io.ByteArrayOutputStream();
        var fopFactory = FopFactoryProvider.get();
        var fop = fopFactory.newFop(org.apache.fop.apps.MimeConstants.MIME_FOP_AREA_TREE, fopFactory.newFOUserAgent(), saida);
        var spf = javax.xml.parsers.SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        var reader = spf.newSAXParser().getXMLReader();
        reader.setContentHandler(fop.getDefaultHandler());
        reader.parse(new org.xml.sax.InputSource(new java.io.StringReader(fo)));
        return saida.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String textoCorrido(String arvore) {
        return arvore.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
    }

    private static long ocorrencias(String texto, String trecho) {
        return texto.split(java.util.regex.Pattern.quote(trecho), -1).length - 1;
    }

    private static AnexoResponseDto anexo(int ordem, String titulo) {
        return new AnexoResponseDto((long) ordem, titulo, null, ordem);
    }

    private static Documento rascunho() {
        return documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA);
    }

    // ─── Estrutura do documento ─────────────────────────────────────────────────

    @Test
    void naoTemPortariaCapaNemSumario() {
        var fo = fo(documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA), estruturaPequena(), List.of());

        assertThat(fo).doesNotContain("PORTARIA").doesNotContain("SUMÁRIO").doesNotContain("ANEXO I<");
        assertThat(fo.split("<fo:page-sequence ", -1).length - 1).isEqualTo(1);
    }

    @Test
    void cadaAnexoTemSuaPropriaSequenciaDepoisDoCorpo() {
        var fo = fo(rascunho(), estruturaPequena(), List.of(anexo(1, "Organograma"), anexo(2, "Fluxograma")));

        assertThat(fo.split("<fo:page-sequence ", -1).length - 1).isEqualTo(3);
    }

    @Test
    void oCabecalhoTrazTodosOsCamposDoLayout() throws Exception {
        var texto = textoCorrido(arvoreDeAreas(fo(rascunho(), estruturaPequena(), List.of())));

        for (var rotulo : List.of("DATAS", "EMISSÃO", "EFETIVAÇÃO", "DISTRIBUIÇÃO", "ASSUNTO", "ANEXOS")) {
            assertThat(texto).as(rotulo).contains(rotulo);
        }
        assertThat(texto).contains("COMANDO DA AERONÁUTICA").contains("GRUPO DE APOIO")
                .contains("DIVISÃO DE SUPORTE OPERACIONAL").contains("NPA-AGO-01")
                .contains("OSTENSIVA").contains("Funcionamento da Divisão").contains("12 MAR 2026");
    }

    // ─── O cabeçalho é o do modelo (Anexo XII) ──────────────────────────────────────

    @Test
    void asCelulasDoCabecalhoSaoMescladasComoNoModelo() {
        var fo = fo(rascunho(), estruturaPequena(), List.of());

        // Comando/OM/setor ocupam as quatro colunas; DATAS ocupa as duas de EMISSÃO e EFETIVAÇÃO; ASSUNTO e ANEXOS,
        // as três colunas de conteúdo.
        assertThat(ocorrencias(fo, "number-columns-spanned=\"4\"")).isEqualTo(1);
        assertThat(ocorrencias(fo, "number-columns-spanned=\"2\"")).isEqualTo(1);
        assertThat(ocorrencias(fo, "number-columns-spanned=\"3\"")).isEqualTo(2);
        // O DOM ocupa três linhas da primeira coluna e DISTRIBUIÇÃO tem a altura de DATAS + EMISSÃO/EFETIVAÇÃO.
        assertThat(ocorrencias(fo, "number-rows-spanned=\"3\"")).isEqualTo(1);
        assertThat(ocorrencias(fo, "number-rows-spanned=\"2\"")).isEqualTo(1);
    }

    @Test
    void osRotulosFicamAcimaDosValoresECadaValorNaCelulaDoSeuRotulo() throws Exception {
        var doc = documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA);
        doc.setBcaReferencia("Boletim Interno Ostensivo nº 20, de 11 de novembro de 2026");
        doc.setDtBcaReferencia(Timestamp.valueOf("2026-11-11 00:00:00"));

        var texto = textoCorrido(arvoreDeAreas(fo(doc, estruturaPequena(), List.of())));

        // DATAS, depois EMISSÃO e EFETIVAÇÃO (lado a lado, numa linha só de rótulos) e só então os valores.
        var datas = texto.indexOf("DATAS");
        var emissao = texto.indexOf("EMISSÃO");
        var efetivacao = texto.indexOf("EFETIVAÇÃO");
        assertThat(datas).isPositive().isLessThan(emissao);
        assertThat(emissao).isLessThan(efetivacao);
        assertThat(efetivacao).isLessThan(texto.indexOf("12 MAR 2026"));
        assertThat(texto.indexOf("12 MAR 2026")).isLessThan(texto.indexOf("BIO 20"));
        // DISTRIBUIÇÃO é o rótulo de OSTENSIVA, que só aparece na linha dos valores.
        assertThat(texto.indexOf("DISTRIBUIÇÃO")).isLessThan(texto.indexOf("OSTENSIVA"));
    }

    @Test
    void aEfetivacaoTemDuasLinhasOBoletimEADataComoNoModelo() {
        var doc = documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA);
        doc.setBcaReferencia("Boletim Interno Ostensivo nº 20, de 11 de novembro de 2026");
        doc.setDtBcaReferencia(Timestamp.valueOf("2026-11-11 00:00:00"));

        var fo = fo(doc, estruturaPequena(), List.of());

        assertThat(fo).contains("<fo:block text-align=\"center\">BIO 20</fo:block><fo:block text-align=\"center\">11 NOV 2026</fo:block>");
    }

    @Test
    void oAssuntoEOsAnexosSaoJustificadosECadaAnexoFicaNaSuaLinha() {
        var fo = fo(rascunho(), estruturaPequena(), List.of(anexo(1, "Organograma"), anexo(2, "Fluxograma")));

        assertThat(fo).contains("<fo:block text-align=\"justify\">Funcionamento da Divisão</fo:block>");
        assertThat(fo).contains(">A - Organograma; e</fo:block>").contains(">B - Fluxograma.</fo:block>");
        assertThat(ocorrencias(fo, "text-align=\"justify\" space-before=\"2pt\"")).isEqualTo(2);
    }

    @Test
    void aIdentificacaoTemCelulaPropriaSeparadaDoDom() {
        var fo = fo(rascunho(), estruturaPequena(), List.of());

        // O DOM é a célula da primeira coluna; a identificação é um bloco com linha em cima, no pé dessa célula.
        assertThat(fo).contains("<fo:block-container height=\"27.5pt\" border-top=\"0.75pt solid #000000\"");
        assertThat(fo.indexOf("NPA-AGO-01")).isGreaterThan(fo.indexOf("border-top=\"0.75pt solid #000000\""));
    }

    @Test
    void todasAsLinhasDoCabecalhoTemAMesmaEspessuraDaMoldura() {
        var fo = fo(rascunho(), estruturaPequena(), List.of());

        var espessuras = new java.util.TreeSet<String>();
        var m = java.util.regex.Pattern.compile("\\bborder(?:-top|-right|-bottom|-left)?=\"([^\"]+)\"").matcher(fo);
        while (m.find()) espessuras.add(m.group(1));

        // Sem o selo de revogação (vermelho, outra espessura), só existe UMA definição de linha no documento inteiro.
        assertThat(espessuras).containsExactly("0.75pt solid #000000");
    }

    @Test
    void oCabecalhoNaoTemBordaPropriaNoTopoNemNasLateraisPoisSaoAsDaMoldura() {
        var fo = fo(rascunho(), estruturaPequena(), List.of());

        // A moldura é o retângulo da área do corpo, a 2,5 cm das bordas, e o corpo não tem margem própria: a tabela
        // começa exatamente nela. As células só têm borda embaixo e (as que não são da última coluna) à direita.
        assertThat(fo).contains("top=\"71.62pt\" left=\"71.62pt\"");
        assertThat(fo).contains("<fo:region-body region-name=\"xsl-region-body\"/>");
        assertThat(fo).doesNotContain("border-left=").doesNotContain("<fo:table-cell padding=\"3pt\" border=");
    }

    @Test
    void aEfetivacaoSoApareceDepoisDePublicada() throws Exception {
        var naoPublicada = textoCorrido(arvoreDeAreas(fo(rascunho(), estruturaPequena(), List.of())));
        assertThat(naoPublicada).contains("BIO __").doesNotContain("Publicada no");

        var doc = documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA);
        doc.setBcaReferencia("Boletim Interno Ostensivo nº 15, de 2 de abril de 2026");
        doc.setDtBcaReferencia(Timestamp.valueOf("2026-04-02 08:00:00"));
        var publicada = textoCorrido(arvoreDeAreas(fo(doc, estruturaPequena(), List.of())));

        assertThat(publicada).doesNotContain("BIO __")
                .contains("EFETIVAÇÃO").contains("BIO 15").contains("02 ABR 2026")
                .contains("(Publicada no Boletim Interno Ostensivo nº 15, de 2 de abril de 2026)");
    }

    @Test
    void osAnexosSaoListadosNoCabecalhoComLetras() throws Exception {
        var texto = textoCorrido(arvoreDeAreas(fo(rascunho(), estruturaPequena(),
                List.of(anexo(1, "Organograma"), anexo(2, "Fluxograma"), anexo(3, "Quadro")))));

        assertThat(texto).contains("A - Organograma; B - Fluxograma; e C - Quadro.");
    }

    @Test
    void semAnexosOCabecalhoDizNaoHa() throws Exception {
        assertThat(textoCorrido(arvoreDeAreas(fo(rascunho(), estruturaPequena(), List.of())))).contains("NÃO HÁ");
    }

    @Test
    void osAnexosSaoRotuladosComLetras() throws Exception {
        var texto = textoCorrido(arvoreDeAreas(fo(rascunho(), estruturaPequena(), List.of(anexo(1, "Organograma")))));

        assertThat(texto).contains("ANEXO A").doesNotContain("ANEXO II");
    }

    // ─── Numeração e corpo ──────────────────────────────────────────────────────

    @Test
    void osElementosSaoNumeradosPeloCaminhoEAsAlineasPorLetra() throws Exception {
        var texto = textoCorrido(arvoreDeAreas(fo(rascunho(), estruturaPequena(), List.of())));

        assertThat(texto).contains("1 DISPOSIÇÕES PRELIMINARES").contains("1.1 FINALIDADE").contains("1.1.1 Estabelecer")
                .contains("1.2 REFERÊNCIAS").contains("1.2.1 Constituem referências:")
                .contains("a) a Constituição Federal;").contains("b) o Decreto 12.002.")
                .contains("2 DISPOSIÇÕES FINAIS").contains("2.1 Os casos omissos");
        // Nada da numeração dos atos normativos.
        assertThat(texto).doesNotContain("CAPÍTULO").doesNotContain("Art.").doesNotContain("Seção I");
    }

    @Test
    void oFechoTemLocalDataEAssinaturasEmTextoLivre() throws Exception {
        var texto = textoCorrido(arvoreDeAreas(fo(rascunho(), estruturaPequena(), List.of())));

        assertThat(texto).contains("Brasília, 12 de março de 2026")
                .contains("Elaborado por").contains("FULANO DE TAL").contains("Major Aviador")
                .contains("Aprovado por").contains("BELTRANO").contains("Coronel Aviador");
        assertThat(texto.indexOf("Elaborado por")).isLessThan(texto.indexOf("Aprovado por"));
    }

    @Test
    void antesDaAprovacaoAsDatasFicamEmBranco() throws Exception {
        var doc = rascunho();
        doc.setDtAprovacao(null);

        var texto = textoCorrido(arvoreDeAreas(fo(doc, estruturaPequena(), List.of())));

        assertThat(texto).contains("__ ___ ____").contains("Brasília, ___ de __________ de ____");
    }

    @Test
    void oSeloRevogadoAparecesoNaNpaRevogada() {
        assertThat(fo(documento(SituacaoBcaEnum.REVOGADO, SituacaoLocalEnum.SEM_ETAPA), estruturaPequena(), List.of()))
                .contains(">REVOGADO<");
        assertThat(fo(documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA), estruturaPequena(), List.of()))
                .doesNotContain(">REVOGADO<");
    }

    @Test
    void oTextoEmVermelhoDoModeloContinuaVermelho() {
        var vermelho = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"[preencher]\","
                + "\"marks\":[{\"type\":\"textStyle\",\"attrs\":{\"color\":\"#FF0000\"}}]}]}]}";
        var par = new ItemAnexoParteNormativaResponseDto(IDS.getAndIncrement(), null, PARAGRAFO, 1, null, vermelho, null,
                ElementoEmendaStatusEnum.INALTERADO, null, null, null, null, null, null, false, null, null, List.of());
        var fo = fo(rascunho(), List.of(item(CAPITULO, "DISPOSIÇÕES FINAIS", null, par)), List.of());

        assertThat(fo).contains("#FF0000").contains("[preencher]");
    }

    // ─── Moldura e numeração de páginas ─────────────────────────────────────────

    private static List<ItemAnexoParteNormativaResponseDto> estruturaLonga() {
        var paragrafos = new ArrayList<ItemAnexoParteNormativaResponseDto>();
        for (int i = 1; i <= 40; i++) {
            paragrafos.add(item(PARAGRAFO, null, ("Texto do parágrafo número " + i + ". ").repeat(30)));
        }
        return List.of(item(CAPITULO, "DISPOSIÇÕES GERAIS", null, paragrafos.toArray(new ItemAnexoParteNormativaResponseDto[0])));
    }

    @Test
    void aMolduraEABordaDoCorpoERepeteEmTodasAsPaginas() {
        var fo = fo(rascunho(), estruturaLonga(), List.of());

        // Uma moldura (retângulo fixo em conteúdo estático) para a primeira página e outra para as demais -- a última é
        // uma delas --, e nenhuma borda no bloco do conteúdo: a moldura NÃO acompanha o texto, vai até o fim da página.
        assertThat(ocorrencias(fo, "border=\"0.75pt solid #000000\"")).isEqualTo(2);
        assertThat(fo).contains("master-name=\"npa-primeira\"").contains("master-name=\"npa-demais\"");
        assertThat(fo).doesNotContain("npa-ultima").doesNotContain("npa-unica")
                .doesNotContain("page-position=\"last\"").doesNotContain("page-position=\"only\"");
    }

    @Test
    void aMolduraVaiAteOFimDaPaginaEmTodasAsPaginasInclusiveAUltima() throws Exception {
        // As assinaturas é que acabam antes do fim da última página; a moldura, não: ela é sempre o retângulo de página
        // inteira, de 698,66 pt de altura (698660 na árvore de áreas), fixo em conteúdo estático.
        var arvore = arvoreDeAreas(fo(rascunho(), estruturaLonga(), List.of()));
        var paginas = arvore.split("<pageViewport");

        assertThat(paginas.length - 1).isGreaterThanOrEqualTo(3);
        for (int i = 1; i < paginas.length; i++) {
            assertThat(paginas[i]).as("página " + i).contains("bpd=\"698660\"").contains("positioning=\"fixed\"");
        }
        // Nenhum bloco do conteúdo carrega borda embaixo (a moldura não é fechada pelo texto).
        var fechadaPeloTexto = java.util.regex.Pattern.compile(
                "<block ipd=\"4535\\d+\" bpd=\"\\d+\"(?![^>]*positioning=\"fixed\")[^>]*border-after=");
        assertThat(fechadaPeloTexto.matcher(paginas[paginas.length - 1]).find()).isFalse();
    }

    @Test
    void osParagrafosSaoSempreNumeradosEComPrimeiraLinhaRecuada() {
        var fo = fo(rascunho(), estruturaPequena(), List.of());

        // O parágrafo tem primeira linha recuada (1,25 cm) e o número em negrito antes do texto.
        assertThat(fo).contains("start-indent=\"6pt\" text-indent=\"35.4pt\"").contains("<fo:inline font-weight=\"bold\">1.1.1</fo:inline>");
        // Nenhum parágrafo fica sem o número, nem sob "Finalidade" nem sob "Âmbito".
        assertThat(fo).doesNotContain("<fo:inline font-weight=\"bold\"></fo:inline>");
    }

    @Test
    void aAlineaTemALetraPendurada() {
        var fo = fo(rascunho(), estruturaPequena(), List.of());

        assertThat(fo).contains("start-indent=\"94.9pt\" text-indent=\"-17pt\"");
    }

    @Test
    void oTituloDaSecaoSaiEmMaiusculasESublinhado() {
        var fo = fo(rascunho(), estruturaPequena(), List.of());

        assertThat(fo).contains("<fo:inline text-decoration=\"underline\">FINALIDADE</fo:inline>")
                .doesNotContain(">Finalidade</fo:inline>");
    }

    @Test
    void oLocalEDataFicamADireitaEOsRotulosDeAssinaturaAEsquerdaComDoisPontos() {
        var fo = fo(rascunho(), estruturaPequena(), List.of());

        assertThat(fo).contains("<fo:block text-align=\"right\" space-before=\"24pt\" keep-with-next=\"always\">Brasília, 12 de março de 2026");
        assertThat(fo).contains("<fo:block text-align=\"left\" keep-with-next=\"always\">Elaborado por:</fo:block>")
                .contains("<fo:block text-align=\"left\" keep-with-next=\"always\">Aprovado por:</fo:block>");
        // O texto livre da assinatura fica centralizado e sem negrito.
        assertThat(fo).contains("<fo:block text-align=\"center\" space-before=\"30pt\">FULANO DE TAL</fo:block>")
                .contains("<fo:block text-align=\"center\">Major Aviador</fo:block>");
    }

    @Test
    void oNumeroDaPaginaEnDeTotalAparecemDaSegundaEmDiante() throws Exception {
        var arvore = arvoreDeAreas(fo(rascunho(), estruturaLonga(), List.of()));
        long paginas = ocorrencias(arvore, "<pageViewport");
        // "2/5" sai do FOP como palavras separadas ("2", "/", "5"): junta antes de procurar.
        var texto = textoCorrido(arvore).replaceAll(" ?/ ?", "/");

        assertThat(paginas).isGreaterThanOrEqualTo(3);
        for (long p = 2; p <= paginas; p++) {
            assertThat(texto).as("página " + p).contains(p + "/" + paginas);
        }
        // Na primeira página não há número.
        assertThat(texto).doesNotContainPattern("(?<!\\d)1/" + paginas + "(?!\\d)");
    }

    @Test
    void oTotalDePaginasNaoContaOsAnexosDeImagem() throws Exception {
        var arvore = arvoreDeAreas(fo(rascunho(), estruturaPequena(), List.of(anexo(1, "Organograma"))));
        long paginas = ocorrencias(arvore, "<pageViewport");

        // 1 página da NPA + 1 do anexo: nenhuma tem "n/total" (a NPA cabe numa página).
        assertThat(paginas).isEqualTo(2);
        assertThat(textoCorrido(arvore)).doesNotContain("2/2").doesNotContain("1/1");
    }
}
