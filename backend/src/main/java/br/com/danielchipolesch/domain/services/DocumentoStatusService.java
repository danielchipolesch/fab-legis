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
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class DocumentoStatusService {

    @Autowired DocumentoRepository documentoRepository;
    @Autowired DocumentoHistoricoService documentoHistoricoService;
    @Autowired DocumentoPdfService documentoPdfService;
    @Autowired ItemAnexoParteNormativaRepository normativaRepository;
    @Autowired ItemPartePreliminarRepository preliminarRepository;
    @Autowired ItemParteFinalRepository finalRepository;

    // Atômico de propósito: a mudança de status envolve várias tabelas (documento,
    // respaçamento de nr_ordem, histórico) e não pode ficar parcialmente aplicada se
    // alguma etapa falhar — foi exatamente essa falta de atomicidade que permitiu o
    // status mudar no banco mesmo quando respacarElementOrders lançava exceção.
    @Transactional
    public DocumentoResponseSemAnexoTextualDto changeStatus(Long id, DocumentoStatusRequestDto request) throws RuntimeException {

        Documento document = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));

        DocumentoStatusEnum novoStatus = request.getStatus();
        DocumentoStatusEnum current = document.getDocumentoStatus();

        boolean transicaoValida = switch (novoStatus) {
            // MINUTA nunca é alcançável a partir de ALTERADO: um documento que passou por
            // EM_ALTERACAO carrega numeração com sufixo de letra e elementos marcados
            // (INCLUIDO/ALTERADO/REVOGADO) que a renumeração simples de MINUTA não entende
            // — só é permitido a partir do fluxo normal (RASCUNHO/APROVADO).
            case MINUTA       -> current == DocumentoStatusEnum.RASCUNHO || current == DocumentoStatusEnum.APROVADO;
            case APROVADO     -> current == DocumentoStatusEnum.RASCUNHO || current == DocumentoStatusEnum.MINUTA;
            case ALTERADO     -> current == DocumentoStatusEnum.EM_ALTERACAO;
            case PUBLICADO    -> current == DocumentoStatusEnum.APROVADO || current == DocumentoStatusEnum.ALTERADO;
            case EM_ALTERACAO -> current == DocumentoStatusEnum.PUBLICADO || current == DocumentoStatusEnum.ALTERADO;
            case ARQUIVADO    -> current == DocumentoStatusEnum.PUBLICADO;
            case REVOGADO     -> current == DocumentoStatusEnum.PUBLICADO;
            case CANCELADO    -> current == DocumentoStatusEnum.RASCUNHO || current == DocumentoStatusEnum.MINUTA;
            default           -> false;
        };

        if (!transicaoValida) {
            throw new StatusCannotBeUpdatedException(DocumentException.CANNOT_BE_UPDATED.getMessage());
        }

        // Ao republicar um documento que passou por alteração (PUBLICADO vindo de
        // ALTERADO), portaria e BCA de referência são obrigatórios — o PDF os embute.
        // A aprovação da alteração em si (EM_ALTERACAO -> ALTERADO) não exige nada além
        // da confirmação de status.
        boolean republicacaoPosAlteracao = novoStatus == DocumentoStatusEnum.PUBLICADO
                && current == DocumentoStatusEnum.ALTERADO;
        if (republicacaoPosAlteracao) {
            String numeroPortaria = request.getNumeroPortaria();
            LocalDate dataPortaria = request.getDataPortaria();
            Integer numeroBca = request.getNumeroBca();
            LocalDate dataBca = request.getDataBca();

            if (numeroPortaria == null || numeroPortaria.isBlank() || dataPortaria == null
                    || numeroBca == null || dataBca == null) {
                throw new StatusCannotBeUpdatedException(
                        "Para republicar um documento alterado é obrigatório informar a portaria e o BCA de referência.");
            }
            // O BCA é publicado apenas em dias úteis, então nunca passa de 366 (dias do ano).
            if (numeroBca < 1 || numeroBca > 366) {
                throw new StatusCannotBeUpdatedException(
                        "O número do BCA deve estar entre 1 e 366.");
            }
            if (document.getDtPortariaReferencia() != null
                    && dataPortaria.isBefore(document.getDtPortariaReferencia().toLocalDateTime().toLocalDate())) {
                throw new StatusCannotBeUpdatedException(
                        "A data da portaria não pode ser anterior à da alteração anterior.");
            }
            if (document.getDtBcaReferencia() != null
                    && dataBca.isBefore(document.getDtBcaReferencia().toLocalDateTime().toLocalDate())) {
                throw new StatusCannotBeUpdatedException(
                        "A data do BCA não pode ser anterior à da alteração anterior.");
            }

            document.setPortariaReferencia("Portaria " + numeroPortaria.strip() + ", de " + formatarDataPorExtenso(dataPortaria));
            document.setBcaReferencia("BCA nº " + numeroBca + ", de " + formatarDataPorExtenso(dataBca));
            document.setDtPortariaReferencia(Timestamp.valueOf(dataPortaria.atStartOfDay()));
            document.setDtBcaReferencia(Timestamp.valueOf(dataBca.atStartOfDay()));
        }

        Timestamp agora = Timestamp.from(Instant.now());
        switch (novoStatus) {
            case MINUTA       -> document.setDtMinuta(agora);
            case APROVADO    -> document.setDtAprovacao(agora);
            case ALTERADO    -> document.setDtAlterado(agora);
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

        if (novoStatus == DocumentoStatusEnum.APROVADO || novoStatus == DocumentoStatusEnum.ALTERADO) {
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

    private static final String[] MESES = {
            "janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    private static String formatarDataPorExtenso(LocalDate data) {
        return data.getDayOfMonth() + " de " + MESES[data.getMonthValue() - 1] + " de " + data.getYear();
    }
}
