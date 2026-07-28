package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemParteFinalDtos.ItemParteFinalResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentException;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class DocumentoPdfService {

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoParteNormativaService documentoParteNormativaService;

    @Autowired
    private DocumentoHtmlService documentoHtmlService;

    @Autowired
    private ImagemService imagemService;

    public byte[] gerarPdfBytes(Long documentoId) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));
        return renderHtml(doc);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String gerarEArmazenarPdf(Documento documento) {
        try {
            byte[] pdfBytes = renderHtml(documento);
            String filename = "documento-" + documento.getId() + ".pdf";
            return imagemService.uploadPdf(pdfBytes, filename);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar/armazenar PDF: " + e.getMessage(), e);
        }
    }

    private byte[] renderHtml(Documento doc) {
        Long id = doc.getId();

        List<ItemPartePreliminarResponseDto> preliminares =
                documentoParteNormativaService.getItensPreliminaresByDocumento(id)
                        .stream().map(ItemPartePreliminarResponseDto::from).toList();

        List<ItemAnexoParteNormativaResponseDto> normativos =
                documentoParteNormativaService.getItensNormativosByDocumento(id)
                        .stream().map(ItemAnexoParteNormativaResponseDto::from).toList();

        List<ItemParteFinalResponseDto> finais =
                documentoParteNormativaService.getItensFinaisByDocumento(id)
                        .stream().map(ItemParteFinalResponseDto::from).toList();

        String html = documentoHtmlService.gerarHtml(doc, preliminares, normativos, finais);

        try (var os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao renderizar PDF: " + e.getMessage(), e);
        }
    }
}
