package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.emendaDtos.MapaAlteracaoPdfRequestDto;
import br.com.danielchipolesch.domain.util.tiptap.TipTapNode;
import br.com.danielchipolesch.domain.util.tiptap.XslFoContentRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;

// Exporta o Quadro de Justificativas das Modificações Propostas (NSCA 5-3, Anexo XXIV)
// em PDF, via Apache FOP — o mesmo motor usado no PDF oficial do documento
// (DocumentoPdfService), em vez de window.print() sobre a página HTML. A4 paisagem,
// com as mesmas cores da tabela em ComparisonPage.vue.
@Service
public class MapaAlteracaoPdfService {

    // Equivalentes sólidos das cores usadas em rgba() na tela (FOP não suporta canal
    // alfa em background-color) — calculados sobre fundo branco.
    private static final String HEADER_BG   = "#ECF0F6"; // rgba(11,61,145,0.08) sobre branco
    private static final String HEADER_TEXT = "#0B3D91";
    private static final String BORDER      = "#CCCCCC";  // rgba(0,0,0,0.2) sobre branco
    private static final String COR_EXCLUIDO = "#B71C1C"; // texto em vigor sendo substituído/removido
    private static final String COR_INSERIDO = "#0D47A1"; // texto proposto sendo inserido

    private static final FopFactory FOP_FACTORY;

    static {
        try {
            FOP_FACTORY = FopFactory.newInstance(new File(".").toURI());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    public byte[] gerarPdf(MapaAlteracaoPdfRequestDto req) {
        String fo = buildFo(req);
        try (var os = new ByteArrayOutputStream()) {
            Fop fop = FOP_FACTORY.newFop(MimeConstants.MIME_PDF, FOP_FACTORY.newFOUserAgent(), os);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            XMLReader reader = spf.newSAXParser().getXMLReader();
            reader.setContentHandler(fop.getDefaultHandler());
            reader.parse(new InputSource(new StringReader(fo)));
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao renderizar PDF do mapa de alteração: " + e.getMessage(), e);
        }
    }

    private String buildFo(MapaAlteracaoPdfRequestDto req) {
        var renderer = new XslFoContentRenderer();
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">\n");
        sb.append("<fo:layout-master-set>\n");
        sb.append("  <fo:simple-page-master master-name=\"a4-paisagem\"")
          .append(" page-width=\"29.7cm\" page-height=\"21cm\"")
          .append(" margin-top=\"1.5cm\" margin-bottom=\"1.5cm\"")
          .append(" margin-left=\"1.5cm\" margin-right=\"1.5cm\">\n")
          .append("    <fo:region-body region-name=\"xsl-region-body\" margin-top=\"1.2cm\"/>\n")
          .append("    <fo:region-before region-name=\"xsl-region-before\" extent=\"1.2cm\"/>\n")
          .append("    <fo:region-after region-name=\"xsl-region-after\" extent=\"1cm\"/>\n")
          .append("  </fo:simple-page-master>\n");
        sb.append("</fo:layout-master-set>\n");

        sb.append("<fo:page-sequence master-reference=\"a4-paisagem\">\n");

        sb.append("<fo:static-content flow-name=\"xsl-region-before\">\n")
          .append("  <fo:block font-size=\"9pt\" text-align=\"right\" color=\"#666666\">")
          .append(foEsc(req.getDocId())).append("</fo:block>\n")
          .append("</fo:static-content>\n");

        sb.append("<fo:static-content flow-name=\"xsl-region-after\">\n")
          .append("  <fo:block font-size=\"9pt\" text-align=\"center\" color=\"#666666\">")
          .append("<fo:page-number/></fo:block>\n")
          .append("</fo:static-content>\n");

        sb.append("<fo:flow flow-name=\"xsl-region-body\">\n");

        sb.append("<fo:block font-size=\"14pt\" font-weight=\"bold\" text-align=\"center\"")
          .append(" color=\"").append(HEADER_TEXT).append("\" space-after=\"4pt\">")
          .append("Quadro de Justificativas das Modificações Propostas</fo:block>\n");
        if (req.getCiclo() != null && !req.getCiclo().isBlank()) {
            sb.append("<fo:block font-size=\"10pt\" text-align=\"center\" color=\"#666666\" space-after=\"14pt\">")
              .append(foEsc(req.getCiclo())).append("</fo:block>\n");
        }

        sb.append("<fo:table table-layout=\"fixed\" width=\"100%\" border-collapse=\"separate\">\n");
        sb.append("  <fo:table-column column-width=\"13%\"/>\n");
        sb.append("  <fo:table-column column-width=\"29%\"/>\n");
        sb.append("  <fo:table-column column-width=\"29%\"/>\n");
        sb.append("  <fo:table-column column-width=\"29%\"/>\n");

        sb.append("  <fo:table-header>\n");
        sb.append("    <fo:table-row>\n");
        for (String header : new String[]{"Referência", "Texto em Vigor", "Texto Proposto", "Justificativa"}) {
            sb.append("      <fo:table-cell border=\"1pt solid ").append(BORDER).append("\"")
              .append(" background-color=\"").append(HEADER_BG).append("\" padding=\"5pt 6pt\">")
              .append("<fo:block font-size=\"9pt\" font-weight=\"bold\" color=\"").append(HEADER_TEXT).append("\">")
              .append(header).append("</fo:block></fo:table-cell>\n");
        }
        sb.append("    </fo:table-row>\n");
        sb.append("  </fo:table-header>\n");

        sb.append("  <fo:table-body>\n");
        var itens = req.getItens() != null ? req.getItens() : java.util.List.<MapaAlteracaoPdfRequestDto.Item>of();
        for (var item : itens) {
            sb.append("    <fo:table-row>\n");
            sb.append(cell("<fo:block font-size=\"9pt\" font-weight=\"bold\" color=\"" + HEADER_TEXT + "\">"
                    + foEsc(item.getReferencia()) + "</fo:block>"));
            sb.append(cell(conteudoCelula(renderer, item.getTextoAnterior(), "INCLUIR".equals(item.getAcao()), "(novo)", COR_EXCLUIDO)));
            sb.append(cell(conteudoCelula(renderer, item.getTextoNovo(), "REVOGAR".equals(item.getAcao()), "(revogado)", COR_INSERIDO)));
            sb.append(cell("<fo:block font-size=\"9pt\">" + foEsc(nvl(item.getJustificativa(), "—")) + "</fo:block>"));
            sb.append("    </fo:table-row>\n");
        }
        if (itens.isEmpty()) {
            sb.append("    <fo:table-row><fo:table-cell number-columns-spanned=\"4\"")
              .append(" border=\"1pt solid ").append(BORDER).append("\" padding=\"8pt\">")
              .append("<fo:block font-size=\"9pt\" text-align=\"center\" color=\"#666666\">")
              .append("Sem modificações neste ciclo.</fo:block></fo:table-cell></fo:table-row>\n");
        }
        sb.append("  </fo:table-body>\n");
        sb.append("</fo:table>\n");

        sb.append("</fo:flow>\n");
        sb.append("</fo:page-sequence>\n");
        sb.append("</fo:root>");
        return sb.toString();
    }

    private String cell(String blockFo) {
        return "      <fo:table-cell border=\"1pt solid " + BORDER + "\" padding=\"5pt 6pt\">"
                + blockFo + "</fo:table-cell>\n";
    }

    private String conteudoCelula(XslFoContentRenderer renderer, String tiptapJson, boolean placeholder,
                                   String placeholderTexto, String cor) {
        if (placeholder) {
            return "<fo:block font-size=\"9pt\" font-style=\"italic\" color=\"#666666\">" + placeholderTexto + "</fo:block>";
        }
        TipTapNode doc = parseConteudo(tiptapJson);
        if (doc == null) return "<fo:block font-size=\"9pt\"/>";
        String fo = renderer.renderDocContent(doc);
        if (fo.isBlank()) return "<fo:block font-size=\"9pt\"/>";
        // color no fo:block externo herda para os fo:block/fo:inline internos, exceto
        // onde o próprio conteúdo já define uma cor explícita (textStyle do TipTap),
        // que corretamente prevalece por ser mais específica.
        return "<fo:block color=\"" + cor + "\">" + fo.replace("font-size=\"12pt\"", "font-size=\"9pt\"") + "</fo:block>";
    }

    private TipTapNode parseConteudo(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) return null;
        try {
            return objectMapper.readValue(conteudo, TipTapNode.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String nvl(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private static String foEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
