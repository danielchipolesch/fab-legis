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

    public DocumentoResponseSemAnexoTextualDto changeStatus(Long id, DocumentoStatusEnum novoStatus) throws RuntimeException {

        Documento document = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));

        DocumentoStatusEnum current = document.getDocumentoStatus();

        boolean transicaoValida = switch (novoStatus) {
            case MINUTA    -> current == DocumentoStatusEnum.RASCUNHO || current == DocumentoStatusEnum.APROVADO;
            case APROVADO  -> current == DocumentoStatusEnum.RASCUNHO || current == DocumentoStatusEnum.MINUTA;
            case PUBLICADO -> current == DocumentoStatusEnum.APROVADO;
            case ARQUIVADO -> current == DocumentoStatusEnum.PUBLICADO;
            case REVOGADO  -> current == DocumentoStatusEnum.PUBLICADO;
            case CANCELADO -> current == DocumentoStatusEnum.RASCUNHO || current == DocumentoStatusEnum.MINUTA;
            default        -> false;
        };

        if (!transicaoValida) {
            throw new StatusCannotBeUpdatedException(DocumentException.CANNOT_BE_UPDATED.getMessage());
        }

        document.setDocumentoStatus(novoStatus);
        documentoRepository.save(document);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(document);
    }
}
