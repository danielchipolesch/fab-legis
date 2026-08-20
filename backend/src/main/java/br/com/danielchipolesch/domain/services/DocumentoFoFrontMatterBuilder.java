package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;

import java.time.LocalDate;

import static br.com.danielchipolesch.domain.services.DocumentoFoContext.block;
import static br.com.danielchipolesch.domain.services.DocumentoFoContext.foEsc;

// Páginas de capa/pré-texto do documento: layout-master-set, Portaria de Aprovação,
// Capa e Anexos — tudo que NÃO é o corpo normativo em si (ver DocumentoFoCorpoBuilder).
// Instância nova por chamada de DocumentoFoBuilder.buildFo().
final class DocumentoFoFrontMatterBuilder {

    private final DocumentoFoContext ctx;
    private final String brasaoRepublica;
    private final String brasaoFab;
    private final ImagemService imagemService;

    DocumentoFoFrontMatterBuilder(DocumentoFoContext ctx, String brasaoRepublica, String brasaoFab,
                                   ImagemService imagemService) {
        this.ctx = ctx;
        this.brasaoRepublica = brasaoRepublica;
        this.brasaoFab = brasaoFab;
        this.imagemService = imagemService;
    }

    // ─── Page master ──────────────────────────────────────────────────────────

    String buildLayoutMasterSet() {
        return """
            <fo:layout-master-set>
              <fo:simple-page-master master-name="a4"
                  page-width="21cm" page-height="29.7cm"
                  margin-top="2cm" margin-bottom="1cm"
                  margin-left="2cm" margin-right="2cm">
                <fo:region-body region-name="xsl-region-body" margin-bottom="1cm"/>
                <fo:region-before region-name="wm" extent="0pt" overflow="visible"/>
                <fo:region-after region-name="xsl-region-after" extent="1cm" display-align="after"/>
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

    // ─── Page 1: Portaria ─────────────────────────────────────────────────────

    String buildPortariaSequence() {
        var sb = new StringBuilder();
        sb.append("<fo:page-sequence master-reference=\"a4-nofooter\">\n");
        sb.append(ctx.buildStaticContentWatermark());
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
        var epigrafe = ctx.findPreli(ItemAnexoParteNormativaTipoEnum.EPIGRAFE);
        String epiText = ctx.inlineTextFrom(epigrafe);
        String epiContent = !epiText.isBlank() ? epiText
                : "PORTARIA Nº ___, DE " + DocumentoFoContext.formatarDataBR(ctx.doc.getDtCriacao());
        sb.append("<fo:block text-align=\"center\" font-size=\"12pt\" space-after=\"5pt\">")
          .append(foEsc(epiContent)).append("</fo:block>\n");

        // Ementa (right side, 9cm wide: start-indent = 17cm - 9cm = 8cm)
        var ementa = ctx.findPreli(ItemAnexoParteNormativaTipoEnum.EMENTA);
        String ementaInline = ctx.inlineFromConteudo(ementa != null ? ementa.getElementContent() : null);
        String ementaDefault = "Aprova a " + ctx.especieCompleta()
                + " que dispõe sobre " + foEsc(ctx.doc.getAssuntoBasico().getNome()) + ".";
        sb.append("<fo:block start-indent=\"8cm\" text-align=\"justify\" space-after=\"8pt\" font-size=\"12pt\">")
          .append(!ementaInline.isBlank() ? ementaInline : ementaDefault)
          .append("</fo:block>\n");

        // Preâmbulo (multi-parágrafo, cada um com recuo de 2,5 cm)
        var preambulo = ctx.findPreli(ItemAnexoParteNormativaTipoEnum.PREAMBULO);
        if (preambulo != null && preambulo.getElementContent() != null) {
            sb.append(ctx.renderParasBlock(preambulo.getElementContent(), "2.5cm", "justify", null));
        } else {
            sb.append("<fo:block text-indent=\"2.5cm\" text-align=\"justify\" space-after=\"5pt\">")
              .append("<fo:inline font-weight=\"bold\">O COMANDANTE DA AERONÁUTICA</fo:inline>")
              .append(", no uso das atribuições que lhe confere o art. 12 da Lei Complementar n° 97, de 9 de junho de 1999,")
              .append("</fo:block>\n");
        }

        // Fecho (alinhado à esquerda)
        var fecho = ctx.findPreli(ItemAnexoParteNormativaTipoEnum.FECHO);
        if (fecho != null && fecho.getElementContent() != null) {
            sb.append(ctx.renderParasBlock(fecho.getElementContent(), null, "right", "20pt"));
        } else {
            sb.append("<fo:block text-align=\"right\" space-before=\"20pt\" space-after=\"5pt\">")
              .append("Brasília, ").append(DocumentoFoContext.formatarDataBR(ctx.doc.getDtCriacao())).append("</fo:block>\n");
        }

        // Assinatura (centralizada)
        var assinatura = ctx.findPreli(ItemAnexoParteNormativaTipoEnum.ASSINATURA);
        if (assinatura != null && assinatura.getElementContent() != null) {
            sb.append(ctx.renderParasBlock(assinatura.getElementContent(), null, "center", "36pt"));
        } else {
            sb.append(block("Comandante da Aeronáutica", "center", "12pt", "normal", "36pt", "0"));
        }

        sb.append("</fo:flow>\n</fo:page-sequence>\n");
        return sb.toString();
    }

    // ─── Page 2: Capa ─────────────────────────────────────────────────────────

    String buildCapaSequence() {
        var sb = new StringBuilder();
        sb.append("<fo:page-sequence master-reference=\"a4-nofooter\">\n");
        sb.append(ctx.buildStaticContentWatermark());
        sb.append("<fo:flow flow-name=\"xsl-region-body\">\n");

        sb.append(block("MINISTÉRIO DA DEFESA", "center", "17pt", "bold", "0", "2pt"));
        sb.append(block("COMANDO DA AERONÁUTICA", "center", "17pt", "bold", "0", "35mm"));

        if (!brasaoFab.isBlank()) {
            sb.append("<fo:block text-align=\"center\" space-after=\"35mm\">")
              .append("<fo:external-graphic src=\"url('").append(brasaoFab).append("')\"")
              .append(" content-width=\"260pt\" content-height=\"260pt\" scaling=\"uniform\"/>")
              .append("</fo:block>\n");
        }

        String assunto = ctx.doc.getAssuntoBasico().getNome().toUpperCase();
        sb.append(block(assunto, "center", "21pt", "bold", "0", "15mm"));

        // Legenda box: espécie+número no topo, título no meio, ano na base
        // Tabela externa centra horizontalmente a caixa (3.5 | 10 | 3.5 cm)
        // Tabela interna divide a célula em 3 linhas de altura fixa (1.2+2.6+1.2=5cm)
        String titulo = ctx.doc.getTituloDocumento() != null
                ? ctx.doc.getTituloDocumento().toUpperCase() : ctx.especieCompleta().toUpperCase();
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
          .append(foEsc(ctx.docId())).append("</fo:block>\n");
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

    // ─── Anexos ───────────────────────────────────────────────────────────────

    String buildAnexoSequence(AnexoResponseDto anexo) {
        var sb = new StringBuilder();
        sb.append("<fo:page-sequence master-reference=\"a4\">\n");
        sb.append("<fo:static-content flow-name=\"xsl-region-after\">\n");
        sb.append("  <fo:block text-align=\"right\" font-size=\"10pt\"><fo:page-number/></fo:block>\n");
        sb.append("</fo:static-content>\n");
        sb.append(ctx.buildStaticContentWatermark());
        sb.append("<fo:flow flow-name=\"xsl-region-body\">\n");

        String numRomano = NumeracaoService.toRoman(anexo.getOrdem() + 1);
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
}
