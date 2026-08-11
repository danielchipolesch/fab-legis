package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.util.tiptap.TipTapHtmlSerializer;
import br.com.danielchipolesch.domain.util.tiptap.TipTapNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DocumentoHtmlService {

    @Autowired
    private ObjectMapper objectMapper;

    private String brasaoRepublica = "";
    private String brasaoFab       = "";

    @PostConstruct
    private void loadStaticImages() {
        brasaoRepublica = classpathDataUri("/images/brasao-do-brasil-republica-colorido.png", "image/png");
        brasaoFab       = classpathDataUri("/images/brasao-fab.png", "image/png");
    }

    private static String classpathDataUri(String path, String mimeType) {
        try (InputStream is = DocumentoHtmlService.class.getResourceAsStream(path)) {
            if (is == null) return "";
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception e) {
            return "";
        }
    }

    public String gerarHtml(
            Documento doc,
            List<ItemPartePreliminarResponseDto> preliminares,
            List<ItemAnexoParteNormativaResponseDto> normativos) {
        return new Generator(doc, preliminares, normativos, brasaoRepublica, brasaoFab, objectMapper).gerar();
    }

    // ─── Stateful generator: one instance per call ───────────────────────────────

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

        private static final Pattern BLOCK_PATTERN =
                Pattern.compile("(?i)<(table|ul|ol|blockquote|h[1-6]|figure)[\\s>]");

        // Void elements sem auto-fechamento causam SAXParseException no parser XHTML do OpenHTMLToPDF
        private static final Pattern VOID_SELF_CLOSE = Pattern.compile(
                "(?i)<(area|base|br|col|embed|hr|img|input|link|meta|param|source|track|wbr)((?:\\s[^>]*)?)(?<!/)>");

        // Substituir src="http://..." por data URIs para garantir renderização offline/Docker
        private static final Pattern IMG_SRC_HTTP = Pattern.compile(
                "src=([\"'])(https?://[^\"'\\s]+)\\1", Pattern.CASE_INSENSITIVE);

        private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();

        private static final String S2 = "  ";
        private static final String S1 = " ";

        private final Documento doc;
        private final List<ItemPartePreliminarResponseDto> preliminares;
        private final List<ItemAnexoParteNormativaResponseDto> normativos;
        private final String brasaoRepublica;
        private final String brasaoFab;
        private final ObjectMapper objectMapper;

        private int artCount = 0;

        Generator(Documento doc,
                  List<ItemPartePreliminarResponseDto> preliminares,
                  List<ItemAnexoParteNormativaResponseDto> normativos,
                  String brasaoRepublica,
                  String brasaoFab,
                  ObjectMapper objectMapper) {
            this.doc = doc;
            this.preliminares    = preliminares != null ? preliminares : List.of();
            this.normativos      = normativos   != null ? normativos   : List.of();
            this.brasaoRepublica = brasaoRepublica;
            this.brasaoFab       = brasaoFab;
            this.objectMapper    = objectMapper;
        }

        // ─── Entry point ─────────────────────────────────────────────────────────

        String gerar() {
            return "<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"/>\n<style>\n"
                    + buildCss()
                    + "\n</style>\n</head>\n<body>\n"
                    + buildPortaria()
                    + buildCapa()
                    + buildSumarioECorpo()
                    + "\n</body>\n</html>";
        }

        // ─── CSS ─────────────────────────────────────────────────────────────────

        private String buildCss() {
            return """
                @page { size: A4; margin: 2cm; }
                * { box-sizing: border-box; }
                body {
                    font-family: Arial, Helvetica, sans-serif;
                    font-size: 12pt;
                    line-height: 1.2;
                    color: #000;
                    text-align: justify;
                    margin: 0;
                }
                .page-break { page-break-after: always; }
                .cabecalho { text-align: center; margin-bottom: 10pt; }
                .cabecalho p { margin: 0; text-align: center; font-size: 12pt; }
                .cabecalho-brasao { width: 45pt; height: 45pt; display: block; margin: 0 auto 4pt; }
                .bold { font-weight: bold; }
                .underline { text-decoration: underline; }
                .epigrafe { text-align: center; text-transform: uppercase; font-weight: normal; margin: 10pt 0 0; }
                .epigrafe p { display: inline; margin: 0; }
                .ementa-bloco { margin-left: 8cm; margin-top: 3pt; margin-bottom: 8pt; }
                .ementa-txt { margin: 0; text-align: justify; }
                .ementa-txt p { display: inline; margin: 0; }
                .body-el { text-indent: 2.5cm; margin: 0 0 5pt; line-height: 1.2; text-align: justify; }
                .body-el p { display: inline; margin: 0; }
                .body-el p + p { display: block; margin-top: 5pt; text-indent: 2.5cm; }
                .body-el strong { font-weight: bold; }
                .body-el em { font-style: italic; }
                .body-el u { text-decoration: underline; }
                .norm-lbl { font-weight: normal; }
                .norm-lbl-bold { font-weight: bold; }
                .norm-content-block { display: inline; text-indent: 0; }
                .norm-content-block table { border-collapse: collapse; width: 100%; margin: 4pt 0; font-size: 10pt; }
                .norm-content-block td, .norm-content-block th { border: 1px solid #999; padding: 3pt 6pt; vertical-align: top; }
                .norm-content-block th { background: rgba(11,61,145,0.06); font-weight: bold; text-align: center; }
                .norm-content-block p { margin: 0; text-indent: 0; }
                .fecho-bloco { text-align: left; margin-top: 20pt; margin-bottom: 5pt; }
                .fecho-bloco p { text-align: left; text-indent: 0; margin: 0; display: block; }
                .assinatura-bloco { margin-top: 36pt; text-align: center; }
                .assinatura-bloco p { margin: 0; text-align: center; text-indent: 0; display: block; }
                .assin-data { margin: 0 0 24pt; }
                .assin-nome { text-transform: uppercase; margin: 0; }
                .assin-cargo { margin: 0; }
                .corpo-assin { margin-top: 24pt; text-align: center; }
                .corpo-assin p { margin: 0; display: block; text-indent: 0; }
                .capa-header { text-align: center; }
                .capa-inst { font-size: 17pt; font-weight: bold; text-transform: uppercase; margin: 2pt 0; text-align: center; }
                .capa-om { font-size: 13pt; text-transform: uppercase; text-align: center; margin: 3pt 0 0; }
                .capa-assunto { font-size: 21pt; font-weight: bold; text-transform: uppercase; text-align: center; margin: 0; }
                .capa-legenda { width: 315pt; border: 1.5px solid #000; padding: 15pt 21pt; text-align: center; margin: 0 auto; }
                .capa-legenda p { font-size: 12pt; font-weight: bold; margin: 0; text-transform: uppercase; line-height: 1.6; }
                .sumario-label { font-weight: bold; text-align: center; margin: 0; }
                .sumario-titulo { font-weight: bold; text-align: center; margin: 0 0 10pt; }
                .toc-table { width: 100%; border-collapse: collapse; font-size: 10pt; }
                .toc-table tr.toc-capitulo td { font-weight: bold; text-transform: uppercase; padding-top: 5pt; }
                .toc-table tr.toc-secao td { padding-left: 12pt; }
                .toc-table tr.toc-subsecao td { padding-left: 20pt; }
                .toc-table tr.toc-artigo td { padding-left: 20pt; }
                td.toc-lbl { white-space: nowrap; max-width: 60%; }
                td.toc-mid { width: 100%; border-bottom: 1px dotted #999; }
                td.toc-pg { white-space: nowrap; text-align: right; padding-left: 6pt; min-width: 40pt; }
                .sumario-sep { height: 1em; }
                .capitulo-heading { text-align: center; margin: 15pt 0 3pt; }
                .cap-numero { font-weight: bold; text-transform: uppercase; margin: 0; }
                .cap-titulo  { font-weight: bold; text-transform: uppercase; margin: 0 0 6pt; }
                .secao-heading { text-align: center; margin: 10pt 0 3pt; }
                .sec-numero { margin: 0; }
                .sec-titulo  { margin: 0 0 5pt; }
                figure { display: block; text-align: center; margin: 10pt auto; max-width: 100%; page-break-inside: avoid; }
                figure img { max-width: 100%; height: auto; max-height: 500pt; display: block; margin: 0 auto; }
                .figura-titulo { font-size: 10pt; font-style: italic; margin: 0; text-align: center; }
                .figura-fonte  { font-size: 9pt; color: #555; margin: 3pt 0 0; text-align: center; }
                .emenda-strikethrough { text-decoration: line-through; }
                .emenda-incluido { color: #004400; }
                .emenda-ref-block { font-size: 10pt; font-style: italic; color: #555555; display: block; padding-left: 2.5cm; margin-bottom: 3pt; }
                """;
        }

        // ─── Page 1: Portaria de Aprovação ───────────────────────────────────────

        private String buildPortaria() {
            var sb = new StringBuilder();
            sb.append("<div class=\"page-break\">\n");

            // Cabeçalho
            sb.append("<div class=\"cabecalho\">");
            if (!brasaoRepublica.isBlank()) {
                sb.append("<img src=\"").append(brasaoRepublica).append("\"")
                  .append(" class=\"cabecalho-brasao\" alt=\"\" />");
            }
            sb.append("<p class=\"bold\">MINISTÉRIO DA DEFESA</p>");
            sb.append("<p class=\"bold\">COMANDO DA AERONÁUTICA</p>");
            sb.append("</div>\n");

            // Epígrafe
            var epigrafe = findPreli(ItemAnexoParteNormativaTipoEnum.EPIGRAFE);
            String epiHtml = conteudoOuNull(epigrafe);
            if (epiHtml != null) {
                sb.append("<div class=\"epigrafe\">").append(epiHtml).append("</div>\n");
            } else {
                sb.append("<p class=\"epigrafe\">PORTARIA Nº ___, DE ").append(formatarDataBR(doc.getDtCriacao())).append("</p>\n");
            }

            // Ementa
            var ementa = findPreli(ItemAnexoParteNormativaTipoEnum.EMENTA);
            String ementaHtml = conteudoOuNull(ementa);
            sb.append("<div class=\"ementa-bloco\"><div class=\"ementa-txt\">");
            if (ementaHtml != null) {
                sb.append(ementaHtml);
            } else {
                sb.append("Aprova a ").append(especieCompleta())
                  .append(" que dispõe sobre ").append(esc(doc.getAssuntoBasico().getNome())).append(".");
            }
            sb.append("</div></div>\n");

            // Preâmbulo
            var preambulo = findPreli(ItemAnexoParteNormativaTipoEnum.PREAMBULO);
            String preambuloHtml = conteudoOuNull(preambulo);
            sb.append("<div class=\"body-el\">");
            if (preambuloHtml != null) {
                sb.append(preambuloHtml);
            } else {
                sb.append("<strong>O COMANDANTE DA AERONÁUTICA</strong>, no uso das atribuições que lhe confere o art. 12 da Lei Complementar n° 97, de 9 de junho de 1999, tendo em vista o que consta do Processo n° ___/___-___/___,");
            }
            sb.append("</div>\n");

            // Fecho (alinhado à esquerda)
            var fecho = findPreli(ItemAnexoParteNormativaTipoEnum.FECHO);
            String fechoHtml = conteudoOuNull(fecho);
            sb.append("<div class=\"fecho-bloco\">");
            if (fechoHtml != null) {
                sb.append(fechoHtml);
            } else {
                sb.append("Brasília, ").append(formatarDataBR(doc.getDtCriacao())).append(".");
            }
            sb.append("</div>\n");

            // Assinatura (centralizada)
            var assinatura = findPreli(ItemAnexoParteNormativaTipoEnum.ASSINATURA);
            String assinaturaHtml = conteudoOuNull(assinatura);
            sb.append("<div class=\"assinatura-bloco\">");
            if (assinaturaHtml != null) {
                sb.append(assinaturaHtml);
            } else {
                sb.append("<p class=\"assin-cargo\">Comandante da Aeronáutica</p>");
            }
            sb.append("</div>\n");

            sb.append("</div>\n");
            return sb.toString();
        }

        // ─── Page 2: Capa ─────────────────────────────────────────────────────────

        private String buildCapa() {
            var sb = new StringBuilder();
            sb.append("<div class=\"page-break\">\n");

            sb.append("<div class=\"capa-header\">");
            sb.append("<p class=\"capa-inst\">MINISTÉRIO DA DEFESA</p>");
            sb.append("<p class=\"capa-inst\">COMANDO DA AERONÁUTICA</p>");
            sb.append("</div>\n");

            sb.append("<div style=\"height:50mm\"></div>\n");

            if (!brasaoFab.isBlank()) {
                sb.append("<div style=\"text-align:center\">");
                sb.append("<img src=\"").append(brasaoFab).append("\"")
                  .append(" style=\"width:180pt;height:180pt\" alt=\"\" />");
                sb.append("</div>\n");
            }

            sb.append("<div style=\"height:20mm\"></div>\n");

            sb.append("<p class=\"capa-assunto\">")
              .append(esc(doc.getAssuntoBasico().getNome()).toUpperCase())
              .append("</p>\n");

            sb.append("<div style=\"height:15mm\"></div>\n");

            String titulo = doc.getTituloDocumento() != null ? doc.getTituloDocumento() : especieCompleta();
            sb.append("<div class=\"capa-legenda\">");
            sb.append("<p>").append(esc(docId())).append("</p>");
            sb.append("<p>").append(esc(titulo.toUpperCase())).append("</p>");
            sb.append("<p>").append(LocalDate.now().getYear()).append("</p>");
            sb.append("</div>\n");

            sb.append("</div>\n");
            return sb.toString();
        }

        // ─── Page 3+: Sumário + Corpo ─────────────────────────────────────────────

        private String buildSumarioECorpo() {
            var sb = new StringBuilder();
            sb.append("<div>\n");

            String titulo = doc.getTituloDocumento() != null ? doc.getTituloDocumento() : especieCompleta();
            sb.append("<p class=\"sumario-label\">ANEXO I</p>\n");
            sb.append("<p class=\"sumario-label\" style=\"margin-bottom:10pt\">")
              .append(esc(titulo.toUpperCase())).append(" (").append(esc(docId())).append(")</p>\n");
            sb.append("<p class=\"sumario-titulo\">SUMÁRIO</p>\n");

            sb.append(buildToc());
            sb.append("<div class=\"sumario-sep\"></div>\n");
            sb.append(buildCorpoNormativo());

            sb.append("</div>\n");
            return sb.toString();
        }

        // ─── TOC ─────────────────────────────────────────────────────────────────

        private record TocEntry(String label, String cssClass, String pg) {}

        private String buildToc() {
            List<TocEntry> entries = new ArrayList<>();
            boolean temAgrupamento = normativos.stream().anyMatch(el ->
                    el.getElementType() == ItemAnexoParteNormativaTipoEnum.CAPITULO
                    || el.getElementType() == ItemAnexoParteNormativaTipoEnum.SECAO_NORMATIVA
                    || el.getElementType() == ItemAnexoParteNormativaTipoEnum.SUBSECAO_NORMATIVA);

            Map<Long, Integer> artNumMap = new java.util.HashMap<>();
            assignArtNums(normativos, new int[]{0}, artNumMap);

            int[] capNum = {0}, secNum = {0}, subSecNum = {0};
            if (temAgrupamento) {
                walkToc(normativos, entries, artNumMap, capNum, secNum, subSecNum);
            } else {
                collectArticleToc(normativos, entries, new int[]{0});
            }

            if (entries.isEmpty()) return "";

            var sb = new StringBuilder();
            sb.append("<table class=\"toc-table\">\n");
            for (var e : entries) {
                sb.append("<tr class=\"").append(e.cssClass()).append("\">");
                sb.append("<td class=\"toc-lbl\">").append(esc(e.label())).append("</td>");
                sb.append("<td class=\"toc-mid\"></td>");
                sb.append("<td class=\"toc-pg\">").append(esc(e.pg())).append("</td>");
                sb.append("</tr>\n");
            }
            sb.append("</table>\n");
            return sb.toString();
        }

        private void assignArtNums(List<ItemAnexoParteNormativaResponseDto> items,
                                   int[] counter, Map<Long, Integer> out) {
            for (var item : items) {
                if (item.getElementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) {
                    counter[0]++;
                    out.put(item.getId(), counter[0]);
                }
                if (item.getChildren() != null) assignArtNums(item.getChildren(), counter, out);
            }
        }

        private void walkToc(List<ItemAnexoParteNormativaResponseDto> items, List<TocEntry> entries,
                             Map<Long, Integer> artNums, int[] cap, int[] sec, int[] sub) {
            for (var item : items) {
                switch (item.getElementType()) {
                    case CAPITULO -> {
                        cap[0]++; sec[0] = 0; sub[0] = 0;
                        String t = item.getElementTitle() != null ? " - " + item.getElementTitle().toUpperCase() : "";
                        entries.add(new TocEntry("CAPÍTULO " + toRoman(cap[0]) + t,
                                "toc-capitulo", artRange(item.getChildren(), artNums)));
                        if (item.getChildren() != null) walkToc(item.getChildren(), entries, artNums, cap, sec, sub);
                    }
                    case SECAO_NORMATIVA -> {
                        sec[0]++; sub[0] = 0;
                        String t = item.getElementTitle() != null ? " - " + item.getElementTitle() : "";
                        entries.add(new TocEntry("Seção " + toRoman(sec[0]) + t,
                                "toc-secao", artRange(item.getChildren(), artNums)));
                        if (item.getChildren() != null) walkToc(item.getChildren(), entries, artNums, cap, sec, sub);
                    }
                    case SUBSECAO_NORMATIVA -> {
                        sub[0]++;
                        String t = item.getElementTitle() != null ? " - " + item.getElementTitle() : "";
                        entries.add(new TocEntry("Subseção " + toRoman(sub[0]) + t,
                                "toc-subsecao", artRange(item.getChildren(), artNums)));
                        if (item.getChildren() != null) walkToc(item.getChildren(), entries, artNums, cap, sec, sub);
                    }
                    default -> {}
                }
            }
        }

        private void collectArticleToc(List<ItemAnexoParteNormativaResponseDto> items,
                                       List<TocEntry> entries, int[] idx) {
            for (var item : items) {
                if (item.getElementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) {
                    idx[0]++;
                    entries.add(new TocEntry("Art. " + ordinalOrCardinal(idx[0]), "toc-artigo", fmtNum(idx[0])));
                }
            }
        }

        private String artRange(List<ItemAnexoParteNormativaResponseDto> children, Map<Long, Integer> artNums) {
            if (children == null || children.isEmpty()) return "";
            int first = -1, last = -1;
            for (var item : collectAllArt(children)) {
                int n = artNums.getOrDefault(item.getId(), -1);
                if (n < 0) continue;
                if (first < 0) first = n;
                last = n;
            }
            if (first < 0) return "";
            return first == last ? fmtNum(first) : fmtNum(first) + "/" + fmtNum(last);
        }

        private List<ItemAnexoParteNormativaResponseDto> collectAllArt(List<ItemAnexoParteNormativaResponseDto> items) {
            var result = new ArrayList<ItemAnexoParteNormativaResponseDto>();
            if (items == null) return result;
            for (var item : items) {
                if (item.getElementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) result.add(item);
                result.addAll(collectAllArt(item.getChildren()));
            }
            return result;
        }

        private String fmtNum(int n) {
            return n <= 9 ? n + "º" : String.valueOf(n);
        }

        // ─── Corpo normativo ─────────────────────────────────────────────────────

        private String buildCorpoNormativo() {
            artCount = 0;
            var sb = new StringBuilder();
            renderNormItems(normativos, sb, new int[]{0}, new int[]{0}, new int[]{0});
            return sb.toString();
        }

        private void renderNormItems(List<ItemAnexoParteNormativaResponseDto> items,
                                     StringBuilder sb, int[] capNum, int[] secNum, int[] subSecNum) {
            if (items == null) return;
            for (var item : items) renderNormItem(item, sb, capNum, secNum, subSecNum);
        }

        private void renderNormItem(ItemAnexoParteNormativaResponseDto item,
                                    StringBuilder sb, int[] capNum, int[] secNum, int[] subSecNum) {
            switch (item.getElementType()) {
                case CAPITULO -> {
                    capNum[0]++; secNum[0] = 0; subSecNum[0] = 0;
                    sb.append("<div class=\"capitulo-heading\">");
                    sb.append("<p class=\"cap-numero\">CAPÍTULO ").append(toRoman(capNum[0])).append("</p>");
                    if (item.getElementTitle() != null && !item.getElementTitle().isBlank())
                        sb.append("<p class=\"cap-titulo\">").append(esc(item.getElementTitle().toUpperCase())).append("</p>");
                    sb.append("</div>\n");
                    renderNormItems(item.getChildren(), sb, capNum, secNum, subSecNum);
                }
                case SECAO_NORMATIVA -> {
                    secNum[0]++; subSecNum[0] = 0;
                    sb.append("<div class=\"secao-heading\">");
                    sb.append("<p class=\"sec-numero\"><strong>Seção ").append(toRoman(secNum[0])).append("</strong></p>");
                    if (item.getElementTitle() != null && !item.getElementTitle().isBlank())
                        sb.append("<p class=\"sec-titulo\"><strong>").append(esc(item.getElementTitle())).append("</strong></p>");
                    sb.append("</div>\n");
                    renderNormItems(item.getChildren(), sb, capNum, secNum, subSecNum);
                }
                case SUBSECAO_NORMATIVA -> {
                    subSecNum[0]++;
                    sb.append("<div class=\"secao-heading\">");
                    sb.append("<p class=\"sec-numero\"><strong>Subseção ").append(toRoman(subSecNum[0])).append("</strong></p>");
                    if (item.getElementTitle() != null && !item.getElementTitle().isBlank())
                        sb.append("<p class=\"sec-titulo\"><strong>").append(esc(item.getElementTitle())).append("</strong></p>");
                    sb.append("</div>\n");
                    renderNormItems(item.getChildren(), sb, capNum, secNum, subSecNum);
                }
                case ARTIGO -> {
                    artCount++;
                    renderBodyEl(sb, "Art. " + ordinalOrCardinal(artCount) + S2, true,
                            item.getElementContent(), item.getEmendaStatus(), item.getConteudoEmenda());
                    renderArtigoChildren(item.getChildren(), sb);
                }
                default -> renderBodyEl(sb, "", false, item.getElementContent(),
                        item.getEmendaStatus(), item.getConteudoEmenda());
            }
        }

        private void renderArtigoChildren(List<ItemAnexoParteNormativaResponseDto> children, StringBuilder sb) {
            if (children == null) return;
            long parCount = children.stream()
                    .filter(c -> c.getElementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO
                              || c.getElementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO)
                    .count();
            int parNum = 0, incisoNum = 0;
            for (var child : children) {
                switch (child.getElementType()) {
                    case PARAGRAFO, PARAGRAFO_UNICO -> {
                        parNum++;
                        boolean unico = parCount == 1 && child.getElementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO;
                        String parLabel = unico ? "Parágrafo único." + S2 : "§ " + ordinalOrCardinal(parNum) + S2;
                        renderBodyEl(sb, parLabel, false, child.getElementContent(),
                                child.getEmendaStatus(), child.getConteudoEmenda());
                        renderIncisoChildren(child.getChildren(), sb);
                    }
                    case INCISO -> {
                        incisoNum++;
                        String incLabel = toRoman(incisoNum) + S1 + "-" + S1;
                        renderBodyEl(sb, incLabel, false, child.getElementContent(),
                                child.getEmendaStatus(), child.getConteudoEmenda());
                        renderAlineaChildren(child.getChildren(), sb);
                    }
                    default -> renderBodyEl(sb, "", false, child.getElementContent(),
                            child.getEmendaStatus(), child.getConteudoEmenda());
                }
            }
        }

        private void renderIncisoChildren(List<ItemAnexoParteNormativaResponseDto> children, StringBuilder sb) {
            if (children == null) return;
            int n = 0;
            for (var child : children) {
                if (child.getElementType() == ItemAnexoParteNormativaTipoEnum.INCISO) {
                    n++;
                    renderBodyEl(sb, toRoman(n) + S1 + "-" + S1, false, child.getElementContent(),
                            child.getEmendaStatus(), child.getConteudoEmenda());
                    renderAlineaChildren(child.getChildren(), sb);
                }
            }
        }

        private void renderAlineaChildren(List<ItemAnexoParteNormativaResponseDto> children, StringBuilder sb) {
            if (children == null) return;
            int n = 0;
            for (var child : children) {
                if (child.getElementType() == ItemAnexoParteNormativaTipoEnum.ALINEA) {
                    n++;
                    renderBodyEl(sb, toLetter(n) + ")" + S1, false, child.getElementContent(),
                            child.getEmendaStatus(), child.getConteudoEmenda());
                    renderSubAlineaChildren(child.getChildren(), sb);
                }
            }
        }

        private void renderSubAlineaChildren(List<ItemAnexoParteNormativaResponseDto> children, StringBuilder sb) {
            if (children == null) return;
            int n = 0;
            for (var child : children) {
                if (child.getElementType() == ItemAnexoParteNormativaTipoEnum.SUB_ALINEA) {
                    n++;
                    renderBodyEl(sb, n + "." + S1, false, child.getElementContent(),
                            child.getEmendaStatus(), child.getConteudoEmenda());
                }
            }
        }

        // conteudo     = original published content (shown struck through for ALTERADO/REVOGADO)
        // conteudoEmenda = new amendment content (shown as current text for ALTERADO)
        private void renderBodyEl(StringBuilder sb, String label, boolean labelBold, String conteudo,
                                   ElementoEmendaStatusEnum emendaStatus, String conteudoEmenda) {
            if (emendaStatus == null || emendaStatus == ElementoEmendaStatusEnum.INALTERADO) {
                renderBodyEl(sb, label, labelBold, conteudo);
                return;
            }
            switch (emendaStatus) {
                case REVOGADO -> {
                    renderBodyElStyled(sb, label, labelBold, conteudo, "emenda-strikethrough");
                    sb.append(buildEmendaRef(emendaStatus));
                }
                case ALTERADO -> {
                    renderBodyElStyled(sb, label, labelBold, conteudo, "emenda-strikethrough");
                    renderBodyEl(sb, label, labelBold, conteudoEmenda);
                    sb.append(buildEmendaRef(emendaStatus));
                }
                case INCLUIDO -> {
                    renderBodyElStyled(sb, label, labelBold, conteudo, "emenda-incluido");
                    sb.append(buildEmendaRef(emendaStatus));
                }
            }
        }

        private void renderBodyElStyled(StringBuilder sb, String label, boolean labelBold,
                                         String conteudo, String extraClass) {
            String safe = processContent(conteudo);
            boolean hasBlock = hasBlockContent(safe);
            String labelHtml = labelBold
                    ? "<span class=\"norm-lbl norm-lbl-bold\">" + label + "</span>"
                    : "<span class=\"norm-lbl\">" + label + "</span>";
            if (hasBlock) {
                sb.append("<div class=\"body-el norm-el ").append(extraClass).append("\">")
                  .append(labelHtml)
                  .append("<div class=\"norm-content-block\">").append(safe).append("</div>")
                  .append("</div>\n");
            } else {
                sb.append("<p class=\"body-el norm-el ").append(extraClass).append("\">")
                  .append(labelHtml).append(safe).append("</p>\n");
            }
        }

        private String buildEmendaRef(ElementoEmendaStatusEnum status) {
            String acao = switch (status) {
                case ALTERADO -> "alterado";
                case REVOGADO -> "revogado";
                case INCLUIDO -> "incluído";
                default       -> "modificado";
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
            return "<span class=\"emenda-ref-block\">" + esc(ref) + "</span>\n";
        }

        private void renderBodyEl(StringBuilder sb, String label, boolean labelBold, String conteudo) {
            String safe = processContent(conteudo);
            boolean hasBlock = hasBlockContent(safe);
            String labelHtml = labelBold
                    ? "<span class=\"norm-lbl norm-lbl-bold\">" + label + "</span>"
                    : "<span class=\"norm-lbl\">" + label + "</span>";
            if (hasBlock) {
                sb.append("<div class=\"body-el norm-el\">")
                  .append(labelHtml)
                  .append("<div class=\"norm-content-block\">")
                  .append(safe)
                  .append("</div></div>\n");
            } else {
                sb.append("<p class=\"body-el norm-el\">")
                  .append(labelHtml)
                  .append(safe)
                  .append("</p>\n");
            }
        }

        // ─── Helpers ─────────────────────────────────────────────────────────────

        private String processContent(String conteudo) {
            if (conteudo == null || conteudo.isBlank()) return "";
            try {
                TipTapNode doc = objectMapper.readValue(conteudo, TipTapNode.class);
                return xhtml(embedImgDataUris(TipTapHtmlSerializer.toHtml(doc)));
            } catch (Exception ignored) {
                return "";
            }
        }

        // Substitui src="http://..." por data URIs — garante imagens no PDF independente de rede/Docker
        private static String embedImgDataUris(String html) {
            return IMG_SRC_HTTP.matcher(html).replaceAll(mr -> {
                String q   = mr.group(1);
                String url = mr.group(2);
                return "src=" + q + fetchDataUri(url) + q;
            });
        }

        private static String fetchDataUri(String url) {
            try {
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();
                var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) return url;
                String ct = response.headers().firstValue("content-type")
                        .orElse(guessMimeType(url));
                String mime = ct.split(";")[0].trim();
                if (!mime.startsWith("image/")) return url;
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(response.body());
            } catch (Exception e) {
                return url;
            }
        }

        private static String guessMimeType(String url) {
            String lc = url.toLowerCase();
            if (lc.endsWith(".png"))              return "image/png";
            if (lc.endsWith(".jpg") || lc.endsWith(".jpeg")) return "image/jpeg";
            if (lc.endsWith(".gif"))              return "image/gif";
            if (lc.endsWith(".webp"))             return "image/webp";
            if (lc.endsWith(".svg"))              return "image/svg+xml";
            return "image/png";
        }

        private ItemPartePreliminarResponseDto findPreli(ItemAnexoParteNormativaTipoEnum tipo) {
            return preliminares.stream()
                    .filter(p -> p.getElementType() == tipo)
                    .findFirst().orElse(null);
        }

        private String conteudoOuNull(ItemPartePreliminarResponseDto item) {
            if (item == null) return null;
            String c = item.getElementContent();
            if (c == null || c.isBlank()) return null;
            String html = processContent(c);
            // Check if there's actual text after stripping HTML
            String text = html.replaceAll("<[^>]*>", "").trim();
            return text.isBlank() ? null : html;
        }

        private String docId() {
            return doc.getEspecieNormativa().getSigla()
                    + " " + doc.getAssuntoBasico().getCodigo()
                    + "-" + doc.getNumeroSecundario();
        }

        private String especieCompleta() {
            String sigla = doc.getEspecieNormativa().getSigla();
            return ESPECIE_COMPLETA.getOrDefault(sigla, sigla.toUpperCase());
        }

        private String formatarDataBR(Timestamp ts) {
            if (ts == null) return "___________";
            LocalDate d = ts.toLocalDateTime().toLocalDate();
            String[] meses = {"janeiro","fevereiro","março","abril","maio","junho",
                    "julho","agosto","setembro","outubro","novembro","dezembro"};
            return d.getDayOfMonth() + " de " + meses[d.getMonthValue() - 1] + " de " + d.getYear();
        }

        private boolean hasBlockContent(String html) {
            return html != null && BLOCK_PATTERN.matcher(html).find();
        }

        private static String xhtml(String html) {
            if (html == null || html.isBlank()) return html == null ? "" : html;
            return VOID_SELF_CLOSE.matcher(html).replaceAll("<$1$2 />");
        }

        // ─── Numbering ────────────────────────────────────────────────────────────

        private static String toRoman(int n) {
            if (n <= 0) return "";
            int[]    vals = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
            String[] syms = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
            var sb = new StringBuilder();
            for (int i = 0; i < vals.length; i++)
                while (n >= vals[i]) { sb.append(syms[i]); n -= vals[i]; }
            return sb.toString();
        }

        private static String toLetter(int n) {
            return String.valueOf((char) ('a' + n - 1));
        }

        private static String ordinalOrCardinal(int n) {
            return n <= 9 ? n + "º" : n + ".";
        }

        private static String esc(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }
    }
}
