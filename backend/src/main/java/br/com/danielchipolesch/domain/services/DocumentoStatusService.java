package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentException;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemParteFinalRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemPartePreliminarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class DocumentoStatusService {

    @Autowired DocumentoRepository documentoRepository;
    @Autowired DocumentoHistoricoService documentoHistoricoService;
    @Autowired DocumentoPdfService documentoPdfService;
    @Autowired ItemAnexoParteNormativaRepository normativaRepository;
    @Autowired ItemPartePreliminarRepository preliminarRepository;
    @Autowired ItemParteFinalRepository finalRepository;

    public DocumentoResponseSemAnexoTextualDto changeStatus(Long id, DocumentoStatusRequestDto request) throws RuntimeException {

        Documento document = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));

        DocumentoStatusEnum novoStatus = request.getStatus();
        DocumentoStatusEnum current = document.getDocumentoStatus();

        boolean transicaoValida = switch (novoStatus) {
            case MINUTA       -> current == DocumentoStatusEnum.RASCUNHO || current == DocumentoStatusEnum.APROVADO;
            case APROVADO     -> current == DocumentoStatusEnum.RASCUNHO
                             || current == DocumentoStatusEnum.MINUTA
                             || current == DocumentoStatusEnum.EM_ALTERACAO;
            case PUBLICADO    -> current == DocumentoStatusEnum.APROVADO;
            case EM_ALTERACAO -> current == DocumentoStatusEnum.PUBLICADO;
            case ARQUIVADO    -> current == DocumentoStatusEnum.PUBLICADO;
            case REVOGADO     -> current == DocumentoStatusEnum.PUBLICADO;
            case CANCELADO    -> current == DocumentoStatusEnum.RASCUNHO || current == DocumentoStatusEnum.MINUTA;
            default           -> false;
        };

        if (!transicaoValida) {
            throw new StatusCannotBeUpdatedException(DocumentException.CANNOT_BE_UPDATED.getMessage());
        }

        // Ao aprovar após emenda, portaria e BCA são obrigatórios (o PDF os embute)
        if (novoStatus == DocumentoStatusEnum.APROVADO && current == DocumentoStatusEnum.EM_ALTERACAO) {
            String portaria = request.getPortariaReferencia();
            String bca = request.getBcaReferencia();
            if (portaria == null || portaria.isBlank() || bca == null || bca.isBlank()) {
                throw new StatusCannotBeUpdatedException(
                        "Para aprovar um documento em alteração é obrigatório informar a portaria e o BCA de referência.");
            }
            document.setPortariaReferencia(portaria.strip());
            document.setBcaReferencia(bca.strip());
        }

        Timestamp agora = Timestamp.from(Instant.now());
        switch (novoStatus) {
            case MINUTA       -> document.setDtMinuta(agora);
            case APROVADO     -> document.setDtAprovacao(agora);
            case PUBLICADO    -> document.setDtPublicacao(agora);
            case EM_ALTERACAO -> document.setDtEmAlteracao(agora);
            case ARQUIVADO    -> document.setDtArquivamento(agora);
            case REVOGADO     -> document.setDtRevogacao(agora);
            case CANCELADO    -> document.setDtCancelamento(agora);
            default           -> { }
        }

        document.setDocumentoStatus(novoStatus);
        documentoRepository.save(document);

        // Ao entrar em EM_ALTERACAO, espaça os elementOrder (×100) para que novos
        // elementos incluídos por emenda possam ser inseridos em posições intermediárias.
        if (novoStatus == DocumentoStatusEnum.EM_ALTERACAO) {
            normativaRepository.respacarElementOrders(id);
            preliminarRepository.respacarElementOrders(id);
            finalRepository.respacarElementOrders(id);
        }

        if (novoStatus == DocumentoStatusEnum.APROVADO) {
            try {
                String urlPdf = documentoPdfService.gerarEArmazenarPdf(document);
                document.setUrlPdf(urlPdf);
                documentoRepository.save(document);
            } catch (Exception e) {
                // PDF generation failure is non-fatal
            }
        }

        documentoHistoricoService.registrar(document, TipoAlteracaoEnum.ALTERACAO_STATUS,
                current.name() + " → " + novoStatus.name(), current, novoStatus);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(document);
    }
}
