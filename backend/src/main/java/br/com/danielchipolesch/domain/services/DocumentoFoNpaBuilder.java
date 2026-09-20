package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.application.dtos.npaDtos.AssinaturaDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.regras.ElementoNumeracao;
import br.com.danielchipolesch.domain.regras.LeiauteDoPdf;
import br.com.danielchipolesch.domain.regras.npa.CabecalhoDaNpa;
import br.com.danielchipolesch.domain.regras.npa.CamposDeNpa;
import br.com.danielchipolesch.domain.regras.npa.NumeracaoDeNpa;
import br.com.danielchipolesch.domain.regras.npa.RotuloDeAnexoDeNpa;
import br.com.danielchipolesch.domain.util.tiptap.TipTapNode;
import br.com.danielchipolesch.domain.util.tiptap.XslFoContentRenderer;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static br.com.danielchipolesch.domain.services.DocumentoFoContext.foEsc;

// O layout do PDF de uma NPA (Anexo XII da NSCA 5-3): tudo dentro de uma MOLDURA que continua em todas as páginas;
// tabela de cabeçalho na primeira página (DOM e identificação, Comando/OM/setor, datas, distribuição, assunto e
// anexos); "n/total" no alto das páginas 2 em diante; corpo numerado pelo caminho (1, 1.1, 1.1.1.1); fecho com
// "Local, data" e as assinaturas em texto livre; e, se publicada, a linha da publicação no Boletim Interno.
// Não há portaria, capa nem sumário. Os anexos de imagem vêm ao final, como nos atos normativos.
//
// O texto do cabeçalho e do fecho vem de CabecalhoDaNpa, o mesmo que o HTML e a prévia usam.
@Service
public class DocumentoFoNpaBuilder implements LeiauteDoPdf {

    private static final String FO_NS = "http://www.w3.org/1999/XSL/Format";
    // A mesma espessura na moldura e em todas as linhas do cabeçalho.
    private static final String LINHA = "0.75pt solid #000000";

    // O texto fica recuado da moldura (o cabeçalho, não): start-indent é absoluto, então o recuo do texto e o da alínea
    // (o do texto + 1 cm) partem dele.
    private static final String RECUO_DO_TEXTO = "6pt";
    private static final String RECUO_DA_ALINEA = "34.35pt";

    private final ObjectMapper objectMapper;
    private final ImagemService imagemService;
    private final NumeracaoDeNpa numeracao;
    private final CamposDeNpa camposDeNpa;

    public DocumentoFoNpaBuilder(ObjectMapper objectMapper, ImagemService imagemService,
                                 NumeracaoDeNpa numeracao, CamposDeNpa camposDeNpa) {
        this.objectMapper = objectMapper;
        this.imagemService = imagemService;
        this.numeracao = numeracao;
        this.camposDeNpa = camposDeNpa;
    }

    @Override
    public String gerarFo(Documento doc, List<ItemPartePreliminarResponseDto> preliminares,
                          List<ItemAnexoParteNormativaResponseDto> normativos, List<AnexoResponseDto> anexos) {
        List<ItemAnexoParteNormativaResponseDto> normativosSeguro = normativos != null ? normativos : List.of();
        List<AnexoResponseDto> anexosSeguro = anexos != null ? anexos : List.of();

        var renderer = new XslFoContentRenderer();
        if (imagemService != null) {
            renderer.setImageResolver(imagemService::getImageAsDataUri);
        }
        var ctx = new DocumentoFoContext(doc, List.of(), objectMapper, renderer);
        var frontMatter = new DocumentoFoFrontMatterBuilder(ctx, "", "", imagemService, new RotuloDeAnexoDeNpa());
        var cabecalho = CabecalhoDaNpa.de(doc, camposDeNpa.camposParaLeiaute(doc.getId()), anexosSeguro);
        Map<Long, ElementoNumeracao> numeros = numeracao.calcular(normativosSeguro);

        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<fo:root xmlns:fo=\"").append(FO_NS).append("\" xmlns:fox=\"http://xmlgraphics.apache.org/fop/extensions\">\n");
        sb.append(frontMatter.buildLayoutMasterSet(masters()));
        sb.append(sequenciaDaNpa(ctx, doc, cabecalho, normativosSeguro, numeros));
        for (AnexoResponseDto anexo : anexosSeguro) {
            sb.append(frontMatter.buildAnexoSequence(anexo));
        }
        sb.append("</fo:root>");
        return sb.toString();
    }

    // ─── Páginas ──────────────────────────────────────────────────────────────

    // A moldura é um retângulo de posição fixa numa região que não ocupa espaço (o FOP não admite borda na região
    // do corpo): por ser conteúdo estático, repete em todas as páginas. O corpo fica inset em relação a ela. As
    // páginas 2+ reservam um espaço acima da moldura para o "n/total" (a marca d'água vai numa região que não ocupa
    // espaço, como nos anexos).
    private static String masters() {
        return """
              <fo:simple-page-master master-name="npa-primeira"
                  page-width="21cm" page-height="29.7cm"
                  margin-top="2.5cm" margin-bottom="2.5cm" margin-left="2.5cm" margin-right="2.5cm">
                <fo:region-body region-name="xsl-region-body"/>
                <fo:region-before region-name="wm" extent="0pt" overflow="visible"/>
                <fo:region-end region-name="npa-moldura-primeira" extent="0pt" overflow="visible"/>
              </fo:simple-page-master>
              <fo:simple-page-master master-name="npa-demais"
                  page-width="21cm" page-height="29.7cm"
                  margin-top="1.7cm" margin-bottom="2.5cm" margin-left="2.5cm" margin-right="2.5cm">
                <fo:region-body region-name="xsl-region-body" margin-top="0.8cm"/>
                <fo:region-before region-name="npa-numero-da-pagina" extent="0.8cm" display-align="after"/>
                <fo:region-start region-name="wm-continuacao" extent="0pt" overflow="visible"/>
                <fo:region-end region-name="npa-moldura-demais" extent="0pt" overflow="visible"/>
              </fo:simple-page-master>
              <fo:page-sequence-master master-name="npa">
                <fo:repeatable-page-master-alternatives>
                  <fo:conditional-page-master-reference master-reference="npa-primeira" page-position="first"/>
                  <fo:conditional-page-master-reference master-reference="npa-demais" page-position="rest"/>
                </fo:repeatable-page-master-alternatives>
              </fo:page-sequence-master>
            """;
    }

    // A moldura é o retângulo da área do corpo: 16 x 24,7 cm, a 2,5 cm das bordas, igual em todas as páginas (o "n/total"
    // fica na margem de cima, acima dela). A largura e a altura descontam a espessura da borda, que o FOP soma por fora.
    private static String moldura(String regiao) {
        return "<fo:static-content flow-name=\"" + regiao + "\">\n"
                + "  <fo:block-container absolute-position=\"fixed\" top=\"2.5cm\" left=\"2.5cm\" width=\"452.04pt\" height=\"698.66pt\""
                + " border=\"" + LINHA + "\"><fo:block/></fo:block-container>\n"
                + "</fo:static-content>\n";
    }

    private String sequenciaDaNpa(DocumentoFoContext ctx, Documento doc, CabecalhoDaNpa cabecalho,
                                  List<ItemAnexoParteNormativaResponseDto> normativos,
                                  Map<Long, ElementoNumeracao> numeros) {
        var sb = new StringBuilder();
        sb.append("<fo:page-sequence master-reference=\"npa\" font-family=\"Calibri\" font-size=\"11pt\">\n");
        sb.append("<fo:static-content flow-name=\"npa-numero-da-pagina\">\n")
          .append("  <fo:block text-align=\"right\" font-size=\"10pt\"><fo:page-number/>/")
          .append("<fo:page-number-citation-last ref-id=\"npa-fim\"/></fo:block>\n")
          .append("</fo:static-content>\n");
        sb.append(moldura("npa-moldura-primeira"));
        sb.append(moldura("npa-moldura-demais"));
        sb.append(ctx.buildStaticContentWatermark());
        sb.append(ctx.buildStaticContentWatermark("wm-continuacao"));
        sb.append("<fo:flow flow-name=\"xsl-region-body\">\n");

        // Revogada: selo vermelho no canto superior direito da primeira página (nenhum elemento é tachado).
        if (VersoesDocumento.exibeSeloRevogado(doc)) {
            sb.append("<fo:block-container absolute-position=\"absolute\" top=\"0cm\" right=\"0cm\" width=\"3.6cm\"")
              .append(" border=\"1.5pt solid #C00000\" padding=\"3pt\">")
              .append("<fo:block text-align=\"center\" font-size=\"14pt\" font-weight=\"bold\" color=\"#C00000\">REVOGADO</fo:block>")
              .append("</fo:block-container>\n");
        }

        sb.append(tabelaDoCabecalho(cabecalho));
        // O texto fica um pouco recuado da moldura; o cabeçalho, não (suas bordas são as da moldura).
        sb.append("<fo:block start-indent=\"6pt\" end-indent=\"6pt\" space-before=\"8pt\">\n");
        for (var item : normativos) renderizar(ctx, item, numeros, sb);
        sb.append(fecho(cabecalho));
        sb.append("</fo:block>\n");
        sb.append("<fo:block id=\"npa-fim\"/>\n");
        sb.append("</fo:flow>\n</fo:page-sequence>\n");
        return sb.toString();
    }

    // ─── Cabeçalho ────────────────────────────────────────────────────────────

    // A tabela começa colada à moldura: as bordas de cima, da esquerda e da direita do cabeçalho SÃO a borda da página
    // (a moldura), então as células só desenham as linhas internas -- a de baixo de cada linha e a da direita de cada
    // coluna que não é a última. Todas com a mesma espessura da moldura.
    //
    // As alturas são as do modelo (Anexo XII) e valem para o conteúdo da linha; o FOP soma o padding de cima e de baixo
    // (3 pt cada), então "48" é uma linha de 54 pt no papel. Não dependem de o conteúdo caber: são mínimos.
    private static String celula(String conteudo, int colunas, int linhas, boolean bordaDireita,
                                 String alinhamentoVertical, String padding) {
        return "<fo:table-cell padding=\"" + padding + "\" border-bottom=\"" + LINHA + "\""
                + (bordaDireita ? " border-right=\"" + LINHA + "\"" : "")
                + (colunas > 1 ? " number-columns-spanned=\"" + colunas + "\"" : "")
                + (linhas > 1 ? " number-rows-spanned=\"" + linhas + "\"" : "")
                + " display-align=\"" + alinhamentoVertical + "\">" + conteudo + "</fo:table-cell>\n";
    }

    private static String celula(String conteudo, int colunas, int linhas, boolean bordaDireita, String alinhamentoVertical) {
        return celula(conteudo, colunas, linhas, bordaDireita, alinhamentoVertical, "3pt");
    }

    private static String centralizado(String texto) {
        return "<fo:block text-align=\"center\">" + foEsc(texto) + "</fo:block>";
    }

    private static String linha(int alturaDoConteudoEmPt, String... celulas) {
        return "<fo:table-row height=\"" + alturaDoConteudoEmPt + "pt\">\n" + String.join("", celulas) + "</fo:table-row>\n";
    }

    // Reproduz o modelo do Anexo XII: quatro colunas (A = DOM, identificação e rótulos; B e C = datas; D = distribuição).
    //   linha 1  Comando, OM e setor emissor, nas quatro colunas
    //   linha 2  [A: DOM ......] [B-C: DATAS .........] [D: DISTRIBUIÇÃO ]
    //   linha 3  [A: .........] [B: EMISSÃO] [C: EFETIVAÇÃO] [D ..........]
    //   linha 4  [A: .........] [B: valor] [C: valor, 2 linhas] [D: OSTENSIVA]
    //   linha 5  [A: ASSUNTO] [B-D: assunto, justificado]
    //   linha 6  [A: ANEXOS] [B-D: um anexo por linha, justificado]
    // O DOM ocupa as linhas 2 a 4 da coluna A e a identificação fica numa célula própria, embaixo dele, separada por uma
    // linha -- é um bloco com borda no pé da célula do DOM, porque no modelo a divisória cai no meio da linha 4.
    private static String tabelaDoCabecalho(CabecalhoDaNpa c) {
        var sb = new StringBuilder();
        sb.append("<fo:table table-layout=\"fixed\" width=\"100%\" border-collapse=\"separate\" font-size=\"11pt\">\n");
        sb.append("<fo:table-column column-width=\"proportional-column-width(24.5)\"/>\n");
        sb.append("<fo:table-column column-width=\"proportional-column-width(25)\"/>\n");
        sb.append("<fo:table-column column-width=\"proportional-column-width(26.5)\"/>\n");
        sb.append("<fo:table-column column-width=\"proportional-column-width(24)\"/>\n");
        sb.append("<fo:table-body>\n");

        var linhasDeCima = new StringBuilder();
        for (String texto : c.linhasDeCima()) {
            linhasDeCima.append("<fo:block font-weight=\"bold\" text-align=\"center\">").append(foEsc(texto)).append("</fo:block>");
        }
        sb.append(linha(48, celula(linhasDeCima.toString(), 4, 1, false, "after")));

        var domEIdentificacao = "<fo:block-container height=\"27.5pt\" border-top=\"" + LINHA + "\" display-align=\"center\">"
                + centralizado(c.identificacao()) + "</fo:block-container>";
        sb.append(linha(15,
                celula(domEIdentificacao, 1, 3, true, "after", "0pt"),               // linhas 2 a 4
                celula(centralizado("DATAS"), 2, 1, true, "center"),
                celula(centralizado("DISTRIBUIÇÃO"), 1, 2, false, "center")));         // linhas 2 e 3

        sb.append(linha(16,
                celula(centralizado("EMISSÃO"), 1, 1, true, "center"),
                celula(centralizado("EFETIVAÇÃO"), 1, 1, true, "center")));

        var efetivacao = new StringBuilder();
        for (String texto : c.efetivacao()) efetivacao.append(centralizado(texto));
        sb.append(linha(55,
                celula(centralizado(c.emissao()), 1, 1, true, "center"),
                celula(efetivacao.toString(), 1, 1, true, "center"),
                celula(centralizado(c.distribuicao()), 1, 1, false, "center")));

        sb.append(linha(29,
                celula(centralizado("ASSUNTO"), 1, 1, true, "center"),
                celula("<fo:block text-align=\"justify\">" + foEsc(c.assunto()) + "</fo:block>", 3, 1, false, "center",
                        "3pt 3pt 3pt 6pt")));

        var anexos = new StringBuilder();
        for (String texto : c.anexos()) {
            anexos.append("<fo:block text-align=\"justify\" space-before=\"2pt\" space-after=\"2pt\">").append(foEsc(texto)).append("</fo:block>");
        }
        sb.append(linha(20,
                celula(centralizado("ANEXOS"), 1, 1, true, "center"),
                celula(anexos.toString(), 3, 1, false, "center", "3pt 3pt 3pt 6pt")));

        sb.append("</fo:table-body>\n</fo:table>\n");
        return sb.toString();
    }

    // ─── Corpo ────────────────────────────────────────────────────────────────

    private static String rotuloDoElemento(Map<Long, ElementoNumeracao> numeros, ItemAnexoParteNormativaResponseDto item) {
        var n = numeros.get(item.id());
        return n != null ? n.label() : "";
    }

    private void renderizar(DocumentoFoContext ctx, ItemAnexoParteNormativaResponseDto item,
                            Map<Long, ElementoNumeracao> numeros, StringBuilder sb) {
        String numero = foEsc(rotuloDoElemento(numeros, item));
        String titulo = item.elementTitle() != null ? item.elementTitle() : "";
        switch (item.elementType()) {
            case CAPITULO -> sb.append("<fo:block font-weight=\"bold\" space-before=\"12pt\" space-after=\"6pt\" keep-with-next=\"always\">")
                    .append(numero).append("  ").append(foEsc(titulo.toUpperCase())).append("</fo:block>\n");
            case SECAO_NORMATIVA, SUBSECAO_NORMATIVA -> sb.append("<fo:block space-before=\"8pt\" space-after=\"4pt\" keep-with-next=\"always\">")
                    .append("<fo:inline font-weight=\"bold\">").append(numero).append("</fo:inline>  ")
                    .append("<fo:inline text-decoration=\"underline\">").append(foEsc(titulo)).append("</fo:inline></fo:block>\n");
            case PARAGRAFO -> texto(ctx, item, "<fo:inline font-weight=\"bold\">" + numero + "</fo:inline>", RECUO_DO_TEXTO, sb);
            case ALINEA -> texto(ctx, item, numero, RECUO_DA_ALINEA, sb);
            default -> {
                // Fora da gramática da NPA (a hierarquia o recusa no salvamento): não é desenhado.
            }
        }
        if (item.children() != null) {
            for (var filho : item.children()) renderizar(ctx, filho, numeros, sb);
        }
    }

    // O rótulo e o primeiro parágrafo na mesma linha; parágrafos, tabelas e figuras seguintes em blocos.
    private void texto(DocumentoFoContext ctx, ItemAnexoParteNormativaResponseDto item, String rotuloFo,
                       String recuo, StringBuilder sb) {
        TipTapNode doc = ctx.parseConteudo(item.elementContent());
        String primeiro = "";
        String resto = "";
        if (doc != null) {
            if (ctx.renderer.startsWithParagraph(doc)) {
                primeiro = ctx.renderer.renderParagraphInlines(doc.getContent().get(0));
                resto = ctx.renderer.renderSkippingFirstParagraph(doc);
            } else {
                resto = ctx.renderer.renderDocContent(doc);
            }
        }
        sb.append("<fo:block text-align=\"justify\" space-before=\"3pt\" space-after=\"3pt\" start-indent=\"").append(recuo).append("\">")
          .append(rotuloFo).append("  ").append(primeiro).append("</fo:block>\n");
        if (!resto.isBlank()) {
            sb.append("<fo:block text-align=\"justify\" start-indent=\"").append(recuo).append("\">").append(resto).append("</fo:block>\n");
        }
    }

    // ─── Fecho ────────────────────────────────────────────────────────────────

    private static String fecho(CabecalhoDaNpa c) {
        var sb = new StringBuilder();
        sb.append("<fo:block text-align=\"center\" space-before=\"24pt\" keep-with-next=\"always\">")
          .append(foEsc(c.localEData())).append("</fo:block>\n");
        for (AssinaturaDaNpaDto assinatura : c.assinaturas()) {
            sb.append("<fo:block text-align=\"center\" space-before=\"26pt\" keep-together.within-page=\"always\">")
              .append("<fo:block>").append(foEsc(assinatura.rotulo())).append("</fo:block>");
            var linhas = assinatura.linhas() != null ? assinatura.linhas() : List.<String>of();
            for (int i = 0; i < linhas.size(); i++) {
                sb.append("<fo:block").append(i == 0 ? " font-weight=\"bold\" space-before=\"14pt\"" : "").append(">")
                  .append(foEsc(linhas.get(i))).append("</fo:block>");
            }
            sb.append("</fo:block>\n");
        }
        if (c.publicadaNo() != null) {
            sb.append("<fo:block text-align=\"center\" font-size=\"10pt\" space-before=\"24pt\">")
              .append(foEsc(c.publicadaNo())).append("</fo:block>\n");
        }
        if (c.revogadaNo() != null) {
            sb.append("<fo:block text-align=\"center\" font-size=\"10pt\" space-before=\"6pt\">")
              .append(foEsc(c.revogadaNo())).append("</fo:block>\n");
        }
        return sb.toString();
    }
}
