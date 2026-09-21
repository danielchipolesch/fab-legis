package intraer.fablegis.domain.services;

import intraer.fablegis.domain.regras.ElementoNumeracao;
import intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto;
import intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import intraer.fablegis.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.VersaoDocumentoEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.regras.RegrasDasEspecies;
import intraer.fablegis.domain.handlers.exceptions.enums.DocumentoException;
import intraer.fablegis.domain.util.tiptap.TipTapHtmlSerializer;
import intraer.fablegis.domain.util.tiptap.TipTapNode;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
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
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoParteNormativaService documentoParteNormativaService;

    @Autowired
    private AnexoRepository anexoRepository;

    @Autowired
    private ImagemService imagemService;

    // O layout do HTML é regra da espécie do documento (ver RegrasDasEspecies).
    @Autowired
    private RegrasDasEspecies regras;

    public String gerarHtml(
            Documento doc,
            List<ItemPartePreliminarResponseDto> preliminares,
            List<ItemAnexoParteNormativaResponseDto> normativos,
            List<AnexoResponseDto> anexos) {
        return regras.para(doc.getEspecieNormativa()).leiauteDoHtml().gerarHtml(doc, preliminares, normativos, anexos);
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

}
