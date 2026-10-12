package intraer.fablegis.domain.util.tiptap;

import java.util.List;
import java.util.Locale;

/**
 * Converts a TipTap JSON document node to an HTML string.
 * Used by DocumentoHtmlService to render section content when stored as JSON.
 */
public final class TipTapHtmlSerializer {

    private TipTapHtmlSerializer() {}

    public static String toHtml(TipTapNode doc) {
        if (doc == null) return "";
        var sb = new StringBuilder();
        List<TipTapNode> nodes = "doc".equals(doc.getType()) ? doc.getContent() : List.of(doc);
        if (nodes != null) {
            for (var node : nodes) renderBlock(node, sb);
        }
        return sb.toString();
    }

    // ─── Block nodes ──────────────────────────────────────────────────────────

    private static void renderBlock(TipTapNode node, StringBuilder sb) {
        if (node == null || node.getType() == null) return;
        switch (node.getType()) {
            case "paragraph" -> {
                String align = node.getAttrStr("textAlign");
                String style = (align != null && !align.equals("left"))
                        ? " style=\"text-align:" + align + "\"" : "";
                sb.append("<p").append(style).append(">");
                renderInlines(node.getContent(), sb);
                sb.append("</p>");
            }
            case "heading" -> {
                int level = node.getAttrInt("level", 2);
                sb.append("<h").append(level).append(">");
                renderInlines(node.getContent(), sb);
                sb.append("</h").append(level).append(">");
            }
            case "bulletList" -> {
                sb.append("<ul>");
                if (node.getContent() != null)
                    for (var li : node.getContent()) renderListItem(li, sb);
                sb.append("</ul>");
            }
            case "orderedList" -> {
                sb.append("<ol>");
                if (node.getContent() != null)
                    for (var li : node.getContent()) renderListItem(li, sb);
                sb.append("</ol>");
            }
            case "table"   -> renderTable(node, sb);
            case "figure"  -> renderFigure(node, sb);
            case "blockquote" -> {
                sb.append("<blockquote>");
                if (node.getContent() != null)
                    for (var c : node.getContent()) renderBlock(c, sb);
                sb.append("</blockquote>");
            }
            case "horizontalRule" -> sb.append("<hr />");
        }
    }

    private static void renderListItem(TipTapNode li, StringBuilder sb) {
        sb.append("<li>");
        if ("listItem".equals(li.getType()) && li.getContent() != null) {
            for (var child : li.getContent()) {
                if ("paragraph".equals(child.getType())) renderInlines(child.getContent(), sb);
                else renderBlock(child, sb);
            }
        }
        sb.append("</li>");
    }

    private static void renderTable(TipTapNode table, StringBuilder sb) {
        var grade = GradeDeTabela.de(table);
        sb.append("<table>");
        if (grade.linhas().isEmpty()) { sb.append("</table>"); return; }
        // Larguras que o usuário arrastou no editor, em porcentagem da tabela (só quando houve ajuste).
        if (grade.temLarguras()) {
            double[] larguras = grade.largurasRelativas();
            double total = 0;
            for (double l : larguras) total += l;
            sb.append("<colgroup>");
            for (double l : larguras) {
                sb.append("<col style=\"width:").append(String.format(Locale.ROOT, "%.2f", l * 100 / total)).append("%\" />");
            }
            sb.append("</colgroup>");
        }
        int cabecalho = grade.linhasDeCabecalho();
        if (cabecalho > 0) {
            sb.append("<thead>");
            for (int r = 0; r < cabecalho; r++) renderLinhaDaTabela(grade, r, sb, "col");
            sb.append("</thead>");
        }
        sb.append("<tbody>");
        for (int r = cabecalho; r < grade.linhas().size(); r++) renderLinhaDaTabela(grade, r, sb, "row");
        sb.append("</tbody></table>");
    }

    // escopoDoTh: "col" para o cabeçalho de coluna (thead), "row" para uma célula de cabeçalho no corpo (cabeçalho de linha).
    private static void renderLinhaDaTabela(GradeDeTabela grade, int indice, StringBuilder sb, String escopoDoTh) {
        sb.append("<tr>");
        for (var celula : grade.linhas().get(indice)) {
            String tag = celula.cabecalho() ? "th" : "td";
            sb.append("<").append(tag).append(spanAttrs(celula.no()));
            if (celula.cabecalho()) sb.append(" scope=\"").append(escopoDoTh).append("\"");
            sb.append(">");
            if (celula.no().getContent() != null)
                for (var c : celula.no().getContent()) renderBlock(c, sb);
            sb.append("</").append(tag).append(">");
        }
        sb.append("</tr>");
    }

    private static String spanAttrs(TipTapNode cell) {
        var sb = new StringBuilder();
        int cs = cell.getAttrInt("colspan", 1);
        int rs = cell.getAttrInt("rowspan", 1);
        if (cs > 1) sb.append(" colspan=\"").append(cs).append("\"");
        if (rs > 1) sb.append(" rowspan=\"").append(rs).append("\"");
        return sb.toString();
    }

    private static void renderFigure(TipTapNode node, StringBuilder sb) {
        String src    = node.getAttrStr("src");
        String alt    = node.getAttrStr("alt");
        String titulo = node.getAttrStr("titulo");
        String fonte  = node.getAttrStr("fonte");
        sb.append("<figure data-type=\"figura\" class=\"doc-figure\">");
        sb.append("<p class=\"figura-titulo\">").append(esc(titulo)).append("</p>");
        sb.append("<img src=\"").append(src != null ? src : "")
          .append("\" alt=\"").append(esc(alt)).append("\" class=\"figura-img\" />");
        if (fonte != null && !fonte.isBlank()) {
            sb.append("<p class=\"figura-fonte\">Fonte: ").append(esc(fonte)).append("</p>");
        } else {
            sb.append("<p class=\"figura-fonte\"></p>");
        }
        sb.append("</figure>");
    }

    // ─── Inline nodes ─────────────────────────────────────────────────────────

    private static void renderInlines(List<TipTapNode> nodes, StringBuilder sb) {
        if (nodes == null) return;
        for (var node : nodes) renderInline(node, sb);
    }

    private static void renderInline(TipTapNode node, StringBuilder sb) {
        if (node == null) return;
        if ("text".equals(node.getType())) {
            String text = esc(node.getText());
            if (node.getMarks() == null || node.getMarks().isEmpty()) {
                sb.append(text);
                return;
            }
            String open = "", close = "";
            for (var mark : node.getMarks()) {
                switch (mark.getType()) {
                    case "bold"      -> { open += "<strong>"; close = "</strong>" + close; }
                    case "italic"    -> { open += "<em>";     close = "</em>" + close;     }
                    case "underline" -> { open += "<u>";      close = "</u>" + close;      }
                    case "strike"    -> { open += "<s>";      close = "</s>" + close;      }
                    case "textStyle" -> {
                        String color = mark.getAttr("color");
                        if (color != null) {
                            open += "<span style=\"color:" + color + "\">";
                            close = "</span>" + close;
                        }
                    }
                    case "highlight" -> {
                        String bg = mark.getAttr("color");
                        if (bg != null) {
                            open += "<mark style=\"background-color:" + bg + "\">";
                            close = "</mark>" + close;
                        }
                    }
                }
            }
            sb.append(open).append(text).append(close);
        } else if ("hardBreak".equals(node.getType())) {
            sb.append("<br />");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
