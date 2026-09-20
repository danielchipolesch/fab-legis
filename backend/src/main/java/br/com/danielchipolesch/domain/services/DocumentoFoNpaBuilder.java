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
    private static final String MOLDURA = "0.75pt solid #000000";
    private static final String LINHA_DA_TABELA = "0.5pt solid #000000";

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
                  margin-top="2cm" margin-bottom="2cm" margin-left="2cm" margin-right="2cm">
                <fo:region-body region-name="xsl-region-body" margin="0.3cm"/>
                <fo:region-before region-name="wm" extent="0pt" overflow="visible"/>
                <fo:region-end region-name="npa-moldura-primeira" extent="0pt" overflow="visible"/>
              </fo:simple-page-master>
              <fo:simple-page-master master-name="npa-demais"
                  page-width="21cm" page-height="29.7cm"
                  margin-top="2cm" margin-bottom="2cm" margin-left="2cm" margin-right="2cm">
                <fo:region-body region-name="xsl-region-body" margin-top="1.1cm" margin-bottom="0.3cm"
                    margin-left="0.3cm" margin-right="0.3cm"/>
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

    // A moldura de cada master: 17 x 25,7 cm na primeira página; nas demais começa 0,8 cm mais abaixo, sob o "n/total".
    private static String moldura(String regiao, String topo, String altura) {
        return "<fo:static-content flow-name=\"" + regiao + "\">\n"
                + "  <fo:block-container absolute-position=\"fixed\" top=\"" + topo + "\" left=\"2cm\" width=\"17cm\" height=\"" + altura
                + "\" border=\"" + MOLDURA + "\"><fo:block>&#160;</fo:block></fo:block-container>\n"
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
        sb.append(moldura("npa-moldura-primeira", "2cm", "25.7cm"));
        sb.append(moldura("npa-moldura-demais", "2.8cm", "24.9cm"));
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
        for (var item : normativos) renderizar(ctx, item, numeros, sb);
        sb.append(fecho(cabecalho));
        sb.append("<fo:block id=\"npa-fim\" font-size=\"1pt\">&#160;</fo:block>\n");
        sb.append("</fo:flow>\n</fo:page-sequence>\n");
        return sb.toString();
    }

    // ─── Cabeçalho ────────────────────────────────────────────────────────────

    private static String celula(String conteudo, int colunas, int linhas) {
        return "<fo:table-cell border=\"" + LINHA_DA_TABELA + "\" padding=\"3pt\""
                + (colunas > 1 ? " number-columns-spanned=\"" + colunas + "\"" : "")
                + (linhas > 1 ? " number-rows-spanned=\"" + linhas + "\"" : "")
                + " display-align=\"center\">" + conteudo + "</fo:table-cell>\n";
    }

    private static String rotulo(String texto) {
        return "<fo:block font-weight=\"bold\" text-align=\"center\">" + foEsc(texto) + "</fo:block>";
    }

    private static String texto(String texto) {
        return "<fo:block text-align=\"center\">" + foEsc(texto) + "</fo:block>";
    }

    // Quatro colunas: a da esquerda (DOM + identificação, e os rótulos ASSUNTO/ANEXOS) e três de conteúdo.
    private static String tabelaDoCabecalho(CabecalhoDaNpa c) {
        var sb = new StringBuilder();
        sb.append("<fo:table table-layout=\"fixed\" width=\"100%\" border=\"").append(LINHA_DA_TABELA)
          .append("\" font-size=\"10pt\" space-after=\"10pt\">\n");
        sb.append("<fo:table-column column-width=\"proportional-column-width(30)\"/>\n");
        sb.append("<fo:table-column column-width=\"proportional-column-width(24)\"/>\n");
        sb.append("<fo:table-column column-width=\"proportional-column-width(23)\"/>\n");
        sb.append("<fo:table-column column-width=\"proportional-column-width(23)\"/>\n");
        sb.append("<fo:table-body>\n");

        // Linha 1: o espaço do DOM (distintivo da OM, tratado depois) e a identificação; Comando, OM e setor.
        var identificacao = "<fo:block-container height=\"2.2cm\"><fo:block>&#160;</fo:block></fo:block-container>"
                + "<fo:block font-weight=\"bold\" text-align=\"center\">" + foEsc(c.identificacao()) + "</fo:block>";
        var linhasDeCima = new StringBuilder();
        for (String linha : c.linhasDeCima()) {
            linhasDeCima.append("<fo:block font-weight=\"bold\" text-align=\"center\">").append(foEsc(linha)).append("</fo:block>");
        }
        sb.append("<fo:table-row>\n").append(celula(identificacao, 1, 3)).append(celula(linhasDeCima.toString(), 3, 1))
          .append("</fo:table-row>\n");

        // Linha 2: DATAS.
        sb.append("<fo:table-row>\n").append(celula(rotulo("DATAS"), 1, 1))
          .append(celula(rotulo("EMISSÃO") + texto(c.emissao()), 1, 1))
          .append(celula(rotulo("EFETIVAÇÃO") + texto(c.efetivacao()), 1, 1)).append("</fo:table-row>\n");

        // Linha 3: DISTRIBUIÇÃO.
        sb.append("<fo:table-row>\n").append(celula(rotulo("DISTRIBUIÇÃO"), 1, 1))
          .append(celula(texto(c.distribuicao()), 2, 1)).append("</fo:table-row>\n");

        // Linhas 4 e 5: ASSUNTO e ANEXOS.
        sb.append("<fo:table-row>\n").append(celula(rotulo("ASSUNTO"), 1, 1))
          .append(celula(texto(c.assunto()), 3, 1)).append("</fo:table-row>\n");
        sb.append("<fo:table-row>\n").append(celula(rotulo("ANEXOS"), 1, 1))
          .append(celula(texto(c.anexos()), 3, 1)).append("</fo:table-row>\n");

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
            case PARAGRAFO -> texto(ctx, item, "<fo:inline font-weight=\"bold\">" + numero + "</fo:inline>", "0pt", sb);
            case ALINEA -> texto(ctx, item, numero, "1cm", sb);
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
        return sb.toString();
    }
}
