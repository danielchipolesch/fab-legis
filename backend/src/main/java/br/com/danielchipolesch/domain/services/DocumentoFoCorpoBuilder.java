package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.util.tiptap.TipTapNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static br.com.danielchipolesch.domain.services.DocumentoFoContext.block;
import static br.com.danielchipolesch.domain.services.DocumentoFoContext.foEsc;
import static br.com.danielchipolesch.domain.services.DocumentoFoContext.labelFo;

// Sumário + corpo normativo (capítulo/seção/subseção/artigo/parágrafo/inciso/alínea),
// ciente de emenda — o texto anterior tachado, a cláusula (atual e/ou anterior) e a
// numeração com sufixo de letra para inclusões, tudo conforme a LC 95/1998. Instância
// nova por chamada de DocumentoFoBuilder.buildFo().
final class DocumentoFoCorpoBuilder {

    private final DocumentoFoContext ctx;
    private final List<ItemAnexoParteNormativaResponseDto> normativos;
    private final NumeracaoService numeracaoService;

    DocumentoFoCorpoBuilder(DocumentoFoContext ctx, List<ItemAnexoParteNormativaResponseDto> normativos,
                             NumeracaoService numeracaoService) {
        this.ctx = ctx;
        this.normativos = normativos;
        this.numeracaoService = numeracaoService;
    }

    // ─── Pages 3+: Sumário + Corpo ────────────────────────────────────────────

    String buildBodySequence() {
        var sb = new StringBuilder();
        sb.append("<fo:page-sequence master-reference=\"a4\">\n");

        // Footer with page number
        sb.append("<fo:static-content flow-name=\"xsl-region-after\">\n");
        sb.append("  <fo:block text-align=\"right\" font-size=\"10pt\"><fo:page-number/></fo:block>\n");
        sb.append("</fo:static-content>\n");

        sb.append(ctx.buildStaticContentWatermark());
        sb.append("<fo:flow flow-name=\"xsl-region-body\">\n");

        String titulo = ctx.doc.getTituloDocumento() != null
                ? ctx.doc.getTituloDocumento().toUpperCase() : ctx.especieCompleta().toUpperCase();
        sb.append(block("ANEXO I", "center", "12pt", "bold", "0", "0"));
        sb.append(block(foEsc(titulo) + " (" + foEsc(ctx.docId()) + ")", "center", "12pt", "bold", "0", "10pt"));
        sb.append(block("SUMÁRIO", "center", "12pt", "bold", "0", "8pt"));

        Map<Long, NumeracaoService.ElementoNumeracao> numbering = numeracaoService.calcular(normativos);
        sb.append(buildToc(numbering));
        sb.append("<fo:block space-after=\"1.2em\"/>\n");
        sb.append(buildCorpoNormativo(numbering));

        sb.append("</fo:flow>\n</fo:page-sequence>\n");
        return sb.toString();
    }

    // ─── Rótulos de numeração ───────────────────────────────────────────────────
    // O cálculo em si vive em NumeracaoService (reutilizável, exposto via API —
    // GET /v1/documentos/{id}/numeracao) — aqui só resta acesso de conveniência.

    private String capLabel(ItemAnexoParteNormativaResponseDto item, Map<Long, NumeracaoService.ElementoNumeracao> num) {
        var en = num.get(item.id());
        return en != null ? en.label() : "";
    }

    private String secLabel(ItemAnexoParteNormativaResponseDto item, Map<Long, NumeracaoService.ElementoNumeracao> num) {
        var en = num.get(item.id());
        return en != null ? en.label() : "";
    }

    private String subLabel(ItemAnexoParteNormativaResponseDto item, Map<Long, NumeracaoService.ElementoNumeracao> num) {
        var en = num.get(item.id());
        return en != null ? en.label() : "";
    }

    private String artLabel(ItemAnexoParteNormativaResponseDto item, Map<Long, NumeracaoService.ElementoNumeracao> num) {
        var en = num.get(item.id());
        return en != null ? en.label() : "";
    }

    // ─── TOC ──────────────────────────────────────────────────────────────────

    private record TocEntry(String label, boolean bold, boolean indent1, boolean indent2, String anchor, String pg) {}

    private String buildToc(Map<Long, NumeracaoService.ElementoNumeracao> num) {
        List<TocEntry> entries = new ArrayList<>();
        boolean temAgrupamento = numeracaoService.temAgrupamento(normativos);

        if (temAgrupamento) {
            walkToc(normativos, entries, num);
        } else {
            collectArtToc(normativos, entries, num);
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

    // Título vigente para o sumário: se ALTERADO por emenda, usa o novo título
    // (o original tachado só faz sentido no corpo, não numa linha de sumário).
    private String effectiveTitle(ItemAnexoParteNormativaResponseDto item) {
        if (item.emendaStatus() == ElementoEmendaStatusEnum.ALTERADO
                && item.tituloEmenda() != null && !item.tituloEmenda().isBlank()) {
            return item.tituloEmenda();
        }
        return item.elementTitle();
    }

    private void walkToc(List<ItemAnexoParteNormativaResponseDto> items, List<TocEntry> entries, Map<Long, NumeracaoService.ElementoNumeracao> num) {
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            String titulo = effectiveTitle(item);
            switch (item.elementType()) {
                case CAPITULO -> {
                    String t = titulo != null ? " - " + titulo.toUpperCase() : "";
                    entries.add(new TocEntry("CAPÍTULO " + capLabel(item, num) + t, true, false, false,
                            "norm-" + item.id(), numeracaoService.intervaloArtigos(item, items, i, num)));
                    walkToc(item.children(), entries, num);
                }
                case SECAO_NORMATIVA -> {
                    String t = titulo != null ? " - " + titulo : "";
                    entries.add(new TocEntry("Seção " + secLabel(item, num) + t, false, true, false,
                            "norm-" + item.id(), numeracaoService.intervaloArtigos(item, items, i, num)));
                    walkToc(item.children(), entries, num);
                }
                case SUBSECAO_NORMATIVA -> {
                    String t = titulo != null ? " - " + titulo : "";
                    entries.add(new TocEntry("Subseção " + subLabel(item, num) + t, false, true, true,
                            "norm-" + item.id(), numeracaoService.intervaloArtigos(item, items, i, num)));
                    walkToc(item.children(), entries, num);
                }
                default -> {}
            }
        }
    }

    private void collectArtToc(List<ItemAnexoParteNormativaResponseDto> items,
                               List<TocEntry> entries, Map<Long, NumeracaoService.ElementoNumeracao> num) {
        for (var item : items) {
            if (item.elementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) {
                var en = num.get(item.id());
                int n = en != null ? en.numero() : 0;
                String pg = n > 0 ? NumeracaoService.fmtNum(n) : "";
                entries.add(new TocEntry("Art. " + (n > 0 ? artLabel(item, num) : "?"), false, true, false,
                        "norm-" + item.id(), pg));
            }
        }
    }

    // ─── Corpo Normativo ──────────────────────────────────────────────────────

    private String buildCorpoNormativo(Map<Long, NumeracaoService.ElementoNumeracao> num) {
        var sb = new StringBuilder();
        renderNormItems(normativos, sb, num);
        return sb.toString();
    }

    private void renderNormItems(List<ItemAnexoParteNormativaResponseDto> items, StringBuilder sb, Map<Long, NumeracaoService.ElementoNumeracao> num) {
        if (items == null) return;
        for (var item : items) renderNormItem(item, sb, num);
    }

    // Cabeçalho de capítulo/seção/subseção, ciente de emenda: título original tachado
    // para REVOGADO/ALTERADO, novo título para ALTERADO/INCLUIDO, nota de referência
    // para os três — espelha renderBodyEl's overload de emenda para conteúdo de artigo.
    private void renderGroupingHeading(StringBuilder sb, String anc, String headingText, String spaceBefore,
                                       String titulo, String tituloEmenda,
                                       ElementoEmendaStatusEnum emendaStatus, boolean uppercase,
                                       String clausulaEmenda, String clausulaEmendaAnterior) {
        sb.append("<fo:block id=\"").append(anc).append("\"")
          .append(" text-align=\"center\" font-weight=\"bold\" space-before=\"").append(spaceBefore)
          .append("\" space-after=\"3pt\">").append(headingText).append("</fo:block>\n");

        // Elementos com emenda (INCLUIDO/ALTERADO/REVOGADO) nunca voltam a INALTERADO
        // — ver consolidarPublicacao — então este ramo é só para elementos que de
        // fato nunca tiveram emenda; clausulaEmenda nunca está presente aqui.
        if (emendaStatus == null || emendaStatus == ElementoEmendaStatusEnum.INALTERADO) {
            if (titulo != null && !titulo.isBlank()) {
                sb.append("<fo:block text-align=\"center\" font-weight=\"bold\" space-after=\"6pt\">")
                  .append(foEsc(uppercase ? titulo.toUpperCase() : titulo)).append("</fo:block>\n");
            }
            return;
        }

        if (emendaStatus == ElementoEmendaStatusEnum.REVOGADO || emendaStatus == ElementoEmendaStatusEnum.ALTERADO) {
            if (titulo != null && !titulo.isBlank()) {
                var tmpTitulo = new StringBuilder();
                tmpTitulo.append("<fo:block text-align=\"center\" font-weight=\"bold\" space-after=\"3pt\" text-decoration=\"line-through\">")
                  .append(foEsc(uppercase ? titulo.toUpperCase() : titulo)).append("</fo:block>\n");
                // Cláusula da emenda anterior (ex.: "incluído pela Portaria X") riscada
                // junto do título que ela descreve, se este título já foi publicado antes.
                if (clausulaEmendaAnterior != null && !clausulaEmendaAnterior.isBlank()) {
                    insertBeforeLastBlockClose(tmpTitulo, wrapEmendaInlineStruck(clausulaEmendaAnterior));
                }
                sb.append(tmpTitulo);
            }
        }
        if (emendaStatus == ElementoEmendaStatusEnum.ALTERADO || emendaStatus == ElementoEmendaStatusEnum.INCLUIDO) {
            String texto = emendaStatus == ElementoEmendaStatusEnum.ALTERADO ? tituloEmenda : titulo;
            if (texto != null && !texto.isBlank()) {
                sb.append("<fo:block text-align=\"center\" font-weight=\"bold\" space-after=\"3pt\">")
                  .append(foEsc(uppercase ? texto.toUpperCase() : texto)).append("</fo:block>\n");
            }
        }
        // Elemento já consolidado numa publicação anterior (REVOGADO permanente, ou
        // INCLUIDO que permanece INCLUIDO para sempre — ver consolidarPublicacao):
        // mostra a cláusula congelada em vez do placeholder ao vivo.
        String refInline = clausulaEmenda != null
                ? wrapEmendaInline(clausulaEmenda) : emendaRefInline(emendaStatus);
        sb.append("<fo:block text-align=\"center\" space-after=\"5pt\">")
          .append(refInline).append("</fo:block>\n");
    }

    private void renderNormItem(ItemAnexoParteNormativaResponseDto item, StringBuilder sb, Map<Long, NumeracaoService.ElementoNumeracao> num) {
        String anc = "norm-" + item.id();
        switch (item.elementType()) {
            case CAPITULO -> {
                renderGroupingHeading(sb, anc, "CAPÍTULO " + capLabel(item, num), "15pt",
                        item.elementTitle(), item.tituloEmenda(), item.emendaStatus(), true,
                        item.clausulaEmenda(), item.clausulaEmendaAnterior());
                renderNormItems(item.children(), sb, num);
            }
            case SECAO_NORMATIVA -> {
                renderGroupingHeading(sb, anc, "Seção " + secLabel(item, num), "10pt",
                        item.elementTitle(), item.tituloEmenda(), item.emendaStatus(), false,
                        item.clausulaEmenda(), item.clausulaEmendaAnterior());
                renderNormItems(item.children(), sb, num);
            }
            case SUBSECAO_NORMATIVA -> {
                renderGroupingHeading(sb, anc, "Subseção " + subLabel(item, num), "8pt",
                        item.elementTitle(), item.tituloEmenda(), item.emendaStatus(), false,
                        item.clausulaEmenda(), item.clausulaEmendaAnterior());
                renderNormItems(item.children(), sb, num);
            }
            case ARTIGO -> {
                renderBodyEl(sb, anc, "Art. " + artLabel(item, num) + "  ", true,
                        item.elementContent(), item.emendaStatus(), item.conteudoEmenda(),
                        item.clausulaEmenda(), item.clausulaEmendaAnterior());
                renderArtigoFilhos(item.children(), sb);
            }
            default -> renderBodyEl(sb, null, "", false, item.elementContent(),
                    item.emendaStatus(), item.conteudoEmenda(), item.clausulaEmenda(),
                    item.clausulaEmendaAnterior());
        }
    }

    private void renderArtigoFilhos(List<ItemAnexoParteNormativaResponseDto> filhos, StringBuilder sb) {
        if (filhos == null) return;
        long parCount = filhos.stream()
                .filter(c -> c.elementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO
                          || c.elementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO)
                .count();
        int parNum = 0, incisoNum = 0;
        for (var child : filhos) {
            switch (child.elementType()) {
                case PARAGRAFO, PARAGRAFO_UNICO -> {
                    parNum++;
                    boolean unico = parCount == 1 && child.elementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO;
                    renderBodyEl(sb, null, unico ? "Parágrafo único.  " : "§ " + NumeracaoService.ordinalOrCardinal(parNum) + "  ",
                            false, child.elementContent(), child.emendaStatus(), child.conteudoEmenda(), child.clausulaEmenda(), child.clausulaEmendaAnterior());
                    renderIncisoFilhos(child.children(), sb);
                }
                case INCISO -> {
                    incisoNum++;
                    renderBodyEl(sb, null, NumeracaoService.toRoman(incisoNum) + " - ", false, child.elementContent(),
                            child.emendaStatus(), child.conteudoEmenda(), child.clausulaEmenda(), child.clausulaEmendaAnterior());
                    renderAlineaFilhos(child.children(), sb);
                }
                default -> renderBodyEl(sb, null, "", false, child.elementContent(),
                        child.emendaStatus(), child.conteudoEmenda(), child.clausulaEmenda(), child.clausulaEmendaAnterior());
            }
        }
    }

    private void renderIncisoFilhos(List<ItemAnexoParteNormativaResponseDto> filhos, StringBuilder sb) {
        if (filhos == null) return;
        int n = 0;
        for (var child : filhos) {
            if (child.elementType() == ItemAnexoParteNormativaTipoEnum.INCISO) {
                n++;
                renderBodyEl(sb, null, NumeracaoService.toRoman(n) + " - ", false, child.elementContent(),
                        child.emendaStatus(), child.conteudoEmenda(), child.clausulaEmenda(), child.clausulaEmendaAnterior());
                renderAlineaFilhos(child.children(), sb);
            }
        }
    }

    private void renderAlineaFilhos(List<ItemAnexoParteNormativaResponseDto> filhos, StringBuilder sb) {
        if (filhos == null) return;
        int n = 0;
        for (var child : filhos) {
            if (child.elementType() == ItemAnexoParteNormativaTipoEnum.ALINEA) {
                n++;
                renderBodyEl(sb, null, NumeracaoService.toLetter(n) + ") ", false, child.elementContent(),
                        child.emendaStatus(), child.conteudoEmenda(), child.clausulaEmenda(), child.clausulaEmendaAnterior());
                renderSubAlineaFilhos(child.children(), sb);
            }
        }
    }

    private void renderSubAlineaFilhos(List<ItemAnexoParteNormativaResponseDto> filhos, StringBuilder sb) {
        if (filhos == null) return;
        int n = 0;
        for (var child : filhos) {
            if (child.elementType() == ItemAnexoParteNormativaTipoEnum.SUB_ALINEA) {
                n++;
                renderBodyEl(sb, null, n + ". ", false, child.elementContent(),
                        child.emendaStatus(), child.conteudoEmenda(), child.clausulaEmenda(), child.clausulaEmendaAnterior());
            }
        }
    }

    private void renderBodyEl(StringBuilder sb, String id, String label, boolean labelBold, String conteudo) {
        String idAttr = (id != null && !id.isBlank()) ? " id=\"" + id + "\"" : "";
        if (conteudo == null || conteudo.isBlank()) {
            sb.append("<fo:block").append(idAttr).append(" text-indent=\"2.5cm\" space-after=\"8pt\">")
              .append(labelFo(label, labelBold)).append("</fo:block>\n");
            return;
        }
        TipTapNode doc = ctx.parseConteudo(conteudo);
        if (doc == null) {
            sb.append("<fo:block").append(idAttr).append(" text-indent=\"2.5cm\" space-after=\"8pt\">")
              .append(labelFo(label, labelBold)).append("</fo:block>\n");
            return;
        }
        boolean blockContent = ctx.renderer.hasBlockContent(doc);
        if (blockContent) {
            if (!label.isBlank() && ctx.renderer.startsWithParagraph(doc)) {
                sb.append("<fo:block").append(idAttr)
                  .append(" text-indent=\"2.5cm\" space-after=\"3pt\" text-align=\"justify\">")
                  .append(labelFo(label, labelBold))
                  .append(ctx.renderer.renderInlineContent(doc))
                  .append("</fo:block>\n");
                sb.append(ctx.renderer.renderSkippingFirstParagraph(doc));
            } else {
                if (!label.isBlank()) {
                    sb.append("<fo:block").append(idAttr)
                      .append(" text-indent=\"2.5cm\" space-after=\"3pt\" text-align=\"justify\">")
                      .append(labelFo(label, labelBold)).append("</fo:block>\n");
                }
                sb.append(ctx.renderer.renderDocContent(doc));
            }
        } else {
            sb.append("<fo:block").append(idAttr).append(" text-indent=\"2.5cm\" space-after=\"8pt\" text-align=\"justify\">")
              .append(labelFo(label, labelBold))
              .append(ctx.renderer.renderInlineContent(doc))
              .append("</fo:block>\n");
        }
    }

    // Overload aware of emenda status:
    //   conteudo       = redação imediatamente anterior a ESTA emenda (mostrada
    //                    tachada para ALTERADO/REVOGADO — LC 95/1998)
    //   conteudoEmenda = redação vigente após a emenda (texto atual para ALTERADO)
    //   clausulaEmenda = cláusula congelada de uma emenda já publicada. Elementos
    //                    com emenda nunca voltam a INALTERADO (ver
    //                    consolidarPublicacao), então este parâmetro só é usado
    //                    dentro dos ramos REVOGADO/ALTERADO/INCLUIDO abaixo.
    //   clausulaEmendaAnterior = cláusula da redação que `conteudo` representa (ex.:
    //                    "incluído pela Portaria X"), quando esse texto já foi
    //                    publicado com sua própria cláusula antes desta emenda — ver
    //                    EmendaService. Mostrada riscada junto de `conteudo`.
    private void renderBodyEl(StringBuilder sb, String id, String label, boolean labelBold,
                              String conteudo,
                              ElementoEmendaStatusEnum emendaStatus,
                              String conteudoEmenda,
                              String clausulaEmenda,
                              String clausulaEmendaAnterior) {
        if (emendaStatus == null || emendaStatus == ElementoEmendaStatusEnum.INALTERADO) {
            renderBodyEl(sb, id, label, labelBold, conteudo);
            return;
        }
        String idAttr = (id != null && !id.isBlank()) ? " id=\"" + id + "\"" : "";
        // Elemento já consolidado numa publicação anterior (REVOGADO permanente, ou
        // INCLUIDO que permanece INCLUIDO para sempre — ver consolidarPublicacao):
        // mostra a cláusula congelada em vez do placeholder ao vivo.
        String refInline = clausulaEmenda != null
                ? wrapEmendaInline(clausulaEmenda) : emendaRefInline(emendaStatus);
        String refAnteriorInline = (clausulaEmendaAnterior != null && !clausulaEmendaAnterior.isBlank())
                ? wrapEmendaInlineStruck(clausulaEmendaAnterior) : null;
        switch (emendaStatus) {
            case REVOGADO -> {
                var tmp = new StringBuilder();
                renderStrikethroughBlock(tmp, idAttr, label, labelBold, conteudo);
                if (refAnteriorInline != null) insertBeforeLastBlockClose(tmp, refAnteriorInline);
                insertBeforeLastBlockClose(tmp, refInline);
                sb.append(tmp);
            }
            case ALTERADO -> {
                var tmpOld = new StringBuilder();
                renderStrikethroughBlock(tmpOld, idAttr, label, labelBold, conteudo);
                if (refAnteriorInline != null) insertBeforeLastBlockClose(tmpOld, refAnteriorInline);
                sb.append(tmpOld);
                var tmp = new StringBuilder();
                renderBodyEl(tmp, null, label, labelBold, conteudoEmenda);
                insertBeforeLastBlockClose(tmp, refInline);
                sb.append(tmp);
            }
            case INCLUIDO -> {
                var tmp = new StringBuilder();
                renderIncludidoBlock(tmp, idAttr, label, labelBold, conteudo);
                insertBeforeLastBlockClose(tmp, refInline);
                sb.append(tmp);
            }
        }
    }

    private void renderStrikethroughBlock(StringBuilder sb, String idAttr,
                                           String label, boolean labelBold, String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            sb.append("<fo:block").append(idAttr)
              .append(" text-indent=\"2.5cm\" space-after=\"3pt\" text-align=\"justify\"")
              .append(" text-decoration=\"line-through\">")
              .append(labelFo(label, labelBold))
              .append("</fo:block>\n");
            return;
        }
        TipTapNode doc = ctx.parseConteudo(conteudo);
        if (doc == null) {
            sb.append("<fo:block").append(idAttr)
              .append(" text-indent=\"2.5cm\" space-after=\"3pt\" text-align=\"justify\"")
              .append(" text-decoration=\"line-through\">")
              .append(labelFo(label, labelBold))
              .append(foEsc(conteudo))
              .append("</fo:block>\n");
            return;
        }
        List<TipTapNode> paras = doc.getContent() != null
                ? doc.getContent().stream()
                     .filter(n -> "paragraph".equals(n.getType()))
                     .collect(java.util.stream.Collectors.toList())
                : List.of();
        if (paras.isEmpty()) {
            sb.append("<fo:block").append(idAttr)
              .append(" text-indent=\"2.5cm\" space-after=\"3pt\" text-align=\"justify\"")
              .append(" text-decoration=\"line-through\">")
              .append(labelFo(label, labelBold))
              .append("</fo:block>\n");
            return;
        }
        boolean first = true;
        for (var para : paras) {
            sb.append("<fo:block").append(first ? idAttr : "")
              .append(" text-indent=\"2.5cm\" space-after=\"3pt\" text-align=\"justify\"")
              .append(" text-decoration=\"line-through\">");
            if (first) sb.append(labelFo(label, labelBold));
            sb.append(ctx.renderer.renderParagraphInlines(para));
            sb.append("</fo:block>\n");
            first = false;
        }
    }

    private void renderIncludidoBlock(StringBuilder sb, String idAttr,
                                      String label, boolean labelBold, String conteudo) {
        TipTapNode node = ctx.parseConteudo(conteudo);
        String inline = node != null ? ctx.renderer.renderInlineContent(node) : foEsc(conteudo);
        sb.append("<fo:block").append(idAttr)
          .append(" text-indent=\"2.5cm\" space-after=\"8pt\" text-align=\"justify\">")
          .append(labelFo(label, labelBold))
          .append(inline)
          .append("</fo:block>\n");
    }

    // Cláusula ao vivo para emenda ainda pendente (não publicada nesta alteração):
    // sempre o placeholder XYZ/ABC, nunca doc.getPortariaReferencia()/getBcaReferencia()
    // — esses campos guardam a portaria/BCA da ÚLTIMA publicação (que pode já existir
    // mesmo numa primeira alteração após a publicação inicial) e não têm relação com
    // a emenda em curso, cuja portaria/BCA só existem quando ELA for republicada. Uma
    // vez publicada, a cláusula é congelada (ver clausulaEmenda) e este cálculo deixa
    // de valer para aquele elemento.
    private String emendaRefInline(ElementoEmendaStatusEnum status) {
        String acao = switch (status) {
            case ALTERADO -> "alterado";
            case REVOGADO -> "revogado";
            case INCLUIDO -> "incluído";
            default -> "modificado";
        };
        String ref = "(" + acao + " pela Portaria DIRAD n° XYZ, de DD de MÊS de AAAA,"
                + " publicada no BCA n° ABC, de DD de mês de AAAA)";
        return wrapEmendaInline(ref);
    }

    private String wrapEmendaInline(String ref) {
        // text-decoration="none" é necessário porque este inline pode ser inserido
        // dentro de um fo:block tachado (REVOGADO) — sem isso, herdaria o tachado do
        // pai e a cláusula apareceria riscada, o que não pode ocorrer para a cláusula
        // ATUAL (só o texto legislativo é tachado, nunca a cláusula que o acompanha).
        return "<fo:inline font-size=\"10pt\" font-style=\"italic\" color=\"#0000FF\" text-decoration=\"none\">"
             + foEsc(" " + ref) + "</fo:inline>";
    }

    // Variante para clausulaEmendaAnterior: SEM text-decoration="none" de propósito —
    // herda o tachado do bloco pai, porque esta cláusula descreve a redação que está
    // sendo substituída (ex.: "incluído pela Portaria X" junto do texto que ela
    // introduziu), então deve aparecer riscada junto com esse texto.
    private String wrapEmendaInlineStruck(String ref) {
        return "<fo:inline font-size=\"10pt\" font-style=\"italic\" color=\"#0000FF\">"
             + foEsc(" " + ref) + "</fo:inline>";
    }

    // Inserts an inline fragment just before the last </fo:block> in a local buffer.
    private void insertBeforeLastBlockClose(StringBuilder sb, String inline) {
        if (inline == null || inline.isBlank()) return;
        String s = sb.toString();
        int idx = s.lastIndexOf("</fo:block>");
        if (idx < 0) return;
        sb.setLength(0);
        sb.append(s, 0, idx).append(inline).append(s.substring(idx));
    }
}
