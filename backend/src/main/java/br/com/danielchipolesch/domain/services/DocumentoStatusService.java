package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentException;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class DocumentoStatusService {

    @Autowired
    DocumentoRepository documentoRepository;

    @Autowired
    DocumentoHistoricoService documentoHistoricoService;

    @Autowired
    DocumentoPdfService documentoPdfService;

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

        Timestamp agora = Timestamp.from(Instant.now());
        switch (novoStatus) {
            case MINUTA    -> document.setDtMinuta(agora);
            case APROVADO  -> document.setDtAprovacao(agora);
            case PUBLICADO -> document.setDtPublicacao(agora);
            case ARQUIVADO -> document.setDtArquivamento(agora);
            case REVOGADO  -> document.setDtRevogacao(agora);
            case CANCELADO -> document.setDtCancelamento(agora);
            default        -> { }
        }

        document.setDocumentoStatus(novoStatus);
        documentoRepository.save(document);

        if (novoStatus == DocumentoStatusEnum.APROVADO) {
            try {
                String urlPdf = documentoPdfService.gerarEArmazenarPdf(document);
                document.setUrlPdf(urlPdf);
                documentoRepository.save(document);
            } catch (Exception e) {
                // PDF generation failure is non-fatal — status change is already committed
            }
        }

        documentoHistoricoService.registrar(document, TipoAlteracaoEnum.ALTERACAO_STATUS,
                current.name() + " → " + novoStatus.name(), current, novoStatus);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(document);
    }
}
