package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.util.tiptap.TipTapNode;
import br.com.danielchipolesch.domain.util.tiptap.XslFoContentRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Estado e utilitários de baixo nível compartilhados entre DocumentoFoFrontMatterBuilder
// e DocumentoFoCorpoBuilder — uma instância nova por chamada de
// DocumentoFoBuilder.buildFo(), nunca reutilizada entre requisições (evita estado
// mutável num bean singleton do Spring). Nenhuma composição de página mora aqui, só
// conversão TipTap → FO/texto e formatação pontual (data, marca d'água, escape de FO).
final class DocumentoFoContext {

    private static final Map<String, String> ESPECIE_COMPLETA = Map.of(
            "ICA",       "INSTRUÇÃO DO COMANDO DA AERONÁUTICA",
            "NSCA",      "NORMA DE SISTEMA DO COMANDO DA AERONÁUTICA",
            "Portaria",  "PORTARIA",
            "Resolução", "RESOLUÇÃO",
            "Decreto",   "DECRETO",
            "Aviso",     "AVISO",
            "Mensagem",  "MENSAGEM"
    );

    final Documento doc;
    final List<ItemPartePreliminarResponseDto> preliminares;
    final ObjectMapper objectMapper;
    final XslFoContentRenderer renderer;

    DocumentoFoContext(Documento doc, List<ItemPartePreliminarResponseDto> preliminares,
                        ObjectMapper objectMapper, XslFoContentRenderer renderer) {
        this.doc = doc;
        this.preliminares = preliminares;
        this.objectMapper = objectMapper;
        this.renderer = renderer;
    }

    // ─── Marca d'água ───────────────────────────────────────────────────────────
    // Rendered as fo:static-content so it repeats on EVERY page of the sequence.
    // Uses fox:transform="rotate(-45)" — rotation around the container's local
    // origin (0, 0). The container is pre-positioned so the text centre lands
    // exactly at the A4 page centre (297, 421) after rotation.
    //
    // After rotate(-45) around local (0,0), local point (cw, cy) maps to:
    //   screen_x = left + (cw + cy) * 0.707
    //   screen_y = top  + (cy - cw) * 0.707
    //
    // Container: 500 × 150 pt.  Text centre: cw=250, cy=75 (space-before≈32pt).
    //   left = 297 − (250+75)*0.707 = 297 − 229.8 ≈  67 pt
    //   top  = 421 − (75−250)*0.707 = 421 + 123.7 ≈ 545 pt

    String buildStaticContentWatermark() {
        String open  = "<fo:static-content flow-name=\"wm\">\n";
        String close = "</fo:static-content>\n";
        DocumentoStatusEnum status = doc.getDocumentoStatus();
        boolean showWm = status == DocumentoStatusEnum.RASCUNHO
                      || status == DocumentoStatusEnum.MINUTA
                      || status == DocumentoStatusEnum.EM_ALTERACAO;
        if (!showWm) return open + "  <fo:block/>\n" + close;

        String label = status == DocumentoStatusEnum.EM_ALTERACAO ? "EM ALTERAÇÃO" : foEsc(status.name());
        // EM_ALTERACAO uses orange-toned color; draft/minuta use the existing pink
        String color = status == DocumentoStatusEnum.EM_ALTERACAO ? "#DDCCAA" : "#DDBBBB";
        var sb = new StringBuilder();
        sb.append(open);
        sb.append("  <fo:block-container absolute-position=\"fixed\"");
        sb.append(" top=\"545pt\" left=\"87pt\" width=\"500pt\" height=\"150pt\"");
        sb.append(" overflow=\"visible\"");
        sb.append(" fox:transform=\"rotate(-45)\">\n");
        sb.append("    <fo:block font-size=\"62pt\" font-weight=\"bold\" color=\"").append(color).append("\"");
        sb.append(" text-align=\"center\" space-before=\"32pt\">").append(foEsc(label)).append("</fo:block>\n");
        sb.append("  </fo:block-container>\n");
        sb.append(close);
        return sb.toString();
    }

    // ─── Identificação do documento ─────────────────────────────────────────────

    String docId() {
        return doc.getEspecieNormativa().getSigla()
                + " " + doc.getAssuntoBasico().getCodigo()
                + "-" + doc.getNumeroSecundario();
    }

    String especieCompleta() {
        return ESPECIE_COMPLETA.getOrDefault(doc.getEspecieNormativa().getSigla(),
                doc.getEspecieNormativa().getSigla().toUpperCase());
    }

    static String formatarDataBR(Timestamp ts) {
        if (ts == null) return "___________";
        LocalDate d = ts.toLocalDateTime().toLocalDate();
        String[] meses = {"janeiro","fevereiro","março","abril","maio","junho",
                "julho","agosto","setembro","outubro","novembro","dezembro"};
        return d.getDayOfMonth() + " de " + meses[d.getMonthValue() - 1] + " de " + d.getYear();
    }

    // ─── TipTap → FO/texto ──────────────────────────────────────────────────────

    TipTapNode parseConteudo(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) return null;
        try {
            return objectMapper.readValue(conteudo, TipTapNode.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Renders each paragraph in a TipTap doc as an fo:block with the given indent/alignment.
     * spaceBefore is applied only to the first block; pass null to omit.
     */
    String renderParasBlock(String conteudo, String textIndent, String textAlign, String spaceBefore) {
        if (conteudo == null || conteudo.isBlank()) return "";
        try {
            TipTapNode tipTapDoc = objectMapper.readValue(conteudo, TipTapNode.class);
            if (tipTapDoc.getContent() == null) return "";
            var sb = new StringBuilder();
            boolean first = true;
            for (var para : tipTapDoc.getContent()) {
                if (!"paragraph".equals(para.getType())) continue;
                String before = (first && spaceBefore != null)
                        ? " space-before=\"" + spaceBefore + "\"" : "";
                String indent = textIndent != null
                        ? " text-indent=\"" + textIndent + "\"" : "";
                sb.append("<fo:block").append(indent)
                  .append(" text-align=\"").append(textAlign != null ? textAlign : "justify").append("\"")
                  .append(before)
                  .append(" space-after=\"5pt\">")
                  .append(renderer.renderParagraphInlines(para))
                  .append("</fo:block>\n");
                first = false;
            }
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String foFromConteudo(String conteudo, boolean asBlocks) {
        if (conteudo == null || conteudo.isBlank()) return "";
        try {
            TipTapNode doc = objectMapper.readValue(conteudo, TipTapNode.class);
            return asBlocks ? renderer.renderDocContent(doc) : renderer.renderInlineContent(doc);
        } catch (Exception ignored) {
            return "";
        }
    }

    String inlineFromConteudo(String conteudo) {
        return foFromConteudo(conteudo, false);
    }

    String inlineTextFrom(ItemPartePreliminarResponseDto item) {
        if (item == null || item.getElementContent() == null) return "";
        try {
            TipTapNode doc = objectMapper.readValue(item.getElementContent(), TipTapNode.class);
            return extractPlainText(doc);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String extractPlainText(TipTapNode node) {
        if (node == null) return "";
        if ("text".equals(node.getType())) return node.getText() != null ? node.getText() : "";
        var sb = new StringBuilder();
        if (node.getContent() != null)
            for (var c : node.getContent()) sb.append(extractPlainText(c));
        return sb.toString().trim();
    }

    ItemPartePreliminarResponseDto findPreli(ItemAnexoParteNormativaTipoEnum tipo) {
        return preliminares.stream()
                .filter(p -> p.getElementType() == tipo)
                .findFirst().orElse(null);
    }

    // ─── FO helpers ───────────────────────────────────────────────────────────

    static String block(String text, String align, String size,
                         String weight, String spaceBefore, String spaceAfter) {
        return "<fo:block text-align=\"" + align + "\" font-size=\"" + size + "\""
             + " font-weight=\"" + weight + "\""
             + " space-before=\"" + spaceBefore + "\" space-after=\"" + spaceAfter + "\">"
             + text + "</fo:block>\n";
    }

    static String labelFo(String label, boolean bold) {
        if (label == null || label.isBlank()) return "";
        if (bold) return "<fo:inline font-weight=\"bold\">" + foEsc(label) + "</fo:inline>";
        return foEsc(label);
    }

    static String foEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
