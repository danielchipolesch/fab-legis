package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
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
}
