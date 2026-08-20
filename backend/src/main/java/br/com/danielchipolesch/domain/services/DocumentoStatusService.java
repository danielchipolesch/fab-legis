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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class DocumentoStatusService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoStatusService.class);

    @Autowired DocumentoRepository documentoRepository;
    @Autowired DocumentoHistoricoService documentoHistoricoService;
    @Autowired DocumentoPdfService documentoPdfService;
    @Autowired ItemAnexoParteNormativaRepository normativaRepository;
    @Autowired ItemPartePreliminarRepository preliminarRepository;
    @Autowired ItemParteFinalRepository finalRepository;
    @Autowired EmendaService emendaService;

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

        // Toda publicação (primeira publicação a partir de APROVADO ou republicação a
        // partir de ALTERADO) exige portaria e BCA de referência — o PDF os embute. A
        // aprovação em si (MINUTA -> APROVADO, EM_ALTERACAO -> ALTERADO) não exige nada
        // além da confirmação de status.
        boolean publicacao = novoStatus == DocumentoStatusEnum.PUBLICADO;
        if (publicacao) {
            String orgaoPortaria = request.getOrgaoPortaria();
            String setorPortaria = request.getSetorPortaria();
            String numeroPortaria = request.getNumeroPortaria();
            LocalDate dataPortaria = request.getDataPortaria();
            Integer numeroBca = request.getNumeroBca();
            LocalDate dataBca = request.getDataBca();

            if (orgaoPortaria == null || orgaoPortaria.isBlank()
                    || setorPortaria == null || setorPortaria.isBlank()
                    || numeroPortaria == null || numeroPortaria.isBlank() || dataPortaria == null
                    || numeroBca == null || dataBca == null) {
                throw new StatusCannotBeUpdatedException(
                        "Para publicar um documento é obrigatório informar a portaria e o BCA de referência.");
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

            String orgaoSetor = (setorPortaria != null && !setorPortaria.isBlank())
                    ? orgaoPortaria.strip() + "/" + setorPortaria.strip()
                    : orgaoPortaria.strip();
            document.setPortariaReferencia("Portaria " + orgaoSetor + " n° " + numeroPortaria.strip()
                    + ", de " + formatarDataPorExtenso(dataPortaria));
            document.setBcaReferencia("BCA n° " + numeroBca + ", de " + formatarDataPorExtenso(dataBca));
            document.setDtPortariaReferencia(Timestamp.valueOf(dataPortaria.atStartOfDay()));
            document.setDtBcaReferencia(Timestamp.valueOf(dataBca.atStartOfDay()));

            // Só há emendas pendentes a consolidar quando vem de ALTERADO (ciclo de
            // alteração concluído); a primeira publicação (a partir de APROVADO) nunca
            // passou por EM_ALTERACAO, então não há nada para consolidar.
            if (current == DocumentoStatusEnum.ALTERADO) {
                emendaService.consolidarPublicacao(id, document.getPortariaReferencia(), document.getBcaReferencia());
            }
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

        // O PDF é gerado e salvo no MinIO nestas transições, e só nelas: exportações
        // subsequentes (independente da tela/botão) sempre servem essa cópia em vez de
        // renderizar de novo — ver DocumentoPdfService.gerarPdfBytes. PUBLICADO precisa
        // regenerar mesmo que ALTERADO já tenha uma cópia, pois é só na publicação que
        // portaria/BCA reais substituem o placeholder e consolidarPublicacao (acima)
        // congela as cláusulas de emenda — o conteúdo efetivamente muda.
        if (novoStatus == DocumentoStatusEnum.APROVADO
                || novoStatus == DocumentoStatusEnum.ALTERADO
                || novoStatus == DocumentoStatusEnum.PUBLICADO) {
            try {
                String urlPdf = documentoPdfService.gerarEArmazenarPdf(document);
                document.setUrlPdf(urlPdf);
                documentoRepository.save(document);
            } catch (Exception e) {
                // Não-fatal: a mudança de status não pode falhar por causa do PDF —
                // gerarPdfBytes cai de volta para renderização ao vivo quando urlPdf
                // está ausente. Mas o erro precisa ficar visível, senão a causa de um
                // PDF armazenado desatualizado/ausente é impossível de diagnosticar.
                log.error("Falha ao gerar/armazenar PDF do documento {} na transição para {}",
                        document.getId(), novoStatus, e);
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
