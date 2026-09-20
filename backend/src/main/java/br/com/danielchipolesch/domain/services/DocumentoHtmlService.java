package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.VersaoDocumentoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentoException;
import br.com.danielchipolesch.domain.util.tiptap.TipTapHtmlSerializer;
import br.com.danielchipolesch.domain.util.tiptap.TipTapNode;
import br.com.danielchipolesch.infrastructure.repositories.AnexoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DocumentoHtmlService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoParteNormativaService documentoParteNormativaService;

    @Autowired
    private AnexoRepository anexoRepository;

    @Autowired
    private ImagemService imagemService;

    @Autowired
    private NumeracaoService numeracaoService;

    // Só o Brasão da República: o Gládio Alado (brasaoFab) só aparecia na capa,
    // dispensada em HTML (NSCA 5-3, Art. 17, V, §1º).
    private String brasaoRepublica = "";

    @PostConstruct
    private void loadStaticImages() {
        brasaoRepublica = classpathDataUri("/images/brasao-do-brasil-republica-colorido.png", "image/png");
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
            List<ItemAnexoParteNormativaResponseDto> normativos,
            List<AnexoResponseDto> anexos) {
        return new Generator(doc, preliminares, normativos, anexos, brasaoRepublica, objectMapper, imagemService, numeracaoService).gerar();
    }

    // Espelha DocumentoPdfService.streamPdf -- mesmo padrão de cópia armazenada vs.
    // renderização ao vivo, só que devolvendo os bytes UTF-8 do HTML direto (sem
    // streaming incremental: ao contrário do PDF, que pode passar de 1MB com
    // imagens embutidas, justificando StreamingResponseBody, o HTML deste tamanho
    // não compensa a complexidade extra).
    public StreamingResponseBody streamHtml(Long documentoId, VersaoDocumentoEnum pedida) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        // Mesma regra do PDF (ver DocumentoPdfService.streamPdf e VersoesDocumento).
        VersaoDocumentoEnum versao = VersoesDocumento.resolver(doc, pedida);
        String urlArmazenada = versao == VersaoDocumentoEnum.VIGENTE ? doc.getUrlHtml()
                : (VersoesDocumento.emTramitacaoArmazenada(doc) ? doc.getUrlHtmlTramitacao() : null);

        if (urlArmazenada != null) {
            InputStream armazenado = imagemService.getObjectStream(urlArmazenada);
            if (armazenado != null) {
                return outputStream -> {
                    try (armazenado) {
                        armazenado.transferTo(outputStream);
                    }
                };
            }
            // URL presente mas não recuperável (objeto removido/inconsistência): recai
            // na renderização ao vivo em vez de falhar a exportação.
        }
        byte[] renderizado = renderHtml(doc).getBytes(StandardCharsets.UTF_8);
        return outputStream -> outputStream.write(renderizado);
    }

    // Espelha DocumentoPdfService.gerarEArmazenarPdf -- mesmo motivo pro
    // readOnly=true (ver comentário lá: auto-flush no meio da travessia recursiva
    // de getItensNormativosByDocumento quebra com "collection with orphan deletion
    // was no longer referenced").
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public String gerarEArmazenarHtml(Documento documento) {
        try {
            byte[] htmlBytes = renderHtml(documento).getBytes(StandardCharsets.UTF_8);
            String filename = "documento-" + documento.getId() + "-" + Instant.now().toEpochMilli() + ".html";
            return imagemService.uploadHtml(htmlBytes, filename);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar/armazenar HTML: " + e.getMessage(), e);
        }
    }

    private String renderHtml(Documento doc) {
        Long id = doc.getId();

        List<ItemPartePreliminarResponseDto> preliminares =
                documentoParteNormativaService.getItensPreliminaresByDocumento(id)
                        .stream().map(ItemPartePreliminarResponseDto::from).toList();

        List<ItemAnexoParteNormativaResponseDto> normativos =
                documentoParteNormativaService.getItensNormativosByDocumento(id)
                        .stream().map(ItemAnexoParteNormativaResponseDto::from).toList();

        List<AnexoResponseDto> anexos = anexoRepository.findByDocumentoIdOrderByOrdemAsc(id)
                .stream().map(AnexoResponseDto::from).toList();

        return gerarHtml(doc, preliminares, normativos, anexos);
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
        private final List<AnexoResponseDto> anexos;
        private final String brasaoRepublica;
        private final ObjectMapper objectMapper;
        private final ImagemService imagemService;
        private final NumeracaoService numeracaoService;
        private final Map<Long, NumeracaoService.ElementoNumeracao> numeracao;

        Generator(Documento doc,
                  List<ItemPartePreliminarResponseDto> preliminares,
                  List<ItemAnexoParteNormativaResponseDto> normativos,
                  List<AnexoResponseDto> anexos,
                  String brasaoRepublica,
                  ObjectMapper objectMapper,
                  ImagemService imagemService,
                  NumeracaoService numeracaoService) {
            this.doc = doc;
            this.preliminares    = preliminares != null ? preliminares : List.of();
            this.normativos      = normativos   != null ? normativos   : List.of();
            this.anexos          = anexos       != null ? anexos       : List.of();
            this.brasaoRepublica = brasaoRepublica;
            this.objectMapper    = objectMapper;
            this.imagemService   = imagemService;
            this.numeracaoService = numeracaoService;
            this.numeracao       = numeracaoService.calcular(this.normativos);
        }

        // ─── Entry point ─────────────────────────────────────────────────────────

        String gerar() {
            // NSCA 5-3, Art. 17, V, §1º: capa é dispensada na versão HTML -- só
            // Portaria + Sumário/Corpo, sem a página de capa que o PDF tem.
            return "<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"/>\n<style>\n"
                    + buildCss()
                    + "\n</style>\n</head>\n<body>\n"
                    + buildPortaria()
                    + buildSumarioECorpo()
                    + buildAnexos()
                    + "\n</body>\n</html>";
        }

        // ─── CSS ─────────────────────────────────────────────────────────────────

        private String buildCss() {
            return """
                @page { size: A4; margin: 2cm; }
                * { box-sizing: border-box; }
                body {
                    font-family: 'Calibri', 'Carlito', 'Segoe UI', Arial, sans-serif;
                    font-size: 12pt;
                    line-height: 1.2;
                    color: #000;
                    /* NSCA 5-3, Art. 8, XXI: texto do ato normativo alinhado à esquerda em
                       HTML (não justificado como no PDF) -- exceção nos agrupamentos
                       (capítulo/seção/subseção), que continuam centralizados (ver
                       .capitulo-heading/.secao-heading, inalterados). */
                    text-align: left;
                    margin: 0;
                }
                /* Respiro lateral só pra leitura direto no navegador -- @media screen
                   nunca se aplica ao imprimir (Ctrl+P usa @page acima, que já está bom
                   e fica intocado), então a impressão continua exatamente como antes. */
                @media screen {
                    body { padding: 0 16px; }
                    /* page-break-after não produz nenhum espaço visual fora da impressão
                       paginada -- sem isso, o fim de uma "página" (ex.: assinatura da
                       Portaria) encosta direto no início da próxima (ex.: "ANEXO I") ao
                       rolar a tela. Só aqui, não interfere no Ctrl+P (que já quebra a
                       página certinho via page-break-after abaixo). */
                    .page-break { margin-bottom: 40px; padding-bottom: 24px; border-bottom: 1px solid #ddd; }
                }
                .page-break { page-break-after: always; }
                /* Selo da revogação total: canto superior direito da página da Portaria (também
                   na impressão). Nenhum elemento é tachado -- só o selo. */
                .page-break { position: relative; }
                .selo-revogado { position: absolute; top: 0; right: 0; border: 2px solid #C00000; color: #C00000;
                                 font-weight: bold; font-size: 14pt; padding: 2pt 8pt; text-align: center; }
                /* NSCA 5-3, Art. 18: cabeçalho antecede a epígrafe em HTML: Brasão da
                   República alinhado à esquerda, verticalmente centralizado ao lado das
                   3 linhas (não empilhado acima delas), demais elementos centralizados
                   na página, entrelinhas simples (1,0) -- diferente do PDF, onde tudo é
                   centralizado, brasão incluso. Grid de 3 colunas (brasão | texto | vazia
                   do mesmo tamanho do brasão) centraliza o texto na página mesmo com o
                   brasão ocupando espaço só do lado esquerdo. */
                .cabecalho {
                    display: grid;
                    grid-template-columns: 45pt 1fr 45pt;
                    align-items: center;
                    column-gap: 10pt;
                    margin-bottom: 10pt;
                    line-height: 1.0;
                }
                .cabecalho-brasao { width: 45pt; height: 45pt; grid-column: 1; }
                .cabecalho-textos { grid-column: 2; text-align: center; }
                .cabecalho-textos p { margin: 0; text-align: center; font-size: 12pt; line-height: 1.0; }
                .bold { font-weight: bold; }
                .underline { text-decoration: underline; }
                .epigrafe { text-align: center; text-transform: uppercase; font-weight: normal; margin: 10pt 0 0; }
                .epigrafe p { display: inline; margin: 0; }
                .ementa-bloco { margin-left: 8cm; margin-top: 3pt; margin-bottom: 8pt; }
                .ementa-txt { margin: 0; text-align: justify; }
                .ementa-txt p { display: inline; margin: 0; }
                .body-el { text-indent: 2.5cm; margin: 0 0 5pt; line-height: 1.2; text-align: left; }
                .body-el p { display: inline; margin: 0; }
                .body-el p + p { display: block; margin-top: 5pt; text-indent: 2.5cm; }
                .body-el strong { font-weight: bold; }
                .body-el em { font-style: italic; }
                .body-el u { text-decoration: underline; }
                .norm-lbl { font-weight: normal; }
                .norm-lbl-bold { font-weight: bold; }
                .norm-content-block { display: inline; text-indent: 0; }
                .norm-content-block table { border-collapse: collapse; table-layout: fixed; width: 100%; margin: 4pt 0; font-size: 10pt; }
                .norm-content-block td, .norm-content-block th { border: 1px solid #999; padding: 3pt 6pt; vertical-align: top; }
                .norm-content-block th { background: rgba(11,61,145,0.06); font-weight: bold; text-align: center; }
                .norm-content-block p { margin: 0; text-indent: 0; }
                .fecho-bloco { text-align: left; margin-top: 20pt; margin-bottom: 5pt; }
                .fecho-bloco p { text-align: left; text-indent: 0; margin: 0; display: block; }
                .assinatura-bloco { margin-top: 36pt; text-align: center; }
                .assinatura-bloco p { margin: 0; text-align: center; text-indent: 0; display: block; }
                /* Aviso de que o HTML é só uma cópia de leitura, não o registro oficial --
                   fica na mesma página da Portaria (antes da quebra para "ANEXO I"), logo
                   após a assinatura, nunca no corpo do ato em si. */
                .aviso-nao-substitui { color: #FF0000; text-align: center; margin-top: 24pt; }
                .assin-data { margin: 0 0 24pt; }
                .assin-nome { text-transform: uppercase; margin: 0; }
                .assin-cargo { margin: 0; }
                .corpo-assin { margin-top: 24pt; text-align: center; }
                .corpo-assin p { margin: 0; display: block; text-indent: 0; }
                .sumario-label { font-weight: bold; text-align: center; margin: 0; }
                .sumario-titulo { font-weight: bold; text-align: center; margin: 0 0 10pt; }
                .sumario-art-hdr { display: block; text-align: right; font-weight: bold; font-size: 10pt; margin: 0 0 2pt; }
                .toc-table { width: 100%; border-collapse: collapse; font-size: 10pt; }
                .toc-table tr.toc-capitulo td { font-weight: bold; text-transform: uppercase; padding-top: 5pt; }
                .toc-table tr.toc-secao td { padding-left: 12pt; }
                .toc-table tr.toc-subsecao td { padding-left: 20pt; }
                .toc-table tr.toc-artigo td { padding-left: 20pt; }
                td.toc-lbl { white-space: nowrap; max-width: 60%; }
                td.toc-mid { width: 100%; border-bottom: 1px dotted #999; }
                td.toc-pg { white-space: nowrap; text-align: right; padding-left: 6pt; min-width: 40pt; }
                .toc-table a { color: inherit; text-decoration: none; }
                .toc-table a:hover { text-decoration: underline; }
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
                .emenda-strikethrough { text-decoration: line-through; color: #0000FF; }
                .emenda-incluido { color: #0000FF; }
                .emenda-ref-block { font-size: 10pt; font-style: italic; color: #0000FF; display: block; padding-left: 2.5cm; margin-bottom: 3pt; }
                .anexo-titulo { text-align: center; font-weight: bold; margin: 0 0 12pt; }
                .anexo-imagem { text-align: center; }
                .anexo-imagem img { max-width: 100%; height: auto; }
                """;
        }

        // ─── Page 1: Portaria de Aprovação ───────────────────────────────────────

        private String buildPortaria() {
            var sb = new StringBuilder();
            sb.append("<div class=\"page-break\">\n");
            if (VersoesDocumento.exibeSeloRevogado(doc)) {
                sb.append("<div class=\"selo-revogado\">REVOGADO</div>\n");
            }

            // Cabeçalho (NSCA 5-3, Art. 18): Brasão à esquerda (parágrafo único),
            // demais elementos centralizados -- inclui a OM que elaborou o ato como
            // "órgão secundário" (IV), já que a capa (onde ela apareceria, Art. 17 II)
            // é dispensada em HTML.
            sb.append("<div class=\"cabecalho\">");
            if (!brasaoRepublica.isBlank()) {
                sb.append("<img src=\"").append(brasaoRepublica).append("\"")
                  .append(" class=\"cabecalho-brasao\" alt=\"\" />");
            }
            sb.append("<div class=\"cabecalho-textos\">");
            sb.append("<p class=\"bold\">MINISTÉRIO DA DEFESA</p>");
            sb.append("<p class=\"bold\">COMANDO DA AERONÁUTICA</p>");
            if (doc.getOm() != null && doc.getOm().getNome() != null) {
                sb.append("<p class=\"bold\">").append(esc(doc.getOm().getNome().toUpperCase())).append("</p>");
            }
            sb.append("</div>\n");
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

            // Aviso de que esta cópia HTML não substitui o registro oficial publicado
            // no BCA -- fica dentro do mesmo "page-break" da Portaria (não do corpo do
            // ato), então acompanha a Portaria antes da quebra de página, nunca gruda em
            // "ANEXO I" na página seguinte.
            sb.append("<p class=\"aviso-nao-substitui\">Esta versão não substitui a publicada no BCA.</p>\n");

            sb.append("</div>\n");
            return sb.toString();
        }

        // ─── Sumário + Corpo (sem capa -- NSCA 5-3, Art. 17, V, §1º) ──────────────

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

        // ─── Anexos (arquivos vinculados ao documento, uma página própria por anexo) ─

        private String buildAnexos() {
            var sb = new StringBuilder();
            for (var anexo : anexos) {
                String numRomano = toRoman(anexo.ordem() + 1);
                sb.append("<div class=\"page-break\">\n");
                sb.append("<p class=\"anexo-titulo\">ANEXO ").append(numRomano).append("</p>\n");
                if (anexo.titulo() != null && !anexo.titulo().isBlank()) {
                    sb.append("<p class=\"anexo-titulo\">").append(esc(anexo.titulo().toUpperCase())).append("</p>\n");
                }
                if (anexo.urlImagem() != null && !anexo.urlImagem().isBlank()) {
                    String dataUri = resolveDataUri(anexo.urlImagem());
                    if (dataUri != null && !dataUri.isBlank()) {
                        sb.append("<div class=\"anexo-imagem\"><img src=\"").append(dataUri).append("\" alt=\"\" /></div>\n");
                    }
                }
                sb.append("</div>\n");
            }
            return sb.toString();
        }

        // ─── TOC ─────────────────────────────────────────────────────────────────

        private record TocEntry(String label, String cssClass, String pg, String anchor) {}

        // Rótulo (romano/ordinal, com o sufixo de letra de emenda) de um capítulo, seção,
        // subseção ou artigo -- vem do mesmo NumeracaoService que o PDF usa, nunca de
        // contadores próprios: artigo e unidades superiores em vigor não podem ser
        // renumerados (LC 95/1998), então um "Art. 5º-A" precisa sair assim no HTML também.
        private String rotulo(ItemAnexoParteNormativaResponseDto item) {
            var n = numeracao.get(item.id());
            return n != null ? n.label() : "";
        }

        private String buildToc() {
            List<TocEntry> entries = new ArrayList<>();
            if (numeracaoService.temAgrupamento(normativos)) {
                walkToc(normativos, entries);
            } else {
                collectArticleToc(normativos, entries);
            }

            if (entries.isEmpty()) return "";

            var sb = new StringBuilder();
            // Rótulo da coluna de número de artigo -- ver mesmo cabeçalho no PDF
            // (DocumentoFoCorpoBuilder.buildToc) e na prévia (DocumentoPreview.vue
            // .sumario-header-row); sem isto o número solto na coluna direita do
            // sumário fica sem indicar o que representa.
            sb.append("<p class=\"sumario-art-hdr\">Art.</p>\n");
            sb.append("<table class=\"toc-table\">\n");
            for (var e : entries) {
                sb.append("<tr class=\"").append(e.cssClass()).append("\">");
                sb.append("<td class=\"toc-lbl\"><a href=\"#").append(e.anchor()).append("\">")
                  .append(esc(e.label())).append("</a></td>");
                sb.append("<td class=\"toc-mid\"></td>");
                sb.append("<td class=\"toc-pg\"><a href=\"#").append(e.anchor()).append("\">")
                  .append(esc(e.pg())).append("</a></td>");
                sb.append("</tr>\n");
            }
            sb.append("</table>\n");
            return sb.toString();
        }

        private void walkToc(List<ItemAnexoParteNormativaResponseDto> items, List<TocEntry> entries) {
            for (int i = 0; i < items.size(); i++) {
                var item = items.get(i);
                switch (item.elementType()) {
                    case CAPITULO -> {
                        String t = item.elementTitle() != null ? " - " + item.elementTitle().toUpperCase() : "";
                        entries.add(new TocEntry("CAPÍTULO " + rotulo(item) + t, "toc-capitulo",
                                numeracaoService.intervaloArtigos(item, items, i, numeracao), "norm-" + item.id()));
                        if (item.children() != null) walkToc(item.children(), entries);
                    }
                    case SECAO_NORMATIVA -> {
                        String t = item.elementTitle() != null ? " - " + item.elementTitle() : "";
                        entries.add(new TocEntry("Seção " + rotulo(item) + t, "toc-secao",
                                numeracaoService.intervaloArtigos(item, items, i, numeracao), "norm-" + item.id()));
                        if (item.children() != null) walkToc(item.children(), entries);
                    }
                    case SUBSECAO_NORMATIVA -> {
                        String t = item.elementTitle() != null ? " - " + item.elementTitle() : "";
                        entries.add(new TocEntry("Subseção " + rotulo(item) + t, "toc-subsecao",
                                numeracaoService.intervaloArtigos(item, items, i, numeracao), "norm-" + item.id()));
                        if (item.children() != null) walkToc(item.children(), entries);
                    }
                    default -> {}
                }
            }
        }

        private void collectArticleToc(List<ItemAnexoParteNormativaResponseDto> items, List<TocEntry> entries) {
            for (var item : items) {
                if (item.elementType() == ItemAnexoParteNormativaTipoEnum.ARTIGO) {
                    entries.add(new TocEntry("Art. " + rotulo(item), "toc-artigo",
                            numeracaoService.pontoFinalArtigo(item, numeracao), "norm-" + item.id()));
                }
            }
        }

        // ─── Corpo normativo ─────────────────────────────────────────────────────

        private String buildCorpoNormativo() {
            var sb = new StringBuilder();
            renderNormItems(normativos, sb);
            return sb.toString();
        }

        private void renderNormItems(List<ItemAnexoParteNormativaResponseDto> items, StringBuilder sb) {
            if (items == null) return;
            for (var item : items) renderNormItem(item, sb);
        }

        private void renderNormItem(ItemAnexoParteNormativaResponseDto item, StringBuilder sb) {
            switch (item.elementType()) {
                case CAPITULO -> {
                    sb.append("<div class=\"capitulo-heading\" id=\"norm-").append(item.id()).append("\">");
                    sb.append("<p class=\"cap-numero\">CAPÍTULO ").append(rotulo(item)).append("</p>");
                    renderGroupingEmenda(sb, item.elementTitle(), item.tituloEmenda(), true, "cap-titulo",
                            item.emendaStatus(), item.clausulaEmenda(), item.clausulaEmendaAnterior());
                    sb.append("</div>\n");
                    renderNormItems(item.children(), sb);
                }
                case SECAO_NORMATIVA -> {
                    sb.append("<div class=\"secao-heading\" id=\"norm-").append(item.id()).append("\">");
                    sb.append("<p class=\"sec-numero\"><strong>Seção ").append(rotulo(item)).append("</strong></p>");
                    renderGroupingEmenda(sb, item.elementTitle(), item.tituloEmenda(), false, "sec-titulo",
                            item.emendaStatus(), item.clausulaEmenda(), item.clausulaEmendaAnterior());
                    sb.append("</div>\n");
                    renderNormItems(item.children(), sb);
                }
                case SUBSECAO_NORMATIVA -> {
                    sb.append("<div class=\"secao-heading\" id=\"norm-").append(item.id()).append("\">");
                    sb.append("<p class=\"sec-numero\"><strong>Subseção ").append(rotulo(item)).append("</strong></p>");
                    renderGroupingEmenda(sb, item.elementTitle(), item.tituloEmenda(), false, "sec-titulo",
                            item.emendaStatus(), item.clausulaEmenda(), item.clausulaEmendaAnterior());
                    sb.append("</div>\n");
                    renderNormItems(item.children(), sb);
                }
                case ARTIGO -> {
                    sb.append("<a id=\"norm-").append(item.id()).append("\"></a>");
                    renderBodyEl(sb, "Art. " + rotulo(item) + S2, true,
                            item.elementContent(), item.emendaStatus(), item.conteudoEmenda(),
                                item.clausulaEmenda(), item.clausulaEmendaAnterior());
                    renderArtigoChildren(item.children(), sb);
                }
                default -> renderBodyEl(sb, "", false, item.elementContent(),
                        item.emendaStatus(), item.conteudoEmenda(),
                                item.clausulaEmenda(), item.clausulaEmendaAnterior());
            }
        }

        private void renderArtigoChildren(List<ItemAnexoParteNormativaResponseDto> children, StringBuilder sb) {
            if (children == null) return;
            // Mesma regra do PDF (parágrafo em vigor nunca é renumerado; incluído por emenda
            // recebe letra): vem de NumeracaoService.numerarParagrafos -- ver o comentário lá.
            var paragrafos = children.stream()
                    .filter(c -> c.elementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO
                              || c.elementType() == ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO)
                    .toList();
            var numeracaoParagrafos = NumeracaoService.numerarParagrafos(paragrafos);
            int parIdx = 0, incisoNum = 0;
            for (var child : children) {
                switch (child.elementType()) {
                    case PARAGRAFO, PARAGRAFO_UNICO -> {
                        var n = numeracaoParagrafos.get(parIdx++);
                        String parLabel = n.semNumero() ? "Parágrafo único." + S2 : n.label() + S2;
                        // Parágrafo único que virou "§ Nº": a linha inteira "Parágrafo único. texto"
                        // sai riscada e o texto se repete sob o novo número, com a cláusula.
                        boolean renumerado = NumeracaoService.unicoRenumerado(paragrafos, child,
                                doc.getSituacaoBca() != SituacaoBcaEnum.NAO_PUBLICADO);
                        if (renumerado) {
                            renderBodyElStyled(sb, "Parágrafo único." + S2, false, child.elementContent(),
                                    "emenda-strikethrough");
                        }
                        renderBodyEl(sb, parLabel, false, child.elementContent(),
                                child.emendaStatus(), child.conteudoEmenda(),
                                child.clausulaEmenda(), child.clausulaEmendaAnterior());
                        if (renumerado) {
                            sb.append("<span class=\"emenda-ref-block\">")
                              .append(esc(NumeracaoService.clausulaRenumeracao(child))).append("</span>\n");
                        }
                        renderIncisoChildren(child.children(), sb);
                    }
                    case INCISO -> {
                        incisoNum++;
                        String incLabel = toRoman(incisoNum) + S1 + "-" + S1;
                        renderBodyEl(sb, incLabel, false, child.elementContent(),
                                child.emendaStatus(), child.conteudoEmenda(),
                                child.clausulaEmenda(), child.clausulaEmendaAnterior());
                        renderAlineaChildren(child.children(), sb);
                    }
                    default -> renderBodyEl(sb, "", false, child.elementContent(),
                            child.emendaStatus(), child.conteudoEmenda(),
                                child.clausulaEmenda(), child.clausulaEmendaAnterior());
                }
            }
        }

        private void renderIncisoChildren(List<ItemAnexoParteNormativaResponseDto> children, StringBuilder sb) {
            if (children == null) return;
            int n = 0;
            for (var child : children) {
                if (child.elementType() == ItemAnexoParteNormativaTipoEnum.INCISO) {
                    n++;
                    renderBodyEl(sb, toRoman(n) + S1 + "-" + S1, false, child.elementContent(),
                            child.emendaStatus(), child.conteudoEmenda(),
                                child.clausulaEmenda(), child.clausulaEmendaAnterior());
                    renderAlineaChildren(child.children(), sb);
                }
            }
        }

        private void renderAlineaChildren(List<ItemAnexoParteNormativaResponseDto> children, StringBuilder sb) {
            if (children == null) return;
            int n = 0;
            for (var child : children) {
                if (child.elementType() == ItemAnexoParteNormativaTipoEnum.ALINEA) {
                    n++;
                    renderBodyEl(sb, toLetter(n) + ")" + S1, false, child.elementContent(),
                            child.emendaStatus(), child.conteudoEmenda(),
                                child.clausulaEmenda(), child.clausulaEmendaAnterior());
                    renderSubAlineaChildren(child.children(), sb);
                }
            }
        }

        private void renderSubAlineaChildren(List<ItemAnexoParteNormativaResponseDto> children, StringBuilder sb) {
            if (children == null) return;
            int n = 0;
            for (var child : children) {
                if (child.elementType() == ItemAnexoParteNormativaTipoEnum.SUB_ALINEA) {
                    n++;
                    renderBodyEl(sb, n + "." + S1, false, child.elementContent(),
                            child.emendaStatus(), child.conteudoEmenda(),
                                child.clausulaEmenda(), child.clausulaEmendaAnterior());
                }
            }
        }

        // conteudo     = original published content (shown struck through for ALTERADO/REVOGADO)
        // conteudoEmenda = new amendment content (shown as current text for ALTERADO)
        // Mesmas regras do PDF (DocumentoFoCorpoBuilder.renderBodyEl):
        //   clausulaEmenda         = cláusula CONGELADA na publicação daquela emenda (a portaria/BCA
        //                            daquela alteração, não a da última publicação); null enquanto
        //                            a emenda está pendente -> placeholder XYZ/ABC
        //   clausulaEmendaAnterior = cláusula da redação anterior, mostrada riscada junto do texto
        //                            que ela descreve
        private void renderBodyEl(StringBuilder sb, String label, boolean labelBold, String conteudo,
                                   ElementoEmendaStatusEnum emendaStatus, String conteudoEmenda,
                                   String clausulaEmenda, String clausulaEmendaAnterior) {
            if (emendaStatus == null || emendaStatus == ElementoEmendaStatusEnum.INALTERADO) {
                renderBodyEl(sb, label, labelBold, conteudo);
                return;
            }
            switch (emendaStatus) {
                case REVOGADO -> {
                    renderBodyElStyled(sb, label, labelBold, conteudo, "emenda-strikethrough");
                    sb.append(buildEmendaRefAnterior(clausulaEmendaAnterior));
                    sb.append(buildEmendaRef(emendaStatus, clausulaEmenda));
                }
                case ALTERADO -> {
                    renderBodyElStyled(sb, label, labelBold, conteudo, "emenda-strikethrough");
                    sb.append(buildEmendaRefAnterior(clausulaEmendaAnterior));
                    renderBodyElStyled(sb, label, labelBold, conteudoEmenda, "emenda-incluido");
                    sb.append(buildEmendaRef(emendaStatus, clausulaEmenda));
                }
                case INCLUIDO -> {
                    renderBodyElStyled(sb, label, labelBold, conteudo, "emenda-incluido");
                    sb.append(buildEmendaRef(emendaStatus, clausulaEmenda));
                }
            }
        }

        // Cabeçalho de capítulo/seção/subseção ciente de emenda (espelha
        // DocumentoFoCorpoBuilder.renderGroupingHeading): título original riscado para
        // REVOGADO/ALTERADO, novo título para ALTERADO/INCLUIDO e a cláusula da emenda.
        private void renderGroupingEmenda(StringBuilder sb, String titulo, String tituloEmenda, boolean uppercase,
                                          String classeTitulo, ElementoEmendaStatusEnum status,
                                          String clausulaEmenda, String clausulaEmendaAnterior) {
            boolean negrito = classeTitulo.startsWith("sec");
            java.util.function.Function<String, String> fmt = t -> {
                String e = esc(uppercase ? t.toUpperCase() : t);
                return negrito ? "<strong>" + e + "</strong>" : e;
            };
            boolean temTitulo = titulo != null && !titulo.isBlank();
            if (status == null || status == ElementoEmendaStatusEnum.INALTERADO) {
                if (temTitulo) sb.append("<p class=\"").append(classeTitulo).append("\">").append(fmt.apply(titulo)).append("</p>");
                return;
            }
            if ((status == ElementoEmendaStatusEnum.REVOGADO || status == ElementoEmendaStatusEnum.ALTERADO) && temTitulo) {
                sb.append("<p class=\"").append(classeTitulo).append(" emenda-strikethrough\">")
                  .append(fmt.apply(titulo)).append("</p>");
                sb.append(buildEmendaRefAnterior(clausulaEmendaAnterior));
            }
            if (status == ElementoEmendaStatusEnum.ALTERADO || status == ElementoEmendaStatusEnum.INCLUIDO) {
                String texto = status == ElementoEmendaStatusEnum.ALTERADO ? tituloEmenda : titulo;
                if (texto != null && !texto.isBlank()) {
                    sb.append("<p class=\"").append(classeTitulo).append("\">").append(fmt.apply(texto)).append("</p>");
                }
            }
            sb.append(buildEmendaRef(status, clausulaEmenda));
        }

        private void renderBodyElStyled(StringBuilder sb, String label, boolean labelBold,
                                         String conteudo, String extraClass) {
            String safe = processContent(conteudo);
            boolean hasBlock = hasBlockContent(safe);
            String labelHtml = labelBold
                    ? "<span class=\"norm-lbl norm-lbl-bold\">" + label + "</span>"
                    : "<span class=\"norm-lbl\">" + label + "</span>";
            // Always use <div> to avoid invalid <p>-in-<p> nesting when TipTap content
            // contains <p> tags — browsers auto-close the outer <p>, losing CSS inheritance.
            if (hasBlock) {
                sb.append("<div class=\"body-el norm-el ").append(extraClass).append("\">")
                  .append(labelHtml)
                  .append("<div class=\"norm-content-block\">").append(safe).append("</div>")
                  .append("</div>\n");
            } else {
                sb.append("<div class=\"body-el norm-el ").append(extraClass).append("\">")
                  .append(labelHtml).append(safe).append("</div>\n");
            }
        }

        private String buildEmendaRefAnterior(String clausulaEmendaAnterior) {
            if (clausulaEmendaAnterior == null || clausulaEmendaAnterior.isBlank()) return "";
            return "<span class=\"emenda-ref-block emenda-strikethrough\">" + esc(clausulaEmendaAnterior) + "</span>\n";
        }

        private String buildEmendaRef(ElementoEmendaStatusEnum status, String clausulaEmenda) {
            String acao = switch (status) {
                case ALTERADO -> "redação dada";
                case REVOGADO -> "revogado";
                case INCLUIDO -> "incluído";
                default       -> "modificado";
            };
            // Emenda já publicada: a cláusula congelada (portaria/BCA DAQUELA alteração). Pendente:
            // sempre o placeholder XYZ/ABC -- nunca a portaria da última publicação, que não tem
            // relação com a emenda em curso (mesma regra do PDF: DocumentoFoCorpoBuilder.emendaRefInline).
            String ref = clausulaEmenda != null ? clausulaEmenda
                    : "(" + acao + " pela Portaria DIRAD n° XYZ, de DD de MÊS de AAAA,"
                      + " publicada no BCA n° ABC, de DD de mês de AAAA)";
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
                sb.append("<div class=\"body-el norm-el\">")
                  .append(labelHtml)
                  .append(safe)
                  .append("</div>\n");
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

        // Substitui src="http://..." por data URIs -- garante imagens independente de
        // rede/Docker. Mesma ordem de resolução que XslFoContentRenderer usa pro PDF:
        // MinIO autenticado primeiro (imagemService.getImageAsDataUri), HTTP cru só
        // como fallback pra imagem genuinamente externa (fora do MinIO deste sistema).
        // O fallback HTTP sozinho NUNCA funciona pra imagem do MinIO: minio.public-url
        // (http://localhost:9000) é o endereço que o NAVEGADOR do usuário usa, não
        // resolve de dentro do container do backend (cada container tem seu próprio
        // "localhost") -- é exatamente por isso que o resolvedor MinIO existe.
        private String embedImgDataUris(String html) {
            return IMG_SRC_HTTP.matcher(html).replaceAll(mr -> {
                String q   = mr.group(1);
                String url = mr.group(2);
                return "src=" + q + resolveDataUri(url) + q;
            });
        }

        private String resolveDataUri(String url) {
            if (imagemService != null) {
                String viaMinio = imagemService.getImageAsDataUri(url);
                if (viaMinio != null && !viaMinio.isBlank()) return viaMinio;
            }
            return fetchDataUri(url);
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
                    .filter(p -> p.elementType() == tipo)
                    .findFirst().orElse(null);
        }

        private String conteudoOuNull(ItemPartePreliminarResponseDto item) {
            if (item == null) return null;
            String c = item.elementContent();
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


        private static String esc(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }
    }
}
