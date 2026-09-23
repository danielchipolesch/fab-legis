package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto;
import intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import intraer.fablegis.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.VersaoDocumentoEnum;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.regras.RegrasDasEspecies;
import intraer.fablegis.domain.handlers.exceptions.enums.DocumentoException;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
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

    private static final FopFactory FOP_FACTORY = FopFactoryProvider.get();

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoParteNormativaService documentoParteNormativaService;

    @Autowired
    private RegrasDasEspecies regras;

    @Autowired
    private ImagemService imagemService;

    @Autowired
    private AnexoRepository anexoRepository;

    @Autowired
    private LimitadorGeracaoPdf limitador;

    // A geração armazenada (depois de aprovar/publicar/revogar) não tem ninguém esperando o
    // resultado: pode aguardar bem mais por uma vaga que um pedido de tela.
    private static final java.time.Duration ESPERA_GERACAO_ARMAZENADA = java.time.Duration.ofMinutes(5);

    // Cópia armazenada: transmite os bytes do MinIO direto para a resposta HTTP à
    // medida que chegam (StreamingResponseBody), sem materializar o PDF inteiro em
    // memória no backend — o antigo getObjectBytes() lia tudo com readAllBytes()
    // antes de responder, dobrando a latência (espera MinIO->backend, só então
    // começa backend->navegador) e retendo o arquivo inteiro no heap por requisição.
    // Renderização ao vivo (fallback): permanece como estava — o Apache FOP monta o
    // PDF inteiro em memória antes de haver qualquer byte pronto, então não há como
    // transmitir em stream nesse caminho sem reescrever a geração do FO.
    public StreamingResponseBody streamPdf(Long documentoId, VersaoDocumentoEnum pedida) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        // Vigente: a cópia gravada ao registrar portaria/BCA. Em tramitação: a cópia congelada
        // (só EM_PUBLICACAO/EM_REVOGACAO -- ver VersoesDocumento); nas demais etapas o texto
        // ainda muda, então é gerada na hora.
        VersaoDocumentoEnum versao = VersoesDocumento.resolver(doc, pedida);
        String urlArmazenada = versao == VersaoDocumentoEnum.VIGENTE ? doc.getUrlPdf()
                : (VersoesDocumento.emTramitacaoArmazenada(doc) ? doc.getUrlPdfTramitacao() : null);

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
        // Renderiza ANTES de devolver o corpo: assim, com todas as vagas ocupadas, ainda dá para
        // responder 503 (depois de iniciado o streaming os cabeçalhos já saíram).
        byte[] renderizado = limitador.executar(() -> renderPdf(doc));
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
            byte[] pdfBytes = limitador.executar(() -> renderPdf(documento), ESPERA_GERACAO_ARMAZENADA);
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

        // O layout do PDF é regra da espécie (atos normativos: Portaria + Capa + Sumário + Corpo).
        String fo = regras.para(doc.getEspecieNormativa()).leiauteDoPdf().gerarFo(doc, preliminares, normativos, anexos);

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
