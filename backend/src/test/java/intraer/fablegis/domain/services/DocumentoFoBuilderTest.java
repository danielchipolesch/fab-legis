package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.OrientacaoDoAnexo;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.numeracaoDocumento.AssuntoBasico;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// A Portaria (1ª página do PDF) só existe depois da 1ª publicação -- ver VersoesDocumento.exibePortaria
// e docs/exportacao-pdf.md. Sem Spring nem FOP: só a estrutura do XSL-FO gerado.
class DocumentoFoBuilderTest {

    private final DocumentoFoBuilder builder = new DocumentoFoBuilder();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(builder, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(builder, "numeracaoService", new NumeracaoService());
    }

    private static Documento documento(SituacaoBcaEnum bca, SituacaoLocalEnum local) {
        var especie = new EspecieNormativa();
        especie.setSigla("ICA");
        especie.setNome("Instrução do Comando da Aeronáutica");
        var assunto = new AssuntoBasico();
        assunto.setCodigo("5");
        assunto.setNome("Publicações");
        var doc = new Documento();
        doc.setId(1L);
        doc.setEspecieNormativa(especie);
        doc.setAssuntoBasico(assunto);
        doc.setNumeroSecundario(3);
        doc.setTituloDocumento("Documento de teste");
        doc.setDtCriacao(Timestamp.valueOf("2026-03-05 10:00:00"));
        var om = new intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar();
        om.setNome("OM de teste");
        doc.setOm(om);
        doc.setSituacaoBca(bca);
        doc.setSituacaoLocal(local);
        return doc;
    }

    private String fo(Documento doc) {
        return builder.buildFo(doc, List.of(), List.of(), List.of());
    }

    private static long sequencias(String fo) {
        return fo.split("<fo:page-sequence ", -1).length - 1;
    }

    @Test
    void naoPublicadoNaoTemAPaginaDaPortaria() {
        for (var local : List.of(SituacaoLocalEnum.RASCUNHO, SituacaoLocalEnum.MINUTA, SituacaoLocalEnum.EM_REVISAO,
                SituacaoLocalEnum.EM_PUBLICACAO)) {
            var fo = fo(documento(SituacaoBcaEnum.NAO_PUBLICADO, local));

            // A epígrafe padrão da Portaria ("PORTARIA Nº ___") não aparece; sobram capa + corpo.
            assertThat(fo).doesNotContain("PORTARIA Nº");
            assertThat(sequencias(fo)).isEqualTo(2);
        }
    }

    @Test
    void publicadoTemAPortariaAntesDaCapa() {
        var fo = fo(documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA));

        assertThat(fo).contains("PORTARIA Nº");
        assertThat(sequencias(fo)).isEqualTo(3);
    }

    @Test
    void aPortariaInicialContinuaDuranteUmaAlteracaoEnoRevogado() {
        assertThat(fo(documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.EM_ALTERACAO))).contains("PORTARIA Nº");
        assertThat(fo(documento(SituacaoBcaEnum.REVOGADO, SituacaoLocalEnum.SEM_ETAPA))).contains("PORTARIA Nº");
    }

    // Espécie Convencional se navega pelo número do artigo/capítulo (já calculado no sumário), não por
    // página -- ao contrário da NPA, que é paginada (ver DocumentoFoNpaBuilderTest). O corpo (ANEXO I,
    // "master-reference=a4") não pode ter <fo:page-number>; a página de portaria/capa também não tem.
    // Um anexo de imagem (ANEXO II+, "master-reference=a4-anexo") continua com número de página --
    // essa regra não muda aqui.
    @Test
    void oCorpoDeEspecieConvencionalNaoTemNumeroDePagina() {
        var fo = fo(documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA));

        // Não "master-reference=\"a4\"" sozinho: essa string também aparece dentro do
        // layout-master-set, no <fo:conditional-page-master-reference> do master a4-anexo (ver
        // buildLayoutMasterSet) -- precisa do elemento fo:page-sequence inteiro para achar certo.
        var inicioCorpo = fo.indexOf("<fo:page-sequence master-reference=\"a4\"");
        assertThat(inicioCorpo).isPositive();
        var corpo = fo.substring(inicioCorpo, fo.indexOf("</fo:page-sequence>", inicioCorpo));

        assertThat(corpo).doesNotContain("<fo:page-number");
    }

    @Test
    void oSeloRevogadoEstaNaPortariaDoDocumentoRevogado() {
        assertThat(fo(documento(SituacaoBcaEnum.REVOGADO, SituacaoLocalEnum.SEM_ETAPA))).contains(">REVOGADO<");
        assertThat(fo(documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA))).doesNotContain(">REVOGADO<");
    }

    // ─── Anexos com mais de uma página: "Continuação do ANEXO X" ─────────────────

    private static String conteudo(String texto) {
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\""
                + texto + "\"}]}]}";
    }

    private static intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto artigo(long id) {
        return new intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto(
                id, null, intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.ARTIGO,
                null, null, conteudo("Texto do artigo ".repeat(40) + id), null,
                intraer.fablegis.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum.INALTERADO,
                null, null, null, null, null, null, false, null, null, List.of());
    }

    // Renderiza o FO no FOP (árvore de áreas em XML): dá para contar páginas e o texto de cada uma
    // sem extrair texto de um PDF.
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

    private static long ocorrencias(String texto, String trecho) {
        return texto.split(java.util.regex.Pattern.quote(trecho), -1).length - 1;
    }

    // As palavras da área de texto ficam separadas em elementos <word>; junta-as para procurar a frase.
    private static String textoCorrido(String arvore) {
        return arvore.replaceAll("<[^>]+>", " ").replaceAll("\s+", " ");
    }

    private static intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto anexo(int ordem, String titulo) {
        return new intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto(1L, titulo, null, ordem);
    }

    // O ANEXO I (sumário + corpo normativo) NUNCA leva "Continuação": a regra vale só do ANEXO II em diante.
    @Test
    void oAnexoIComVariasPaginasNaoMostraContinuacao() throws Exception {
        var artigos = new java.util.ArrayList<intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto>();
        for (long i = 1; i <= 60; i++) artigos.add(artigo(i));
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), artigos, List.of());

        var arvore = arvoreDeAreas(fo);

        assertThat(ocorrencias(arvore, "<pageViewport")).isGreaterThan(3); // capa + várias páginas do anexo I
        assertThat(textoCorrido(arvore)).doesNotContain("Continuação do");
    }

    @Test
    void umAnexoIIComVariasPaginasMostraContinuacaoDaSegundaEmDiante() throws Exception {
        // Título enorme: o texto do anexo passa para a página seguinte (um anexo de imagem só
        // ocupa mais de uma página quando o conteúdo não cabe na primeira).
        var titulo = "PALAVRA ".repeat(900);
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), List.of(artigo(1)), List.of(anexo(1, titulo)));

        var arvore = arvoreDeAreas(fo);
        long paginasDoAnexo = ocorrencias(arvore, "<pageViewport") - 2; // menos capa e ANEXO I (1 página)

        assertThat(paginasDoAnexo).isGreaterThanOrEqualTo(2);
        // Nada na primeira página do anexo; "Continuação do ANEXO II" em TODAS as demais.
        assertThat(ocorrencias(textoCorrido(arvore), "Continuação do ANEXO II")).isEqualTo(paginasDoAnexo - 1);
        assertThat(textoCorrido(arvore)).doesNotContain("Continuação do ANEXO I ");
    }

    @Test
    void umAnexoIIDeUmaSoPaginaNaoMostraContinuacao() throws Exception {
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), List.of(artigo(1)), List.of(anexo(1, "Curto")));

        assertThat(textoCorrido(arvoreDeAreas(fo))).doesNotContain("Continuação do");
    }

    // ─── Imagem de anexo maior que a página: redimensionada para caber, mantendo as proporções ──────────────────

    // PNG sólido (comprime muito pouco espaço, mas tem as dimensões em pixels pedidas) como data URI.
    private static String pngComoDataUri(int largura, int altura) throws Exception {
        var imagem = new java.awt.image.BufferedImage(largura, altura, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var saida = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(imagem, "png", saida);
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(saida.toByteArray());
    }

    private void servirImagem(String dataUri) {
        var imagens = org.mockito.Mockito.mock(ImagemService.class);
        org.mockito.Mockito.when(imagens.getImageAsDataUri(org.mockito.ArgumentMatchers.anyString())).thenReturn(dataUri);
        ReflectionTestUtils.setField(builder, "imagemService", imagens);
    }

    private static intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto anexoComImagem(
            OrientacaoDoAnexo orientacao) {
        return new intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto(
                1L, "Imagem grande", "http://minio/bucket/imagem.png", 1, orientacao);
    }

    // Tamanho (em milésimos de ponto) que a última imagem da árvore de áreas ocupa: <viewport ... pos="x y largura altura">.
    private static long[] tamanhoDaUltimaImagem(String arvore) {
        var m = java.util.regex.Pattern.compile("<viewport[^>]*pos=\"(-?\\d+) (-?\\d+) (\\d+) (\\d+)\"").matcher(arvore);
        long[] tamanho = null;
        while (m.find()) tamanho = new long[]{Long.parseLong(m.group(3)), Long.parseLong(m.group(4))};
        return tamanho;
    }

    private static long cm(double centimetros) { return Math.round(centimetros * 28346.457); } // em milésimos de ponto

    // Capa + ANEXO I + o anexo: a imagem não pode gerar página extra nem sair cortada, e as proporções se mantêm.
    @Test
    void umaImagemAltaEmRetratoEncolheParaCaberEmUmaSoPagina() throws Exception {
        servirImagem(pngComoDataUri(1500, 4500)); // 1:3 -- a 17 cm de largura teria 51 cm de altura
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), List.of(artigo(1)), List.of(anexoComImagem(OrientacaoDoAnexo.RETRATO)));
        var arvore = arvoreDeAreas(fo);

        assertThat(orientacoesDasPaginas(arvore)).containsExactly("RETRATO", "RETRATO", "RETRATO");
        var imagem = tamanhoDaUltimaImagem(arvore);
        assertThat(imagem[1]).isLessThanOrEqualTo(cm(22.5));          // cabe na altura da caixa
        assertThat(imagem[0]).isLessThanOrEqualTo(cm(17));                         // e na largura
        assertThat((double) imagem[0] / imagem[1]).isCloseTo(1.0 / 3.0, within(0.01)); // 1:3 mantido
    }

    @Test
    void umaImagemAltaEmPaisagemEncolheParaCaberEmUmaSoPagina() throws Exception {
        servirImagem(pngComoDataUri(1000, 3000));
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), List.of(artigo(1)), List.of(anexoComImagem(OrientacaoDoAnexo.PAISAGEM)));
        var arvore = arvoreDeAreas(fo);

        assertThat(orientacoesDasPaginas(arvore)).containsExactly("RETRATO", "RETRATO", "PAISAGEM");
        var imagem = tamanhoDaUltimaImagem(arvore);
        assertThat(imagem[1]).isLessThanOrEqualTo(cm(14));
        assertThat(imagem[0]).isLessThanOrEqualTo(cm(25.7));
        assertThat((double) imagem[0] / imagem[1]).isCloseTo(1.0 / 3.0, within(0.01));
    }

    @Test
    void umaImagemLargaEmRetratoEncolheParaALarguraDaCaixa() throws Exception {
        servirImagem(pngComoDataUri(6000, 1000)); // 6:1
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), List.of(artigo(1)), List.of(anexoComImagem(OrientacaoDoAnexo.RETRATO)));
        var arvore = arvoreDeAreas(fo);

        assertThat(orientacoesDasPaginas(arvore)).containsExactly("RETRATO", "RETRATO", "RETRATO");
        var imagem = tamanhoDaUltimaImagem(arvore);
        assertThat(imagem[0]).isLessThanOrEqualTo(cm(17));
        assertThat((double) imagem[0] / imagem[1]).isCloseTo(6.0, within(0.05));
    }

    // ─── Orientação do anexo de imagem (OrientacaoDoAnexo) ─────────────────────────

    private static intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto anexo(
            int ordem, String titulo, OrientacaoDoAnexo orientacao) {
        return new intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto((long) ordem, titulo, null, ordem, orientacao);
    }

    // Tamanho de cada página, na ordem, a partir da árvore de áreas do FOP (atributo bounds="0 0 largura altura").
    private static List<String> orientacoesDasPaginas(String arvore) {
        var orientacoes = new java.util.ArrayList<String>();
        var m = java.util.regex.Pattern.compile("<pageViewport bounds=\"0 0 (\\d+) (\\d+)\"").matcher(arvore);
        while (m.find()) {
            orientacoes.add(Integer.parseInt(m.group(1)) > Integer.parseInt(m.group(2)) ? "PAISAGEM" : "RETRATO");
        }
        return orientacoes;
    }

    // Cada anexo escolhe a orientação da própria página: o PDF mistura as duas no mesmo documento.
    @Test
    void umAnexoEmPaisagemTemPaginaDeitadaEOsDemaisContinuamEmRetrato() throws Exception {
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), List.of(artigo(1)),
                List.of(anexo(1, "Retrato", OrientacaoDoAnexo.RETRATO),
                        anexo(2, "Paisagem", OrientacaoDoAnexo.PAISAGEM),
                        anexo(3, "Outro retrato", OrientacaoDoAnexo.RETRATO)));

        // capa, ANEXO I (sumário + corpo), ANEXO II, ANEXO III, ANEXO IV
        assertThat(orientacoesDasPaginas(arvoreDeAreas(fo)))
                .containsExactly("RETRATO", "RETRATO", "RETRATO", "PAISAGEM", "RETRATO");
    }

    @Test
    void aContinuacaoDeUmAnexoEmPaisagemTambemFicaDeitada() throws Exception {
        var titulo = "PALAVRA ".repeat(900);
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), List.of(artigo(1)), List.of(anexo(1, titulo, OrientacaoDoAnexo.PAISAGEM)));

        var arvore = arvoreDeAreas(fo);
        var paginasDoAnexo = orientacoesDasPaginas(arvore).subList(2, orientacoesDasPaginas(arvore).size()); // sem capa e ANEXO I

        assertThat(paginasDoAnexo).hasSizeGreaterThanOrEqualTo(2).containsOnly("PAISAGEM");
        assertThat(ocorrencias(textoCorrido(arvore), "Continuação do ANEXO II")).isEqualTo(paginasDoAnexo.size() - 1);
    }

    @Test
    void semOrientacaoInformadaOAnexoFicaEmRetrato() throws Exception {
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), List.of(artigo(1)), List.of(anexo(1, "Curto")));

        assertThat(fo).contains("master-reference=\"a4-anexo\"").doesNotContain("master-reference=\"a4-anexo-paisagem\"");
    }

    // A marca d'água de tramitação é centrada na página: em paisagem o centro muda de lugar.
    @Test
    void aMarcaDAguaDoAnexoEmPaisagemFicaNoCentroDaFolhaDeitada() {
        var fo = builder.buildFo(documento(SituacaoBcaEnum.NAO_PUBLICADO, SituacaoLocalEnum.MINUTA),
                List.of(), List.of(artigo(1)), List.of(anexo(1, "Paisagem", OrientacaoDoAnexo.PAISAGEM)));

        assertThat(fo).contains("top=\"421pt\" left=\"211pt\"")   // folha deitada (anexo II)
                .contains("top=\"545pt\" left=\"87pt\"");          // retrato (capa, corpo)
    }
}
