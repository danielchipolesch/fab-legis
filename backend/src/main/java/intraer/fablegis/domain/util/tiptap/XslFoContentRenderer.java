package intraer.fablegis.domain.util.tiptap;

import intraer.fablegis.domain.util.ImagemRemota;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Converts a TipTap JSON document to XSL-FO XML fragments.
 * One instance per PDF render; figCount and tabelaCount track figure and table numbering across all sections.
 */
public class XslFoContentRenderer {

    private static final Pattern BLOCK_PATTERN =
            Pattern.compile("(?i)^\\s*(table|bulletList|orderedList|heading|figure)$");

    // Caixa máxima padrão de uma figura fora de tabela (largura útil do corpo,
    // 17cm de A4 menos margens, com folga) -- ver renderFigure.
    private static final double FIG_MAX_W = 450;
    private static final double FIG_MAX_H = 350;
    // padding="3pt 6pt" (block 3pt, inline 6pt) + border 1pt de cada lado da
    // fo:table-cell (ver renderTable) -- descontado da largura da coluna antes de
    // virar caixa máxima da figura, senão a imagem cabe na COLUNA mas estoura a
    // CÉLULA (a coluna é só o espaço de conteúdo + padding + borda).
    private static final double TABLE_CELL_OVERHEAD_W = 14;

    private int figCount = 0;
    private int tabelaCount = 0;
    private Function<String, String> imageResolver = null;

    public int getFigCount() { return figCount; }
    public void setFigCount(int figCount) { this.figCount = figCount; }

    /**
     * Define um resolvedor de imagens que recebe a URL original e retorna um data URI.
     * Quando definido, é tentado antes do fallback HTTP. Retornar null delega ao HTTP.
     */
    public void setImageResolver(Function<String, String> resolver) { this.imageResolver = resolver; }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Renders all block-level children of a TipTap doc node as FO blocks.
     * Use this when the normative element contains block content (table, figure, list).
     */
    public String renderDocContent(TipTapNode doc) {
        if (doc == null) return "";
        var sb = new StringBuilder();
        for (var node : children(doc)) renderBlock(node, sb);
        return sb.toString();
    }

    /**
     * Renders the inline text of a TipTap doc (first paragraph) as FO inline elements.
     * Use this for artigo/parágrafo text that sits on the same line as the label.
     */
    public String renderInlineContent(TipTapNode doc) {
        if (doc == null) return "";
        var sb = new StringBuilder();
        for (var node : children(doc)) {
            if ("paragraph".equals(node.getType())) {
                renderInlines(node.getContent(), sb);
            }
        }
        return sb.toString();
    }

    /**
     * Renders inline content of a single paragraph node (not a full doc).
     * Use when iterating a doc's paragraphs manually to produce per-paragraph fo:blocks.
     */
    public String renderParagraphInlines(TipTapNode para) {
        if (para == null) return "";
        var sb = new StringBuilder();
        renderInlines(para.getContent(), sb);
        return sb.toString();
    }

    /**
     * Returns true if the doc contains any block-level element (table, figure, list, heading).
     */
    public boolean hasBlockContent(TipTapNode doc) {
        if (doc == null) return false;
        for (var node : children(doc)) {
            String t = node.getType();
            if (t != null && BLOCK_PATTERN.matcher(t).matches()) return true;
        }
        return false;
    }

    /**
     * Returns true if the first child of the doc is a paragraph node.
     * Used to decide whether the label and first paragraph can share the same fo:block.
     */
    public boolean startsWithParagraph(TipTapNode doc) {
        if (doc == null) return false;
        var ch = children(doc);
        return !ch.isEmpty() && "paragraph".equals(ch.get(0).getType());
    }

    /**
     * Renders all children of the doc as blocks, skipping the first one if it is a paragraph.
     * Used after the first paragraph has been rendered inline together with the element label.
     */
    public String renderSkippingFirstParagraph(TipTapNode doc) {
        if (doc == null) return "";
        var ch = children(doc);
        if (ch.isEmpty()) return "";
        var sb = new StringBuilder();
        int start = "paragraph".equals(ch.get(0).getType()) ? 1 : 0;
        for (int i = start; i < ch.size(); i++) renderBlock(ch.get(i), sb);
        return sb.toString();
    }

    // ─── Block rendering ──────────────────────────────────────────────────────

    private void renderBlock(TipTapNode node, StringBuilder sb) {
        renderBlock(node, sb, FIG_MAX_W, FIG_MAX_H);
    }

    // figMaxW/figMaxH: caixa máxima disponível para uma figura encontrada dentro
    // deste bloco -- FIG_MAX_W/H por padrão, ou a largura de uma coluna de tabela
    // (já descontada de padding/borda) quando renderBlock é chamado a partir de
    // renderTable, para uma figura dentro de uma célula nunca ultrapassar a
    // própria célula (ver renderTable/renderFigure).
    private void renderBlock(TipTapNode node, StringBuilder sb, double figMaxW, double figMaxH) {
        if (node == null || node.getType() == null) return;
        switch (node.getType()) {
            case "paragraph" -> {
                String align = node.getAttrStr("textAlign");
                String foAlign = align != null ? align : "justify";
                sb.append("<fo:block font-size=\"12pt\" space-after=\"5pt\" text-align=\"").append(foAlign).append("\">");
                renderInlines(node.getContent(), sb);
                sb.append("</fo:block>");
            }
            case "heading" -> {
                int level = node.getAttrInt("level", 2);
                String size = switch (level) { case 1 -> "16pt"; case 2 -> "14pt"; case 3 -> "13pt"; default -> "12pt"; };
                sb.append("<fo:block font-size=\"").append(size).append("\" font-weight=\"bold\" space-after=\"5pt\">");
                renderInlines(node.getContent(), sb);
                sb.append("</fo:block>");
            }
            case "bulletList" -> {
                sb.append("<fo:list-block provisional-distance-between-starts=\"1.5em\" space-after=\"5pt\">");
                if (node.getContent() != null)
                    for (var li : node.getContent()) renderListItem(li, sb, "•");
                sb.append("</fo:list-block>");
            }
            case "orderedList" -> {
                sb.append("<fo:list-block provisional-distance-between-starts=\"1.5em\" space-after=\"5pt\">");
                if (node.getContent() != null) {
                    int[] n = {0};
                    for (var li : node.getContent()) { n[0]++; renderListItem(li, sb, n[0] + "."); }
                }
                sb.append("</fo:list-block>");
            }
            case "table"    -> renderTable(node, sb);
            case "figure"   -> renderFigure(node, sb, figMaxW, figMaxH);
            case "blockquote" -> {
                sb.append("<fo:block start-indent=\"1cm\" space-after=\"5pt\">");
                if (node.getContent() != null)
                    for (var c : node.getContent()) renderBlock(c, sb, figMaxW, figMaxH);
                sb.append("</fo:block>");
            }
            case "horizontalRule" ->
                sb.append("<fo:block border-top=\"1pt solid black\" space-before=\"5pt\" space-after=\"5pt\"/>");
        }
    }

    private void renderListItem(TipTapNode li, StringBuilder sb, String bullet) {
        sb.append("<fo:list-item>");
        sb.append("<fo:list-item-label end-indent=\"label-end()\"><fo:block>")
          .append(foEsc(bullet)).append("</fo:block></fo:list-item-label>");
        sb.append("<fo:list-item-body start-indent=\"body-start()\">");
        if ("listItem".equals(li.getType()) && li.getContent() != null) {
            for (var child : li.getContent()) {
                if ("paragraph".equals(child.getType())) {
                    sb.append("<fo:block font-size=\"12pt\">");
                    renderInlines(child.getContent(), sb);
                    sb.append("</fo:block>");
                } else {
                    renderBlock(child, sb);
                }
            }
        }
        sb.append("</fo:list-item-body></fo:list-item>");
    }

    private void renderTable(TipTapNode table, StringBuilder sb) {
        var grade = GradeDeTabela.de(table);
        if (grade.linhas().isEmpty()) return;
        int colunas = Math.max(1, grade.colunas());
        double[] larguras = grade.largurasRelativas();
        String estilo = EstiloDeTabela.estilo(table.getAttrStr("estilo"));
        var cor = EstiloDeTabela.cor(table.getAttrStr("cor"));
        String titulo = table.getAttrStr("titulo");
        String fonte = table.getAttrStr("fonte");

        // Legenda como a da figura: "Tabela N — título" acima (só a tabela com título é numerada), "Fonte: …" abaixo.
        sb.append("<fo:block space-before=\"5pt\" space-after=\"5pt\">");
        if (EstiloDeTabela.temTitulo(titulo)) {
            tabelaCount++;
            sb.append("<fo:block font-size=\"10pt\" font-style=\"italic\" text-align=\"center\" space-after=\"3pt\" keep-with-next.within-page=\"always\">")
              .append("Tabela ").append(tabelaCount).append(" — ").append(foEsc(titulo.trim()))
              .append("</fo:block>");
        }
        sb.append("<fo:table table-layout=\"fixed\" width=\"100%\" border-collapse=\"separate\">");
        // Largura de cada coluna: a que o usuário arrastou no editor (colwidth), em proporção -- o total se ajusta à página.
        for (int i = 0; i < colunas; i++) {
            sb.append("<fo:table-column column-width=\"proportional-column-width(")
              .append(fmtPt(i < larguras.length ? larguras[i] : 1)).append(")\"/>");
        }
        int cabecalho = grade.linhasDeCabecalho();
        if (cabecalho > 0) {
            // Cabeçalho repetido no topo de cada página quando a tabela atravessa uma quebra.
            sb.append("<fo:table-header>");
            for (int r = 0; r < cabecalho; r++) renderLinhaDaTabela(grade, r, sb, estilo, cor, r - cabecalho);
            sb.append("</fo:table-header>");
        }
        sb.append("<fo:table-body>");
        for (int r = cabecalho; r < grade.linhas().size(); r++) renderLinhaDaTabela(grade, r, sb, estilo, cor, r - cabecalho);
        sb.append("</fo:table-body></fo:table>");
        if (fonte != null && !fonte.isBlank()) {
            sb.append("<fo:block font-size=\"9pt\" text-align=\"center\" space-before=\"3pt\">Fonte: ").append(foEsc(fonte.trim())).append("</fo:block>");
        }
        sb.append("</fo:block>");
    }

    // indiceNoCorpo: posição da linha no corpo (0, 1, 2…); negativo no cabeçalho. Serve ao zebrado.
    private void renderLinhaDaTabela(GradeDeTabela grade, int indice, StringBuilder sb,
                                     String estilo, EstiloDeTabela.Cor cor, int indiceNoCorpo) {
        boolean compacto = EstiloDeTabela.COMPACTO.equals(estilo);
        boolean aberto = EstiloDeTabela.ABERTO.equals(estilo);
        boolean zebra = EstiloDeTabela.ZEBRADO.equals(estilo) && indiceNoCorpo >= 0 && indiceNoCorpo % 2 == 1;
        String borda = "1pt solid " + cor.borda();
        // Uma linha não se parte entre páginas.
        sb.append("<fo:table-row keep-together.within-page=\"always\">");
        for (var celula : grade.linhas().get(indice)) {
            var no = celula.no();
            boolean ehCabecalho = celula.cabecalho();
            // A cor escolhida para a célula vale mais que a do cabeçalho e a do zebrado.
            String fundo = EstiloDeTabela.fundo(no.getAttrStr("fundo"));
            if (fundo == null) fundo = ehCabecalho ? cor.cabecalho() : (zebra ? EstiloDeTabela.COR_DA_ZEBRA : "transparent");
            String peso = ehCabecalho ? "bold" : "normal";
            String alinhamentoPadrao = ehCabecalho ? "center" : "justify";
            // Uma figura dentro da célula cabe na coluna (ou colunas) que a célula realmente ocupa, descontado padding/borda.
            double figMaxW = Math.max(40, (FIG_MAX_W * grade.fracaoDaLargura(celula)) - TABLE_CELL_OVERHEAD_W);
            sb.append("<fo:table-cell padding=\"").append(compacto ? "1pt 3pt" : "3pt 6pt").append("\"");
            if (aberto) {
                sb.append(" border-top=\"").append(borda).append("\" border-bottom=\"").append(borda).append("\"");
            } else {
                sb.append(" border=\"").append(borda).append("\"");
            }
            sb.append(" background-color=\"").append(fundo).append("\"")
              .append(" display-align=\"").append(EstiloDeTabela.displayAlignDoFo(no.getAttrStr("alinhamentoVertical"))).append("\"");
            if (celula.colspan() > 1) sb.append(" number-columns-spanned=\"").append(celula.colspan()).append("\"");
            if (celula.rowspan() > 1) sb.append(" number-rows-spanned=\"").append(celula.rowspan()).append("\"");
            sb.append(">");
            if (no.getContent() != null && !no.getContent().isEmpty()) {
                for (var c : no.getContent()) {
                    if ("paragraph".equals(c.getType())) {
                        // O alinhamento escolhido pelo autor no parágrafo vale; sem escolha, cabeçalho centralizado e corpo justificado.
                        String alinhamento = c.getAttrStr("textAlign");
                        sb.append("<fo:block font-size=\"").append(compacto ? "8.5pt" : "10pt").append("\" font-weight=\"").append(peso)
                          .append("\" text-align=\"").append(alinhamento != null ? alinhamento : alinhamentoPadrao).append("\">");
                        renderInlines(c.getContent(), sb);
                        sb.append("</fo:block>");
                    } else {
                        renderBlock(c, sb, figMaxW, FIG_MAX_H);
                    }
                }
            } else {
                sb.append("<fo:block/>");
            }
            sb.append("</fo:table-cell>");
        }
        sb.append("</fo:table-row>");
    }

    private void renderFigure(TipTapNode node, StringBuilder sb, double maxW, double maxH) {
        figCount++;
        String src    = node.getAttrStr("src");
        String titulo = node.getAttrStr("titulo");
        String fonte  = node.getAttrStr("fonte");

        String imgSrc = resolveImageSrc(src);

        sb.append("<fo:block space-before=\"5pt\" space-after=\"5pt\">");
        sb.append("<fo:block font-size=\"10pt\" font-style=\"italic\" text-align=\"center\" keep-with-next.within-page=\"always\">")
          .append("Figura ").append(figCount).append(" — ").append(foEsc(titulo))
          .append("</fo:block>");
        if (imgSrc != null && !imgSrc.isBlank()) {
            // Viewport explícito (width + height) para que o FOP reserve espaço no layout
            // antes de posicionar o próximo bloco -- evita sobreposição de texto. Antes
            // isto era sempre 450x350pt fixo (a caixa máxima), e scale-to-fit só
            // encolhia a IMAGEM dentro dela -- uma imagem com proporção diferente de
            // 450:350 deixava a caixa maior que a imagem visível, e o "Fonte:" (que vem
            // logo depois do bloco) parecia flutuar longe da imagem. Calculando aqui as
            // dimensões reais já escaladas (mesma lógica de scale-to-fit, mas aplicada
            // ao viewport também, não só ao conteúdo), o bloco fica do tamanho exato da
            // imagem renderizada. maxW/maxH: caixa cheia (FIG_MAX_W/H) fora de tabela,
            // ou a largura da coluna (já descontada de padding/borda) dentro de uma
            // célula -- ver renderTable/renderBlock.
            double[] dims = dimensoesEscaladas(imgSrc, maxW, maxH);
            sb.append("<fo:block text-align=\"center\" keep-with-next.within-page=\"always\">")
              .append("<fo:external-graphic src=\"url('").append(imgSrc).append("')\"")
              .append(" width=\"").append(fmtPt(dims[0])).append("pt\"")
              .append(" height=\"").append(fmtPt(dims[1])).append("pt\"")
              .append(" content-width=\"scale-to-fit\" content-height=\"scale-to-fit\"")
              .append(" scaling=\"uniform\"/>")
              .append("</fo:block>");
        }
        if (fonte != null && !fonte.isBlank()) {
            sb.append("<fo:block font-size=\"9pt\" text-align=\"center\">Fonte: ").append(foEsc(fonte)).append("</fo:block>");
        }
        sb.append("</fo:block>");
    }

    // ─── Inline rendering ─────────────────────────────────────────────────────

    private void renderInlines(List<TipTapNode> nodes, StringBuilder sb) {
        if (nodes == null) return;
        for (var node : nodes) renderInline(node, sb);
    }

    private void renderInline(TipTapNode node, StringBuilder sb) {
        if (node == null) return;
        if ("text".equals(node.getType())) {
            String text = foEsc(node.getText());
            if (node.getMarks() == null || node.getMarks().isEmpty()) {
                sb.append(text);
                return;
            }
            String open = "", close = "";
            for (var mark : node.getMarks()) {
                switch (mark.getType()) {
                    case "bold"      -> { open += "<fo:inline font-weight=\"bold\">";         close = "</fo:inline>" + close; }
                    case "italic"    -> { open += "<fo:inline font-style=\"italic\">";        close = "</fo:inline>" + close; }
                    case "underline" -> { open += "<fo:inline text-decoration=\"underline\">"; close = "</fo:inline>" + close; }
                    case "strike"    -> { open += "<fo:inline text-decoration=\"line-through\">"; close = "</fo:inline>" + close; }
                    case "textStyle" -> {
                        String color = mark.getAttr("color");
                        if (color != null) { open += "<fo:inline color=\"" + color + "\">"; close = "</fo:inline>" + close; }
                    }
                    case "highlight" -> {
                        String bg = mark.getAttr("color");
                        if (bg != null) { open += "<fo:inline background-color=\"" + bg + "\">"; close = "</fo:inline>" + close; }
                    }
                }
            }
            sb.append(open).append(text).append(close);
        } else if ("hardBreak".equals(node.getType())) {
            sb.append("<fo:block/>");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static List<TipTapNode> children(TipTapNode node) {
        var c = node.getContent();
        return c != null ? c : List.of();
    }

    // Lê as dimensões intrínsecas do data URI (só o cabeçalho, ImageIO não decodifica
    // os pixels todos pra isso) e devolve [largura, altura] em pt já escaladas pra
    // caber em maxW x maxH preservando a proporção -- mesmo cálculo que
    // scale-to-fit faria no conteúdo, mas aplicado também ao viewport do bloco (ver
    // renderFigure). Se não der pra ler (formato não suportado, data URI malformado),
    // devolve a caixa máxima original como fallback seguro -- comportamento idêntico
    // ao de antes desta mudança.
    private static double[] dimensoesEscaladas(String dataUri, double maxW, double maxH) {
        try {
            int comma = dataUri.indexOf(',');
            if (comma < 0 || !dataUri.startsWith("data:")) return new double[]{maxW, maxH};
            byte[] bytes = Base64.getDecoder().decode(dataUri.substring(comma + 1));
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) return new double[]{maxW, maxH};
            double escala = Math.min(maxW / img.getWidth(), maxH / img.getHeight());
            return new double[]{img.getWidth() * escala, img.getHeight() * escala};
        } catch (Exception e) {
            return new double[]{maxW, maxH};
        }
    }

    private static String fmtPt(double valor) {
        return String.format(Locale.ROOT, "%.1f", valor);
    }

    private String resolveImageSrc(String src) {
        if (src == null || src.isBlank()) return "";
        if (src.startsWith("data:")) return src;
        // Tenta resolver via MinIO client (autenticado) antes do fallback HTTP
        if (imageResolver != null) {
            String resolved = imageResolver.apply(src);
            if (resolved != null && !resolved.isBlank()) return resolved;
        }
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return ImagemRemota.comoDataUri(src);
        }
        return src;
    }

    static String foEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
