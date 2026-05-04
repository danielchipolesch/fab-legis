package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentException;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentoStatusService {

    @Autowired
    DocumentoRepository documentoRepository;

    public DocumentoResponseSemAnexoTextualDto approveDocument(Long id) throws RuntimeException {

        Documento document = documentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));
        if (document.getDocumentoStatus().equals(DocumentoStatusEnum.RASCUNHO) || document.getDocumentoStatus().equals(DocumentoStatusEnum.MINUTA)) {
            document.setDocumentoStatus(DocumentoStatusEnum.APROVADO);
            documentoRepository.save(document);
            return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(document);
        } else if (document.getDocumentoStatus().equals(DocumentoStatusEnum.APROVADO)) {
            throw new StatusCannotBeUpdatedException(DocumentException.APROVADO.getMessage());
        }
        throw new StatusCannotBeUpdatedException(DocumentException.CANNOT_BE_UPDATED.getMessage());
    }

    /* TODO Must review statuses and its usability. */

    public DocumentoResponseSemAnexoTextualDto publishDocument(){
        /* TODO Insert logic to publish documents (PUBLICADO). To choose this status, current status must be APROVADO. */
        return null;
    }

    public DocumentoResponseSemAnexoTextualDto archiveDocument(){
        /* TODO Insert logic to archive documents (ARQUIVADO). To choose this status, current status must be XYZ. */
        return null;
    }

    public DocumentoResponseSemAnexoTextualDto cancelDocument(){
        /* TODO Insert logic to cancel documents (CANCELADO). To choose this status, current status must be ZYX. */
        return null;
    }

    public DocumentoResponseSemAnexoTextualDto revokeDocument(){
        /* TODO Insert logic to revoke documents (REVOGAR). To choose this status, current status must be AAAAA. */
        return null;
    }
}
