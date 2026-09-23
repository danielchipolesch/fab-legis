package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto;
import intraer.fablegis.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Anexo;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.handlers.exceptions.enums.DocumentoException;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class AnexoService {

    @Autowired
    private AnexoRepository anexoRepository;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private ImagemService imagemService;

    @Autowired
    private DocumentoStatusService documentoStatusService;

    public List<AnexoResponseDto> listar(Long documentoId) {
        documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));
        return anexoRepository.findByDocumentoIdOrderByOrdemAsc(documentoId)
                .stream().map(AnexoResponseDto::from).toList();
    }

    @Transactional
    public AnexoResponseDto adicionar(Long documentoId, String titulo, MultipartFile arquivo) throws Exception {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        String url = imagemService.uploadImagem(arquivo);

        int proximaOrdem = anexoRepository.findMaxOrdemByDocumentoId(documentoId) + 1;

        Anexo anexo = new Anexo();
        anexo.setDocumento(documento);
        anexo.setTitulo(titulo);
        anexo.setUrlImagem(url);
        anexo.setOrdem(proximaOrdem);

        AnexoResponseDto resultado = AnexoResponseDto.from(anexoRepository.save(anexo));

        if (documento.getSituacaoLocal() == SituacaoLocalEnum.RASCUNHO) {
            documentoStatusService.changeStatus(documentoId, new DocumentoStatusRequestDto(
                    SituacaoLocalEnum.MINUTA, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null));
        }

        return resultado;
    }

    @Transactional
    public void remover(Long documentoId, Long anexoId) {
        Anexo anexo = anexoRepository.findByIdAndDocumentoId(anexoId, documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado."));
        anexoRepository.delete(anexo);

        // Reordena os anexos restantes
        List<Anexo> restantes = anexoRepository.findByDocumentoIdOrderByOrdemAsc(documentoId);
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).setOrdem(i + 1);
            anexoRepository.save(restantes.get(i));
        }
    }
}
