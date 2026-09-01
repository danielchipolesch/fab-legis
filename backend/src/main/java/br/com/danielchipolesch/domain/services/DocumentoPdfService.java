package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentoException;
import br.com.danielchipolesch.infrastructure.repositories.AnexoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class DocumentoPdfService {

    // Situações em que o documento tem redação estável e já possui PDF salvo no
    // MinIO (gerado por DocumentoStatusService nas transições correspondentes) —
    // nesses casos o PDF é sempre servido do MinIO, nunca renderizado de novo,
    // independente da tela/botão que disparou a exportação (PUBLICADO cobre tanto
    // a primeira publicação quanto qualquer republicação).
    private static final Set<DocumentoStatusEnum> STATUS_COM_PDF_ARMAZENADO = EnumSet.of(
            DocumentoStatusEnum.APROVADO, DocumentoStatusEnum.ALTERADO, DocumentoStatusEnum.PUBLICADO);

    private static final FopFactory FOP_FACTORY = FopFactoryProvider.get();

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoParteNormativaService documentoParteNormativaService;

    @Autowired
    private DocumentoFoBuilder documentoFoBuilder;

    @Autowired
    private ImagemService imagemService;

    @Autowired
    private AnexoRepository anexoRepository;

    // Cópia armazenada: transmite os bytes do MinIO direto para a resposta HTTP à
    // medida que chegam (StreamingResponseBody), sem materializar o PDF inteiro em
    // memória no backend — o antigo getObjectBytes() lia tudo com readAllBytes()
    // antes de responder, dobrando a latência (espera MinIO->backend, só então
    // começa backend->navegador) e retendo o arquivo inteiro no heap por requisição.
    // Renderização ao vivo (fallback): permanece como estava — o Apache FOP monta o
    // PDF inteiro em memória antes de haver qualquer byte pronto, então não há como
    // transmitir em stream nesse caminho sem reescrever a geração do FO.
    public StreamingResponseBody streamPdf(Long documentoId) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        if (STATUS_COM_PDF_ARMAZENADO.contains(doc.getDocumentoStatus()) && doc.getUrlPdf() != null) {
            InputStream armazenado = imagemService.getObjectStream(doc.getUrlPdf());
            if (armazenado != null) {
                return outputStream -> {
                    try (armazenado) {
                        armazenado.transferTo(outputStream);
                    }
                };
            }
            // urlPdf presente mas não recuperável (objeto removido/inconsistência): recai
            // na renderização ao vivo em vez de falhar a exportação.
        }
        byte[] renderizado = renderPdf(doc);
        return outputStream -> outputStream.write(renderizado);
    }

    // readOnly=true é essencial aqui, não só um detalhe de estilo: sem ele, o Hibernate
    // faz auto-flush antes de cada query emitida durante a travessia recursiva da
    // árvore de itens normativos (getItensNormativosByDocumento), e como
    // carregarChildrenRecursivamente substitui a coleção `children` (orphanRemoval=true)
    // gerenciada pelo Hibernate por uma List avulsa a cada nível, esse auto-flush no
    // meio da travessia lança "A collection with orphan deletion was no longer
    // referenced" para documentos com mais de um nível de aninhamento. readOnly=true
    // desativa o auto-flush (FlushMode.MANUAL) nesta transação somente-leitura.
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public String gerarEArmazenarPdf(Documento documento) {
        try {
            byte[] pdfBytes = renderPdf(documento);
            String filename = "documento-" + documento.getId() + "-" + Instant.now().toEpochMilli() + ".pdf";
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

        List<AnexoResponseDto> anexos = anexoRepository.findByDocumentoIdOrderByOrdemAsc(id)
                .stream().map(AnexoResponseDto::from).toList();

        String fo = documentoFoBuilder.buildFo(doc, preliminares, normativos, anexos);

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
