package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentException;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.util.List;

@Service
public class DocumentoPdfService {

    private static final FopFactory FOP_FACTORY;

    static {
        try {
            FOP_FACTORY = FopFactory.newInstance(new File(".").toURI());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoParteNormativaService documentoParteNormativaService;

    @Autowired
    private DocumentoFoBuilder documentoFoBuilder;

    @Autowired
    private ImagemService imagemService;

    public byte[] gerarPdfBytes(Long documentoId) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));
        return renderPdf(doc);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String gerarEArmazenarPdf(Documento documento) {
        try {
            byte[] pdfBytes = renderPdf(documento);
            String filename = "documento-" + documento.getId() + ".pdf";
            return imagemService.uploadPdf(pdfBytes, filename);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar/armazenar PDF: " + e.getMessage(), e);
        }
    }

    private byte[] renderPdf(Documento doc) {
        Long id = doc.getId();

        List<ItemPartePreliminarResponseDto> preliminares =
                documentoParteNormativaService.getItensPreliminaresByDocumento(id)
                        .stream().map(ItemPartePreliminarResponseDto::from).toList();

        List<ItemAnexoParteNormativaResponseDto> normativos =
                documentoParteNormativaService.getItensNormativosByDocumento(id)
                        .stream().map(ItemAnexoParteNormativaResponseDto::from).toList();

        String fo = documentoFoBuilder.buildFo(doc, preliminares, normativos);

        try (var os = new ByteArrayOutputStream()) {
            Fop fop = FOP_FACTORY.newFop(MimeConstants.MIME_PDF, FOP_FACTORY.newFOUserAgent(), os);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            XMLReader reader = spf.newSAXParser().getXMLReader();
            reader.setContentHandler(fop.getDefaultHandler());
            reader.parse(new InputSource(new StringReader(fo)));
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao renderizar PDF: " + e.getMessage(), e);
        }
    }
}
