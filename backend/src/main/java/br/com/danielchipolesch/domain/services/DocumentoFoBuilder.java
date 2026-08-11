package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.util.tiptap.TipTapNode;
import br.com.danielchipolesch.domain.util.tiptap.XslFoContentRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a complete XSL-FO document from a Documento entity + section DTOs.
 * The resulting FO string is passed to Apache FOP to produce a PDF.
 */
@Service
public class DocumentoFoBuilder {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImagemService imagemService;

    private String brasaoRepublica = "";
    private String brasaoFab       = "";

    @PostConstruct
    private void loadStaticImages() {
        brasaoRepublica = classpathDataUri("/images/brasao-do-brasil-republica-colorido.png", "image/png");
        brasaoFab       = classpathDataUri("/images/brasao-fab.png",                          "image/png");
    }

    private static String classpathDataUri(String path, String mimeType) {
        try (InputStream is = DocumentoFoBuilder.class.getResourceAsStream(path)) {
            if (is == null) return "";
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception e) {
            return "";
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public String buildFo(Documento doc,
                           List<ItemPartePreliminarResponseDto> preliminares,
                           List<ItemAnexoParteNormativaResponseDto> normativos,
                           List<AnexoResponseDto> anexos) {
        return new Generator(doc,
                preliminares != null ? preliminares : List.of(),
                normativos   != null ? normativos   : List.of(),
                anexos       != null ? anexos       : List.of(),
                brasaoRepublica, brasaoFab, objectMapper, imagemService).build();
    }

    // ─── Stateful generator ───────────────────────────────────────────────────

    private static final class Generator {

        private static final Map<String, String> ESPECIE_COMPLETA = Map.of(
                "ICA",       "INSTRUÇÃO DO COMANDO DA AERONÁUTICA",
                "NSCA",      "NORMA DE SISTEMA DO COMANDO DA AERONÁUTICA",
                "Portaria",  "PORTARIA",
                "Resolução", "RESOLUÇÃO",
                "Decreto",   "DECRETO",
                "Aviso",     "AVISO",
                "Mensagem",  "MENSAGEM"
        );

        private static final String FO_NS = "http://www.w3.org/1999/XSL/Format";

        private final Documento doc;
        private final List<ItemPartePreliminarResponseDto> preliminares;
        private final List<ItemAnexoParteNormativaResponseDto> normativos;
        private final List<AnexoResponseDto> anexos;
        private final String brasaoRepublica;
        private final String brasaoFab;
        private final ObjectMapper objectMapper;
        private final XslFoContentRenderer renderer;
        private final ImagemService imagemService;

        Generator(Documento doc,
                  List<ItemPartePreliminarResponseDto> preliminares,
                  List<ItemAnexoParteNormativaResponseDto> normativos,
                  List<AnexoResponseDto> anexos,
                  String brasaoRepublica,
                  String brasaoFab,
                  ObjectMapper objectMapper,
                  ImagemService imagemService) {
            this.doc            = doc;
            this.preliminares   = preliminares;
            this.normativos     = normativos;
            this.anexos         = anexos;
            this.brasaoRepublica = brasaoRepublica;
            this.brasaoFab      = brasaoFab;
            this.objectMapper   = objectMapper;
            this.imagemService  = imagemService;
            this.renderer       = new XslFoContentRenderer();
            if (imagemService != null) {
                this.renderer.setImageResolver(imagemService::getImageAsDataUri);
            }
        }

        // ─── Entry point ──────────────────────────────────────────────────────

        String build() {
            var sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<fo:root xmlns:fo=\"").append(FO_NS).append("\" xmlns:fox=\"http://xmlgraphics.apache.org/fop/extensions\">\n");
            sb.append(buildLayoutMasterSet());
            sb.append(buildPortariaSequence());
            sb.append(buildCapaSequence());
            sb.append(buildBodySequence());
            for (AnexoResponseDto anexo : anexos) {
                sb.append(buildAnexoSequence(anexo));
            }
            sb.append("</fo:root>");
            return sb.toString();
        }

        // ─── Watermark ────────────────────────────────────────────────────────
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

        private String buildStaticContentWatermark() {
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

        // ─── Page master ──────────────────────────────────────────────────────

        private String buildLayoutMasterSet() {
            return """
                <fo:layout-master-set>
                  <fo:simple-page-master master-name="a4"
                      page-width="21cm" page-height="29.7cm"
                      margin-top="2cm" margin-bottom="2cm"
                      margin-left="2cm" margin-right="2cm">
                    <fo:region-body region-name="xsl-region-body"/>
                    <fo:region-before region-name="wm" extent="0pt" overflow="visible"/>
                    <fo:region-after region-name="xsl-region-after" extent="1cm"/>
                  </fo:simple-page-master>
                  <fo:simple-page-master master-name="a4-nofooter"
                      page-width="21cm" page-height="29.7cm"
                      margin-top="2cm" margin-bottom="2cm"
                      margin-left="2cm" margin-right="2cm">
                    <fo:region-body region-name="xsl-region-body"/>
                    <fo:region-before region-name="wm" extent="0pt" overflow="visible"/>
                  </fo:simple-page-master>
                </fo:layout-master-set>
                """;
        }

        // ─── Page 1: Portaria ─────────────────────────────────────────────────

        private String buildPortariaSequence() {
            var sb = new StringBuilder();
            sb.append("<fo:page-sequence master-reference=\"a4-nofooter\">\n");
            sb.append(buildStaticContentWatermark());
            sb.append("<fo:flow flow-name=\"xsl-region-body\">\n");

            // Cabeçalho
            if (!brasaoRepublica.isBlank()) {
                sb.append("<fo:block text-align=\"center\">")
                  .append("<fo:external-graphic src=\"url('").append(brasaoRepublica).append("')\"")
                  .append(" content-width=\"45pt\" content-height=\"45pt\" scaling=\"uniform\"/>")
                  .append("</fo:block>\n");
            }
            sb.append(block("MINISTÉRIO DA DEFESA", "center", "12pt", "bold", "0", "0"));
            sb.append(block("COMANDO DA AERONÁUTICA", "center", "12pt", "bold", "0", "8pt"));

            // Epígrafe
            var epigrafe = findPreli(ItemAnexoParteNormativaTipoEnum.EPIGRAFE);
            String epiText = inlineTextFrom(epigrafe);
            String epiContent = !epiText.isBlank() ? epiText
                    : "PORTARIA Nº ___, DE " + formatarDataBR(doc.getDtCriacao());
            sb.append("<fo:block text-align=\"center\" font-size=\"12pt\" space-after=\"5pt\">")
              .append(foEsc(epiContent)).append("</fo:block>\n");

            // Ementa (right side, 9cm wide: start-indent = 17cm - 9cm = 8cm)
            var ementa = findPreli(ItemAnexoParteNormativaTipoEnum.EMENTA);
            String ementaInline = inlineFromConteudo(ementa != null ? ementa.getElementContent() : null);
            String ementaDefault = "Aprova a " + especieCompleta()
                    + " que dispõe sobre " + foEsc(doc.getAssuntoBasico().getNome()) + ".";
            sb.append("<fo:block start-indent=\"8cm\" text-align=\"justify\" space-after=\"8pt\" font-size=\"12pt\">")
              .append(!ementaInline.isBlank() ? ementaInline : ementaDefault)
              .append("</fo:block>\n");

            // Preâmbulo (multi-parágrafo, cada um com recuo de 2,5 cm)
            var preambulo = findPreli(ItemAnexoParteNormativaTipoEnum.PREAMBULO);
            if (preambulo != null && preambulo.getElementContent() != null) {
                sb.append(renderParasBlock(preambulo.getElementContent(), "2.5cm", "justify", null));
            } else {
                sb.append("<fo:block text-indent=\"2.5cm\" text-align=\"justify\" space-after=\"5pt\">")
                  .append("<fo:inline font-weight=\"bold\">O COMANDANTE DA AERONÁUTICA</fo:inline>")
                  .append(", no uso das atribuições que lhe confere o art. 12 da Lei Complementar n° 97, de 9 de junho de 1999,")
                  .append("</fo:block>\n");
            }

            // Fecho (alinhado à esquerda)
            var fecho = findPreli(ItemAnexoParteNormativaTipoEnum.FECHO);
            if (fecho != null && fecho.getElementContent() != null) {
                sb.append(renderParasBlock(fecho.getElementContent(), null, "right", "20pt"));
            } else {
                sb.append("<fo:block text-align=\"right\" space-before=\"20pt\" space-after=\"5pt\">")
                  .append("Brasília, ").append(formatarDataBR(doc.getDtCriacao())).append("</fo:block>\n");
            }

            // Assinatura (centralizada)
            var assinatura = findPreli(ItemAnexoParteNormativaTipoEnum.ASSINATURA);
            if (assinatura != null && assinatura.getElementContent() != null) {
                sb.append(renderParasBlock(assinatura.getElementContent(), null, "center", "36pt"));
            } else {
                sb.append(block("Comandante da Aeronáutica", "center", "12pt", "normal", "36pt", "0"));
            }

            sb.append("</fo:flow>\n</fo:page-sequence>\n");
            return sb.toString();
        }

        // ─── Page 2: Capa ─────────────────────────────────────────────────────

        private String buildCapaSequence() {
            var sb = new StringBuilder();
            sb.append("<fo:page-sequence master-reference=\"a4-nofooter\">\n");
            sb.append(buildStaticContentWatermark());
            sb.append("<fo:flow flow-name=\"xsl-region-body\">\n");

            sb.append(block("MINISTÉRIO DA DEFESA", "center", "17pt", "bold", "0", "2pt"));
            sb.append(block("COMANDO DA AERONÁUTICA", "center", "17pt", "bold", "0", "50mm"));

            if (!brasaoFab.isBlank()) {
                sb.append("<fo:block text-align=\"center\" space-after=\"20mm\">")
                  .append("<fo:external-graphic src=\"url('").append(brasaoFab).append("')\"")
                  .append(" content-width=\"180pt\" content-height=\"180pt\" scaling=\"uniform\"/>")
                  .append("</fo:block>\n");
            }

            String assunto = doc.getAssuntoBasico().getNome().toUpperCase();
            sb.append(block(assunto, "center", "21pt", "bold", "0", "15mm"));

            // Legenda box: espécie+número no topo, título no meio, ano na base
            // Tabela externa centra horizontalmente a caixa (3.5 | 10 | 3.5 cm)
            // Tabela interna divide a célula em 3 linhas de altura fixa (1.2+2.6+1.2=5cm)
            String titulo = doc.getTituloDocumento() != null
                    ? doc.getTituloDocumento().toUpperCase() : especieCompleta().toUpperCase();
            String ano = String.valueOf(LocalDate.now().getYear());
            sb.append("<fo:table table-layout=\"fixed\" width=\"17cm\">\n");
            sb.append("  <fo:table-column column-width=\"3.5cm\"/>");
            sb.append("  <fo:table-column column-width=\"10cm\"/>");
            sb.append("  <fo:table-column column-width=\"3.5cm\"/>\n");
            sb.append("  <fo:table-body><fo:table-row height=\"5cm\">\n");
            sb.append("    <fo:table-cell><fo:block/></fo:table-cell>\n");
            sb.append("    <fo:table-cell border=\"1.5pt solid black\" padding=\"0\">\n");
            // Tabela interna com 3 linhas
            sb.append("      <fo:table table-layout=\"fixed\" width=\"100%\">\n");
            sb.append("        <fo:table-column column-width=\"proportional-column-width(1)\"/>\n");
            sb.append("        <fo:table-body>\n");
            // Linha 1 — espécie e número (topo)
            sb.append("          <fo:table-row height=\"1.2cm\">\n");
            sb.append("            <fo:table-cell display-align=\"center\" padding=\"0.1cm 0.5cm\">\n");
            sb.append("              <fo:block text-align=\"center\" font-size=\"12pt\" font-weight=\"bold\">")
              .append(foEsc(docId())).append("</fo:block>\n");
            sb.append("            </fo:table-cell>\n");
            sb.append("          </fo:table-row>\n");
            // Linha 2 — título (meio)
            sb.append("          <fo:table-row height=\"2.6cm\">\n");
            sb.append("            <fo:table-cell display-align=\"center\" padding=\"0 0.5cm\">\n");
            sb.append("              <fo:block text-align=\"center\" font-size=\"12pt\" font-weight=\"bold\">")
              .append(foEsc(titulo)).append("</fo:block>\n");
            sb.append("            </fo:table-cell>\n");
            sb.append("          </fo:table-row>\n");
            // Linha 3 — ano (base)
            sb.append("          <fo:table-row height=\"1.2cm\">\n");
            sb.append("            <fo:table-cell display-align=\"center\" padding=\"0.1cm 0.5cm\">\n");
            sb.append("              <fo:block text-align=\"center\" font-size=\"12pt\" font-weight=\"bold\">")
              .append(ano).append("</fo:block>\n");
            sb.append("            </fo:table-cell>\n");
            sb.append("          </fo:table-row>\n");
            sb.append("        </fo:table-body>\n");
            sb.append("      </fo:table>\n");
            sb.append("    </fo:table-cell>\n");
            sb.append("    <fo:table-cell><fo:block/></fo:table-cell>\n");
            sb.append("  </fo:table-row></fo:table-body>\n</fo:table>\n");

            sb.append("</fo:flow>\n</fo:page-sequence>\n");
            return sb.toString();
        }

        // ─── Pages 3+: Sumário + Corpo ────────────────────────────────────────

        private String buildBodySequence() {
            var sb = new StringBuilder();
            sb.append("<fo:page-sequence master-reference=\"a4\">\n");

            // Footer with page number
            sb.append("<fo:static-content flow-name=\"xsl-region-after\">\n");
            sb.append("  <fo:block text-align=\"right\" font-size=\"10pt\"><fo:page-number/></fo:block>\n");
            sb.append("</fo:static-content>\n");

            sb.append(buildStaticContentWatermark());
            sb.append("<fo:flow flow-name=\"xsl-region-body\">\n");

            String titulo = doc.getTituloDocumento() != null
                    ? doc.getTituloDocumento().toUpperCase() : especieCompleta().toUpperCase();
            sb.append(block("ANEXO I", "center", "12pt", "bold", "0", "0"));
            sb.append(block(foEsc(titulo) + " (" + foEsc(docId()) + ")", "center", "12pt", "bold", "0", "10pt"));
            sb.append(block("SUMÁRIO", "center", "12pt", "bold", "0", "8pt"));

            sb.append(buildToc());
            sb.append("<fo:block space-after=\"1.2em\"/>\n");
            sb.append(buildCorpoNormativo());

            sb.append("</fo:flow>\n</fo:page-sequence>\n");
            return sb.toString();
        }

        // ─── Annexes ──────────────────────────────────────────────────────────

        private String buildAnexoSequence(AnexoResponseDto anexo) {
            var sb = new StringBuilder();
            sb.append("<fo:page-sequence master-reference=\"a4\">\n");
            sb.append("<fo:static-content flow-name=\"xsl-region-after\">\n");
            sb.append("  <fo:block text-align=\"right\" font-size=\"10pt\"><fo:page-number/></fo:block>\n");
            sb.append("</fo:static-content>\n");
            sb.append(buildStaticContentWatermark());
            sb.append("<fo:flow flow-name=\"xsl-region-body\">\n");

            String numRomano = toRoman(anexo.getOrdem() + 1);
            sb.append(block("ANEXO " + numRomano, "center", "12pt", "bold", "0", "4pt"));
            sb.append(block(foEsc(anexo.getTitulo().toUpperCase()), "center", "12pt", "bold", "0", "12pt"));

            if (imagemService != null && anexo.getUrlImagem() != null && !anexo.getUrlImagem().isBlank()) {
                try {
                    String dataUri = imagemService.getImageAsDataUri(anexo.getUrlImagem());
                    if (dataUri != null && !dataUri.isBlank()) {
                        sb.append("<fo:block text-align=\"center\">");
                        sb.append("<fo:external-graphic src=\"url('").append(dataUri).append("')\"");
                        sb.append(" content-width=\"scale-to-fit\" width=\"17cm\" scaling=\"uniform\"/>");
                        sb.append("</fo:block>\n");
                    }
                } catch (Exception ignored) {}
            }

            sb.append("</fo:flow>\n</fo:page-sequence>\n");
            return sb.toString();
        }

        // ─── TOC ──────────────────────────────────────────────────────────────

        private record TocEntry(String label, boolean bold, boolean indent1, boolean indent2, String anchor, String pg) {}

        private String buildToc() {
            List<TocEntry> entries = new ArrayList<>();
            boolean temAgrupamento = normativos.stream().anyMatch(el ->
                    el.getElementType() == ItemAnexoParteNormativaTipoEnum.CAPITULO
                    || el.getElementType() == ItemAnexoParteNormativaTipoEnum.SECAO_NORMATIVA
                    || el.getElementType() == ItemAnexoParteNormativaTipoEnum.SUBSECAO_NORMATIVA);

            Map<Long, Integer> artNums = buildArtNums(normativos);

            if (temAgrupamento) {
                walkToc(normativos, entries, artNums, new int[]{0}, new int[]{0}, new int[]{0});
            } else {
                collectArtToc(normativos, entries, artNums);
            }

            if (entries.isEmpty()) return "";

            var sb = new StringBuilder();
            // Header "Art." right-aligned above the entries
            sb.append("<fo:block font-size=\"10pt\" font-weight=\"bold\" text-align=\"right\" space-after=\"2pt\">Art.</fo:block>\n");
            for (var e : entries) {
                String indent = e.indent2() ? "20pt" : e.indent1() ? "12pt" : "0pt";
                String fw  = e.bold() ? "bold" : "normal";
                String anc = e.anchor();
                String pg  = e.pg();
                // text-align-last="justify" makes fo:leader stretch from label text to the page number
                sb.append("<fo:block font-size=\"10pt\" font-weight=\"").append(fw).append("\"")
                  .append(" start-indent=\"").append(indent).append("\"")
                  .append(" text-align-last=\"justify\"")
                  .append(" space-before=\"").append(e.bold() ? "4pt" : "0").append("\"")
                  .append(" space-after=\"1pt\">\n")
                  .append("  <fo:basic-link internal-destination=\"").append(anc).append("\" color=\"#000000\">")
                  .append(foEsc(e.label()))
                  .append("</fo:basic-link>")
                  .append("<fo:leader leader-pattern=\"dots\" leader-alignment=\"reference-area\"/>")
                  .append(pg.isBlank() ? "" :
                      "<fo:basic-link internal-destination=\"" + anc + "\" color=\"#000000\">"
                      + foEsc(pg) + "</fo:basic-link>")
                  .append("\n</fo:block>\n");
            }
            return sb.toString();
        }

        private Map<Long, Integer> buildArtNums(List<ItemAnexoParteNormativaResponseDto> items) {
            Map<Long, Integer> map = new HashMap<>();
            assignArtNumsRec(items, new int[]{0}, map);
            return map;
        }

        private void assignArtNumsRec(List<ItemAnexoParteNormativaResponseDto> items,
                                      int[] counter, Map<Long, Integer> map) {
            if (items == null) return;
            for (var item : items) {
                if (item.getElementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) {
                    counter[0]++;
                    map.put(item.getId(), counter[0]);
                }
                assignArtNumsRec(item.getChildren(), counter, map);
            }
        }

        private int firstArtigoNum(List<ItemAnexoParteNormativaResponseDto> items, Map<Long, Integer> artNums) {
            if (items == null) return -1;
            for (var item : items) {
                if (item.getElementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) {
                    int n = artNums.getOrDefault(item.getId(), -1);
                    if (n > 0) return n;
                }
                int found = firstArtigoNum(item.getChildren(), artNums);
                if (found > 0) return found;
            }
            return -1;
        }

        private int lastArtigoNum(List<ItemAnexoParteNormativaResponseDto> items, Map<Long, Integer> artNums) {
            if (items == null) return -1;
            int last = -1;
            for (var item : items) {
                if (item.getElementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) {
                    int n = artNums.getOrDefault(item.getId(), -1);
                    if (n > 0) last = n;
                }
                int childLast = lastArtigoNum(item.getChildren(), artNums);
                if (childLast > 0) last = childLast;
            }
            return last;
        }

        private String fmtRange(List<ItemAnexoParteNormativaResponseDto> children, Map<Long, Integer> artNums) {
            int first = firstArtigoNum(children, artNums);
            if (first < 0) return "";
            int last = lastArtigoNum(children, artNums);
            if (last < 0) last = first;
            return first == last ? fmtNum(first) : fmtNum(first) + "/" + fmtNum(last);
        }

        private static final java.util.Set<ItemAnexoParteNormativaTipoEnum> GROUPING_TYPES = java.util.Set.of(
                ItemAnexoParteNormativaTipoEnum.CAPITULO,
                ItemAnexoParteNormativaTipoEnum.SECAO_NORMATIVA,
                ItemAnexoParteNormativaTipoEnum.SUBSECAO_NORMATIVA);

        /** Range of artigos belonging to this grouping element.
         *  Checks children (tree structure) AND following siblings until the next
         *  grouping element (flat structure where artigos are siblings of sections). */
        private String artRangeFor(ItemAnexoParteNormativaResponseDto item,
                                   List<ItemAnexoParteNormativaResponseDto> siblings,
                                   int idx,
                                   Map<Long, Integer> artNums) {
            int first = firstArtigoNum(item.getChildren(), artNums);
            int last  = lastArtigoNum (item.getChildren(), artNums);
            for (int j = idx + 1; j < siblings.size(); j++) {
                var sib = siblings.get(j);
                if (GROUPING_TYPES.contains(sib.getElementType())) break;
                if (sib.getElementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) {
                    int n = artNums.getOrDefault(sib.getId(), -1);
                    if (n > 0) {
                        if (first < 0) first = n; else first = Math.min(first, n);
                        if (last  < 0) last  = n; else last  = Math.max(last,  n);
                    }
                }
            }
            if (first < 0) return "";
            if (last  < 0) last = first;
            return first == last ? fmtNum(first) : fmtNum(first) + "/" + fmtNum(last);
        }

        private void walkToc(List<ItemAnexoParteNormativaResponseDto> items, List<TocEntry> entries,
                             Map<Long, Integer> artNums, int[] cap, int[] sec, int[] sub) {
            if (items == null) return;
            for (int i = 0; i < items.size(); i++) {
                var item = items.get(i);
                switch (item.getElementType()) {
                    case CAPITULO -> {
                        cap[0]++; sec[0] = 0; sub[0] = 0;
                        String t = item.getElementTitle() != null ? " - " + item.getElementTitle().toUpperCase() : "";
                        entries.add(new TocEntry("CAPÍTULO " + toRoman(cap[0]) + t, true, false, false,
                                "norm-" + item.getId(), artRangeFor(item, items, i, artNums)));
                        walkToc(item.getChildren(), entries, artNums, cap, sec, sub);
                    }
                    case SECAO_NORMATIVA -> {
                        sec[0]++; sub[0] = 0;
                        String t = item.getElementTitle() != null ? " - " + item.getElementTitle() : "";
                        entries.add(new TocEntry("Seção " + toRoman(sec[0]) + t, false, true, false,
                                "norm-" + item.getId(), artRangeFor(item, items, i, artNums)));
                        walkToc(item.getChildren(), entries, artNums, cap, sec, sub);
                    }
                    case SUBSECAO_NORMATIVA -> {
                        sub[0]++;
                        String t = item.getElementTitle() != null ? " - " + item.getElementTitle() : "";
                        entries.add(new TocEntry("Subseção " + toRoman(sub[0]) + t, false, true, true,
                                "norm-" + item.getId(), artRangeFor(item, items, i, artNums)));
                        walkToc(item.getChildren(), entries, artNums, cap, sec, sub);
                    }
                    default -> {}
                }
            }
        }

        private void collectArtToc(List<ItemAnexoParteNormativaResponseDto> items,
                                   List<TocEntry> entries, Map<Long, Integer> artNums) {
            for (var item : items) {
                if (item.getElementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) {
                    int n = artNums.getOrDefault(item.getId(), 0);
                    String pg = n > 0 ? fmtNum(n) : "";
                    entries.add(new TocEntry("Art. " + (n > 0 ? ordinalOrCardinal(n) : "?"), false, true, false,
                            "norm-" + item.getId(), pg));
                }
            }
        }

        // ─── Corpo Normativo ──────────────────────────────────────────────────

        private String buildCorpoNormativo() {
            var sb = new StringBuilder();
            renderNormItems(normativos, sb, new int[]{0}, new int[]{0}, new int[]{0}, new int[]{0});
            return sb.toString();
        }

        private void renderNormItems(List<ItemAnexoParteNormativaResponseDto> items,
                                     StringBuilder sb, int[] cap, int[] sec, int[] sub, int[] art) {
            if (items == null) return;
            for (var item : items) renderNormItem(item, sb, cap, sec, sub, art);
        }

        private void renderNormItem(ItemAnexoParteNormativaResponseDto item,
                                    StringBuilder sb, int[] cap, int[] sec, int[] sub, int[] art) {
            String anc = "norm-" + item.getId();
            switch (item.getElementType()) {
                case CAPITULO -> {
                    cap[0]++; sec[0] = 0; sub[0] = 0;
                    sb.append("<fo:block id=\"").append(anc).append("\"")
                      .append(" text-align=\"center\" font-weight=\"bold\" space-before=\"15pt\" space-after=\"3pt\">")
                      .append("CAPÍTULO ").append(toRoman(cap[0])).append("</fo:block>\n");
                    if (item.getElementTitle() != null && !item.getElementTitle().isBlank()) {
                        sb.append("<fo:block text-align=\"center\" font-weight=\"bold\" space-after=\"6pt\">")
                          .append(foEsc(item.getElementTitle().toUpperCase())).append("</fo:block>\n");
                    }
                    renderNormItems(item.getChildren(), sb, cap, sec, sub, art);
                }
                case SECAO_NORMATIVA -> {
                    sec[0]++; sub[0] = 0;
                    sb.append("<fo:block id=\"").append(anc).append("\"")
                      .append(" text-align=\"center\" font-weight=\"bold\" space-before=\"10pt\" space-after=\"3pt\">")
                      .append("Seção ").append(toRoman(sec[0])).append("</fo:block>\n");
                    if (item.getElementTitle() != null && !item.getElementTitle().isBlank()) {
                        sb.append("<fo:block text-align=\"center\" font-weight=\"bold\" space-after=\"5pt\">")
                          .append(foEsc(item.getElementTitle())).append("</fo:block>\n");
                    }
                    renderNormItems(item.getChildren(), sb, cap, sec, sub, art);
                }
                case SUBSECAO_NORMATIVA -> {
                    sub[0]++;
                    sb.append("<fo:block id=\"").append(anc).append("\"")
                      .append(" text-align=\"center\" font-weight=\"bold\" space-before=\"8pt\" space-after=\"3pt\">")
                      .append("Subseção ").append(toRoman(sub[0])).append("</fo:block>\n");
                    if (item.getElementTitle() != null && !item.getElementTitle().isBlank()) {
                        sb.append("<fo:block text-align=\"center\" font-weight=\"bold\" space-after=\"5pt\">")
                          .append(foEsc(item.getElementTitle())).append("</fo:block>\n");
                    }
                    renderNormItems(item.getChildren(), sb, cap, sec, sub, art);
                }
                case ARTIGO -> {
                    art[0]++;
                    renderBodyEl(sb, anc, "Art. " + ordinalOrCardinal(art[0]) + "  ", true,
                            item.getElementContent(), item.getEmendaStatus(), item.getConteudoOriginal());
                    renderArtigoFilhos(item.getChildren(), sb);
                }
                default -> renderBodyEl(sb, null, "", false, item.getElementContent(),
                        item.getEmendaStatus(), item.getConteudoOriginal());
            }
        }

        private void renderArtigoFilhos(List<ItemAnexoParteNormativaResponseDto> filhos, StringBuilder sb) {
            if (filhos == null) return;
            long parCount = filhos.stream()
                    .filter(c -> c.getElementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO
                              || c.getElementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO)
                    .count();
            int parNum = 0, incisoNum = 0;
            for (var child : filhos) {
                switch (child.getElementType()) {
                    case PARAGRAFO, PARAGRAFO_UNICO -> {
                        parNum++;
                        boolean unico = parCount == 1 && child.getElementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO;
                        renderBodyEl(sb, null, unico ? "Parágrafo único.  " : "§ " + ordinalOrCardinal(parNum) + "  ",
                                false, child.getElementContent(), child.getEmendaStatus(), child.getConteudoOriginal());
                        renderIncisoFilhos(child.getChildren(), sb);
                    }
                    case INCISO -> {
                        incisoNum++;
                        renderBodyEl(sb, null, toRoman(incisoNum) + " - ", false, child.getElementContent(),
                                child.getEmendaStatus(), child.getConteudoOriginal());
                        renderAlineaFilhos(child.getChildren(), sb);
                    }
                    default -> renderBodyEl(sb, null, "", false, child.getElementContent(),
                            child.getEmendaStatus(), child.getConteudoOriginal());
                }
            }
        }

        private void renderIncisoFilhos(List<ItemAnexoParteNormativaResponseDto> filhos, StringBuilder sb) {
            if (filhos == null) return;
            int n = 0;
            for (var child : filhos) {
                if (child.getElementType() == ItemAnexoParteNormativaTipoEnum.INCISO) {
                    n++;
                    renderBodyEl(sb, null, toRoman(n) + " - ", false, child.getElementContent(),
                            child.getEmendaStatus(), child.getConteudoOriginal());
                    renderAlineaFilhos(child.getChildren(), sb);
                }
            }
        }

        private void renderAlineaFilhos(List<ItemAnexoParteNormativaResponseDto> filhos, StringBuilder sb) {
            if (filhos == null) return;
            int n = 0;
            for (var child : filhos) {
                if (child.getElementType() == ItemAnexoParteNormativaTipoEnum.ALINEA) {
                    n++;
                    renderBodyEl(sb, null, toLetter(n) + ") ", false, child.getElementContent(),
                            child.getEmendaStatus(), child.getConteudoOriginal());
                    renderSubAlineaFilhos(child.getChildren(), sb);
                }
            }
        }

        private void renderSubAlineaFilhos(List<ItemAnexoParteNormativaResponseDto> filhos, StringBuilder sb) {
            if (filhos == null) return;
            int n = 0;
            for (var child : filhos) {
                if (child.getElementType() == ItemAnexoParteNormativaTipoEnum.SUB_ALINEA) {
                    n++;
                    renderBodyEl(sb, null, n + ". ", false, child.getElementContent(),
                            child.getEmendaStatus(), child.getConteudoOriginal());
                }
            }
        }

        private void renderBodyEl(StringBuilder sb, String id, String label, boolean labelBold, String conteudo) {
            String idAttr = (id != null && !id.isBlank()) ? " id=\"" + id + "\"" : "";
            if (conteudo == null || conteudo.isBlank()) {
                sb.append("<fo:block").append(idAttr).append(" text-indent=\"2.5cm\" space-after=\"5pt\">")
                  .append(labelFo(label, labelBold)).append("</fo:block>\n");
                return;
            }
            TipTapNode doc = parseConteudo(conteudo);
            if (doc == null) {
                sb.append("<fo:block").append(idAttr).append(" text-indent=\"2.5cm\" space-after=\"5pt\">")
                  .append(labelFo(label, labelBold)).append("</fo:block>\n");
                return;
            }
            boolean blockContent = renderer.hasBlockContent(doc);
            if (blockContent) {
                if (!label.isBlank() && renderer.startsWithParagraph(doc)) {
                    // Label + first paragraph text on the same line; remaining blocks (figures, etc.) follow
                    sb.append("<fo:block").append(idAttr)
                      .append(" text-indent=\"2.5cm\" space-after=\"2pt\" text-align=\"justify\">")
                      .append(labelFo(label, labelBold))
                      .append(renderer.renderInlineContent(doc))
                      .append("</fo:block>\n");
                    sb.append(renderer.renderSkippingFirstParagraph(doc));
                } else {
                    if (!label.isBlank()) {
                        sb.append("<fo:block").append(idAttr)
                          .append(" text-indent=\"2.5cm\" space-after=\"2pt\" text-align=\"justify\">")
                          .append(labelFo(label, labelBold)).append("</fo:block>\n");
                    }
                    sb.append(renderer.renderDocContent(doc));
                }
            } else {
                sb.append("<fo:block").append(idAttr).append(" text-indent=\"2.5cm\" space-after=\"5pt\" text-align=\"justify\">")
                  .append(labelFo(label, labelBold))
                  .append(renderer.renderInlineContent(doc))
                  .append("</fo:block>\n");
            }
        }

        // Overload aware of emenda status — falls through to the plain version for INALTERADO
        private void renderBodyEl(StringBuilder sb, String id, String label, boolean labelBold,
                                  String conteudo,
                                  ElementoEmendaStatusEnum emendaStatus,
                                  String conteudoOriginal) {
            if (emendaStatus == null || emendaStatus == ElementoEmendaStatusEnum.INALTERADO) {
                renderBodyEl(sb, id, label, labelBold, conteudo);
                return;
            }
            String idAttr = (id != null && !id.isBlank()) ? " id=\"" + id + "\"" : "";
            switch (emendaStatus) {
                case REVOGADO -> {
                    renderStrikethroughBlock(sb, idAttr, label, labelBold, conteudoOriginal);
                    renderEmendaRefBlock(sb, emendaStatus);
                }
                case ALTERADO -> {
                    renderStrikethroughBlock(sb, idAttr, label, labelBold, conteudoOriginal);
                    renderBodyEl(sb, null, label, labelBold, conteudo);
                    renderEmendaRefBlock(sb, emendaStatus);
                }
                case INCLUIDO -> {
                    renderIncludidoBlock(sb, idAttr, label, labelBold, conteudo);
                    renderEmendaRefBlock(sb, emendaStatus);
                }
            }
        }

        private void renderStrikethroughBlock(StringBuilder sb, String idAttr,
                                               String label, boolean labelBold, String conteudo) {
            if (conteudo == null || conteudo.isBlank()) {
                sb.append("<fo:block").append(idAttr)
                  .append(" text-indent=\"2.5cm\" space-after=\"2pt\" text-align=\"justify\"")
                  .append(" text-decoration=\"line-through\" color=\"#777777\">")
                  .append(labelFo(label, labelBold))
                  .append("</fo:block>\n");
                return;
            }
            TipTapNode doc = parseConteudo(conteudo);
            if (doc == null) {
                sb.append("<fo:block").append(idAttr)
                  .append(" text-indent=\"2.5cm\" space-after=\"2pt\" text-align=\"justify\"")
                  .append(" text-decoration=\"line-through\" color=\"#777777\">")
                  .append(labelFo(label, labelBold))
                  .append(foEsc(conteudo))
                  .append("</fo:block>\n");
                return;
            }
            java.util.List<TipTapNode> paras = doc.getContent() != null
                    ? doc.getContent().stream()
                         .filter(n -> "paragraph".equals(n.getType()))
                         .collect(java.util.stream.Collectors.toList())
                    : java.util.List.of();
            if (paras.isEmpty()) {
                sb.append("<fo:block").append(idAttr)
                  .append(" text-indent=\"2.5cm\" space-after=\"2pt\" text-align=\"justify\"")
                  .append(" text-decoration=\"line-through\" color=\"#777777\">")
                  .append(labelFo(label, labelBold))
                  .append("</fo:block>\n");
                return;
            }
            boolean first = true;
            for (var para : paras) {
                sb.append("<fo:block").append(first ? idAttr : "")
                  .append(" text-indent=\"2.5cm\" space-after=\"2pt\" text-align=\"justify\"")
                  .append(" text-decoration=\"line-through\" color=\"#777777\">");
                if (first) sb.append(labelFo(label, labelBold));
                sb.append(renderer.renderParagraphInlines(para));
                sb.append("</fo:block>\n");
                first = false;
            }
        }

        private void renderIncludidoBlock(StringBuilder sb, String idAttr,
                                          String label, boolean labelBold, String conteudo) {
            TipTapNode node = parseConteudo(conteudo);
            String inline = node != null ? renderer.renderInlineContent(node) : foEsc(conteudo);
            sb.append("<fo:block").append(idAttr)
              .append(" text-indent=\"2.5cm\" space-after=\"2pt\" text-align=\"justify\"")
              .append(" color=\"#004400\">")
              .append(labelFo(label, labelBold))
              .append(inline)
              .append("</fo:block>\n");
        }

        private void renderEmendaRefBlock(StringBuilder sb, ElementoEmendaStatusEnum status) {
            String acao = switch (status) {
                case ALTERADO -> "alterado";
                case REVOGADO -> "revogado";
                case INCLUIDO -> "incluído";
                default -> "modificado";
            };
            String portaria = doc.getPortariaReferencia();
            String bca      = doc.getBcaReferencia();
            String ref;
            if (portaria != null && !portaria.isBlank() && bca != null && !bca.isBlank()) {
                ref = "(" + acao + " pela " + portaria + ", publicada no " + bca + ")";
            } else {
                ref = "(" + acao + " pela Portaria DIRAD n° XYZ, de DD de MÊS de AAAA,"
                    + " publicada no BCA n° ABC, de DD de mês de AAAA)";
            }
            sb.append("<fo:block font-size=\"10pt\" font-style=\"italic\" color=\"#555555\"")
              .append(" start-indent=\"2.5cm\" space-after=\"3pt\">")
              .append(foEsc(ref))
              .append("</fo:block>\n");
        }

        private TipTapNode parseConteudo(String conteudo) {
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
        private String renderParasBlock(String conteudo, String textIndent, String textAlign, String spaceBefore) {
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

        private String inlineFromConteudo(String conteudo) {
            return foFromConteudo(conteudo, false);
        }

        private String inlineTextFrom(ItemPartePreliminarResponseDto item) {
            if (item == null || item.getElementContent() == null) return "";
            try {
                TipTapNode doc = objectMapper.readValue(item.getElementContent(), TipTapNode.class);
                return extractPlainText(doc);
            } catch (Exception ignored) {
                return "";
            }
        }

        private String extractPlainText(TipTapNode node) {
            if (node == null) return "";
            if ("text".equals(node.getType())) return node.getText() != null ? node.getText() : "";
            var sb = new StringBuilder();
            if (node.getContent() != null)
                for (var c : node.getContent()) sb.append(extractPlainText(c));
            return sb.toString().trim();
        }

        private ItemPartePreliminarResponseDto findPreli(ItemAnexoParteNormativaTipoEnum tipo) {
            return preliminares.stream()
                    .filter(p -> p.getElementType() == tipo)
                    .findFirst().orElse(null);
        }

        // ─── FO helpers ───────────────────────────────────────────────────────

        private static String block(String text, String align, String size,
                                    String weight, String spaceBefore, String spaceAfter) {
            return "<fo:block text-align=\"" + align + "\" font-size=\"" + size + "\""
                 + " font-weight=\"" + weight + "\""
                 + " space-before=\"" + spaceBefore + "\" space-after=\"" + spaceAfter + "\">"
                 + text + "</fo:block>\n";
        }

        private static String labelFo(String label, boolean bold) {
            if (label == null || label.isBlank()) return "";
            if (bold) return "<fo:inline font-weight=\"bold\">" + foEsc(label) + "</fo:inline>";
            return foEsc(label);
        }

        // ─── Numbering ────────────────────────────────────────────────────────

        private static String toRoman(int n) {
            if (n <= 0) return "";
            int[]    vals = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
            String[] syms = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
            var sb = new StringBuilder();
            for (int i = 0; i < vals.length; i++)
                while (n >= vals[i]) { sb.append(syms[i]); n -= vals[i]; }
            return sb.toString();
        }

        private static String toLetter(int n) { return String.valueOf((char) ('a' + n - 1)); }
        private static String ordinalOrCardinal(int n) { return n <= 9 ? n + "º" : n + "."; }
        private static String fmtNum(int n) { return n <= 9 ? n + "º" : String.valueOf(n); }

        private String docId() {
            return doc.getEspecieNormativa().getSigla()
                    + " " + doc.getAssuntoBasico().getCodigo()
                    + "-" + doc.getNumeroSecundario();
        }

        private String especieCompleta() {
            return ESPECIE_COMPLETA.getOrDefault(doc.getEspecieNormativa().getSigla(),
                    doc.getEspecieNormativa().getSigla().toUpperCase());
        }

        private static String formatarDataBR(Timestamp ts) {
            if (ts == null) return "___________";
            LocalDate d = ts.toLocalDateTime().toLocalDate();
            String[] meses = {"janeiro","fevereiro","março","abril","maio","junho",
                    "julho","agosto","setembro","outubro","novembro","dezembro"};
            return d.getDayOfMonth() + " de " + meses[d.getMonthValue() - 1] + " de " + d.getYear();
        }

        static String foEsc(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
