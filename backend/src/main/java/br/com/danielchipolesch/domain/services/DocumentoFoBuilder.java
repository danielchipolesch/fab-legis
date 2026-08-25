package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.util.tiptap.XslFoContentRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;

/**
 * Builds a complete XSL-FO document from a Documento entity + section DTOs.
 * The resulting FO string is passed to Apache FOP to produce a PDF.
 *
 * Monta o documento delegando cada seção a um builder dedicado — pré-texto
 * (DocumentoFoFrontMatterBuilder: portaria/capa/anexos) e corpo normativo
 * (DocumentoFoCorpoBuilder: sumário + capítulos/artigos) — ambos compartilhando
 * estado por requisição via DocumentoFoContext (nunca guardado em campo deste
 * bean singleton).
 */
@Service
public class DocumentoFoBuilder {

    private static final String FO_NS = "http://www.w3.org/1999/XSL/Format";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImagemService imagemService;

    @Autowired
    private NumeracaoService numeracaoService;

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
        List<ItemPartePreliminarResponseDto> preliminaresSeguro = preliminares != null ? preliminares : List.of();
        List<ItemAnexoParteNormativaResponseDto> normativosSeguro = normativos != null ? normativos : List.of();
        List<AnexoResponseDto> anexosSeguro = anexos != null ? anexos : List.of();

        var renderer = new XslFoContentRenderer();
        if (imagemService != null) {
            renderer.setImageResolver(imagemService::getImageAsDataUri);
        }
        var ctx = new DocumentoFoContext(doc, preliminaresSeguro, objectMapper, renderer);
        var frontMatter = new DocumentoFoFrontMatterBuilder(ctx, brasaoRepublica, brasaoFab, imagemService);
        var corpo = new DocumentoFoCorpoBuilder(ctx, normativosSeguro, numeracaoService);

        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<fo:root xmlns:fo=\"").append(FO_NS).append("\" xmlns:fox=\"http://xmlgraphics.apache.org/fop/extensions\">\n");
        sb.append(frontMatter.buildLayoutMasterSet());
        sb.append(frontMatter.buildPortariaSequence());
        sb.append(frontMatter.buildCapaSequence());
        sb.append(corpo.buildBodySequence());
        for (AnexoResponseDto anexo : anexosSeguro) {
            sb.append(frontMatter.buildAnexoSequence(anexo));
        }
        sb.append("</fo:root>");
        return sb.toString();
    }
}
