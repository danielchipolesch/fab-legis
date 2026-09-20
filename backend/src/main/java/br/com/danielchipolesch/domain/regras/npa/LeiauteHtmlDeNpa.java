package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.application.dtos.npaDtos.AssinaturaDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.regras.ElementoNumeracao;
import br.com.danielchipolesch.domain.regras.LeiauteDoHtml;
import br.com.danielchipolesch.domain.services.ImagemService;
import br.com.danielchipolesch.domain.services.VersoesDocumento;
import br.com.danielchipolesch.domain.util.tiptap.TipTapHtmlSerializer;
import br.com.danielchipolesch.domain.util.tiptap.TipTapNode;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// O layout HTML de uma NPA: o mesmo conteúdo do PDF (cabeçalho, corpo numerado pelo caminho, fecho e assinaturas),
// diagramado para a tela -- tudo dentro de uma moldura, texto alinhado à esquerda (como nos demais HTML da NSCA 5-3)
// e sem "n/total", que só faz sentido em páginas. Os textos vêm de CabecalhoDaNpa, os mesmos do PDF e da prévia.
@Component
public class LeiauteHtmlDeNpa implements LeiauteDoHtml {

    private static final Pattern BLOCO = Pattern.compile("(?i)<(table|ul|ol|blockquote|h[1-6]|figure)[\\s>]");

    // Elementos void sem auto-fechamento quebram parsers XHTML.
    private static final Pattern VOID_SEM_FECHAMENTO = Pattern.compile(
            "(?i)<(area|base|br|col|embed|hr|img|input|link|meta|param|source|track|wbr)((?:\\s[^>]*)?)(?<!/)>");

    private static final Pattern IMG_SRC_HTTP = Pattern.compile(
            "src=([\"'])(https?://[^\"'\\s]+)\\1", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final ImagemService imagemService;
    private final NumeracaoDeNpa numeracao;
    private final CamposDeNpa camposDeNpa;
    private final RotuloDeAnexoDeNpa rotuloDosAnexos = new RotuloDeAnexoDeNpa();

    public LeiauteHtmlDeNpa(ObjectMapper objectMapper, ImagemService imagemService, NumeracaoDeNpa numeracao,
                            CamposDeNpa camposDeNpa) {
        this.objectMapper = objectMapper;
        this.imagemService = imagemService;
        this.numeracao = numeracao;
        this.camposDeNpa = camposDeNpa;
    }

    @Override
    public String gerarHtml(Documento doc, List<ItemPartePreliminarResponseDto> preliminares,
                            List<ItemAnexoParteNormativaResponseDto> normativos, List<AnexoResponseDto> anexos) {
        var normativosSeguro = normativos != null ? normativos : List.<ItemAnexoParteNormativaResponseDto>of();
        var anexosSeguro = anexos != null ? anexos : List.<AnexoResponseDto>of();
        var cabecalho = CabecalhoDaNpa.de(doc, camposDeNpa.camposParaLeiaute(doc.getId()), anexosSeguro);
        Map<Long, ElementoNumeracao> numeros = numeracao.calcular(normativosSeguro);

        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"/>\n<style>\n").append(css()).append("\n</style>\n</head>\n<body>\n");
        sb.append("<div class=\"moldura\">\n");
        if (VersoesDocumento.exibeSeloRevogado(doc)) {
            sb.append("<div class=\"selo-revogado\">REVOGADO</div>\n");
        }
        sb.append(tabelaDoCabecalho(cabecalho));
        for (var item : normativosSeguro) renderizar(item, numeros, sb);
        sb.append(fecho(cabecalho));
        sb.append("</div>\n");
        sb.append(anexos(anexosSeguro));
        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private static String css() {
        return """
            @page { size: A4; margin: 2cm; }
            * { box-sizing: border-box; }
            body { font-family: 'Calibri', 'Carlito', 'Segoe UI', Arial, sans-serif; font-size: 12pt; line-height: 1.25;
                   color: #000; text-align: left; margin: 0; }
            @media screen { body { padding: 0 16px; } }
            .moldura { position: relative; border: 1px solid #000; padding: 10px 12px; }
            .selo-revogado { position: absolute; top: 8px; right: 8px; border: 2px solid #C00000; color: #C00000;
                             font-weight: bold; font-size: 14pt; padding: 2pt 8pt; text-align: center; }
            table.cabecalho { width: 100%; border-collapse: collapse; font-size: 10pt; margin-bottom: 14px; }
            table.cabecalho td { border: 1px solid #000; padding: 3px 5px; text-align: center; vertical-align: middle; }
            table.cabecalho .dom { height: 2.2cm; }
            .rotulo { font-weight: bold; }
            .capitulo { font-weight: bold; margin: 16px 0 6px; text-align: left; }
            .secao { margin: 10px 0 4px; }
            .secao u { text-decoration: underline; }
            .num { font-weight: bold; }
            .paragrafo, .alinea { margin: 4px 0; }
            .alinea { margin-left: 1cm; }
            .alinea .num { font-weight: normal; }
            /* O texto vem do TipTap como <p>: inline, para o número e o texto ficarem na mesma linha (como nos atos normativos). */
            .paragrafo p, .alinea p { display: inline; margin: 0; }
            .bloco { display: inline; }
            .fecho { text-align: center; margin-top: 26px; }
            .assinatura { text-align: center; margin-top: 30px; }
            .assinatura .nome { font-weight: bold; margin-top: 14px; }
            .publicada { text-align: center; font-size: 10pt; margin-top: 26px; }
            .anexo { page-break-before: always; margin-top: 30px; }
            .anexo-titulo { text-align: center; font-weight: bold; margin: 0 0 6px; }
            .anexo-imagem { text-align: center; }
            .anexo-imagem img { max-width: 100%; }
            """;
    }

    // ─── Cabeçalho ────────────────────────────────────────────────────────────

    private static String tabelaDoCabecalho(CabecalhoDaNpa c) {
        var sb = new StringBuilder("<table class=\"cabecalho\">\n");
        sb.append("<tr><td rowspan=\"3\" style=\"width:30%\"><div class=\"dom\">&nbsp;</div><b>").append(esc(c.identificacao())).append("</b></td>")
          .append("<td colspan=\"3\">");
        for (String linha : c.linhasDeCima()) sb.append("<div class=\"rotulo\">").append(esc(linha)).append("</div>");
        sb.append("</td></tr>\n");
        sb.append("<tr><td class=\"rotulo\">DATAS</td><td><div class=\"rotulo\">EMISSÃO</div>").append(esc(c.emissao()))
          .append("</td><td><div class=\"rotulo\">EFETIVAÇÃO</div>").append(esc(c.efetivacao())).append("</td></tr>\n");
        sb.append("<tr><td class=\"rotulo\">DISTRIBUIÇÃO</td><td colspan=\"2\">").append(esc(c.distribuicao())).append("</td></tr>\n");
        sb.append("<tr><td class=\"rotulo\">ASSUNTO</td><td colspan=\"3\">").append(esc(c.assunto())).append("</td></tr>\n");
        sb.append("<tr><td class=\"rotulo\">ANEXOS</td><td colspan=\"3\">").append(esc(c.anexos())).append("</td></tr>\n");
        sb.append("</table>\n");
        return sb.toString();
    }

    // ─── Corpo ────────────────────────────────────────────────────────────────

    private void renderizar(ItemAnexoParteNormativaResponseDto item, Map<Long, ElementoNumeracao> numeros, StringBuilder sb) {
        var n = numeros.get(item.id());
        String numero = esc(n != null ? n.label() : "");
        String titulo = item.elementTitle() != null ? item.elementTitle() : "";
        switch (item.elementType()) {
            case CAPITULO -> sb.append("<div class=\"capitulo\">").append(numero).append("&nbsp;&nbsp;")
                    .append(esc(titulo.toUpperCase())).append("</div>\n");
            case SECAO_NORMATIVA, SUBSECAO_NORMATIVA -> sb.append("<div class=\"secao\"><span class=\"num\">").append(numero)
                    .append("</span>&nbsp;&nbsp;<u>").append(esc(titulo)).append("</u></div>\n");
            case PARAGRAFO -> texto(item, "paragrafo", numero, sb);
            case ALINEA -> texto(item, "alinea", numero, sb);
            default -> {
                // Fora da gramática da NPA (a hierarquia o recusa no salvamento): não é desenhado.
            }
        }
        if (item.children() != null) {
            for (var filho : item.children()) renderizar(filho, numeros, sb);
        }
    }

    // Sempre <div>: o conteúdo do TipTap traz <p>, e um <p> dentro de <p> é auto-fechado pelo navegador.
    private void texto(ItemAnexoParteNormativaResponseDto item, String classe, String numero, StringBuilder sb) {
        String conteudo = conteudoEmHtml(item.elementContent());
        String rotulo = "<span class=\"num\">" + numero + "</span>&nbsp;&nbsp;";
        if (BLOCO.matcher(conteudo).find()) {
            sb.append("<div class=\"").append(classe).append("\">").append(rotulo)
              .append("<div class=\"bloco\">").append(conteudo).append("</div></div>\n");
        } else {
            sb.append("<div class=\"").append(classe).append("\">").append(rotulo).append(conteudo).append("</div>\n");
        }
    }

    private String conteudoEmHtml(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) return "";
        try {
            TipTapNode doc = objectMapper.readValue(conteudo, TipTapNode.class);
            String html = TipTapHtmlSerializer.toHtml(doc);
            html = IMG_SRC_HTTP.matcher(html).replaceAll(mr -> {
                String uri = imagemService != null ? imagemService.getImageAsDataUri(mr.group(2)) : null;
                return "src=" + mr.group(1) + (uri != null && !uri.isBlank() ? uri : mr.group(2)) + mr.group(1);
            });
            return VOID_SEM_FECHAMENTO.matcher(html).replaceAll("<$1$2 />");
        } catch (Exception e) {
            return "";
        }
    }

    // ─── Fecho e anexos ───────────────────────────────────────────────────────

    private static String fecho(CabecalhoDaNpa c) {
        var sb = new StringBuilder();
        sb.append("<div class=\"fecho\">").append(esc(c.localEData())).append("</div>\n");
        for (AssinaturaDaNpaDto assinatura : c.assinaturas()) {
            sb.append("<div class=\"assinatura\"><div>").append(esc(assinatura.rotulo())).append("</div>");
            var linhas = assinatura.linhas() != null ? assinatura.linhas() : List.<String>of();
            for (int i = 0; i < linhas.size(); i++) {
                sb.append("<div").append(i == 0 ? " class=\"nome\"" : "").append(">").append(esc(linhas.get(i))).append("</div>");
            }
            sb.append("</div>\n");
        }
        if (c.publicadaNo() != null) {
            sb.append("<div class=\"publicada\">").append(esc(c.publicadaNo())).append("</div>\n");
        }
        if (c.revogadaNo() != null) {
            sb.append("<div class=\"publicada\">").append(esc(c.revogadaNo())).append("</div>\n");
        }
        return sb.toString();
    }

    private String anexos(List<AnexoResponseDto> anexos) {
        var sb = new StringBuilder();
        for (var anexo : anexos) {
            sb.append("<div class=\"anexo\">\n<p class=\"anexo-titulo\">").append(esc(rotuloDosAnexos.rotulo(anexo.ordem()))).append("</p>\n");
            if (anexo.titulo() != null && !anexo.titulo().isBlank()) {
                sb.append("<p class=\"anexo-titulo\">").append(esc(anexo.titulo().toUpperCase())).append("</p>\n");
            }
            if (imagemService != null && anexo.urlImagem() != null && !anexo.urlImagem().isBlank()) {
                String dataUri = imagemService.getImageAsDataUri(anexo.urlImagem());
                if (dataUri != null && !dataUri.isBlank()) {
                    sb.append("<div class=\"anexo-imagem\"><img src=\"").append(dataUri).append("\" alt=\"\" /></div>\n");
                }
            }
            sb.append("</div>\n");
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
