package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.SecaoItemRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoPortariaPublicacaoEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentoException;
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
import java.util.List;

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
    @Autowired NotificacaoService notificacaoService;
    @Autowired DocumentoParteNormativaService documentoParteNormativaService;
    @Autowired PortariaPublicacaoService portariaPublicacaoService;

    // Atômico de propósito: a mudança de status envolve várias tabelas (documento,
    // respaçamento de nr_ordem, histórico) e não pode ficar parcialmente aplicada se
    // alguma etapa falhar — foi exatamente essa falta de atomicidade que permitiu o
    // status mudar no banco mesmo quando respacarElementOrders lançava exceção.
    @Transactional
    public DocumentoResponseSemAnexoTextualDto changeStatus(Long id, DocumentoStatusRequestDto request) throws RuntimeException {

        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        DocumentoStatusEnum novoStatus = request.status();
        DocumentoStatusEnum current = documento.getDocumentoStatus();

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
            throw new StatusCannotBeUpdatedException(DocumentoException.CANNOT_BE_UPDATED.getMessage());
        }

        // Publicar (primeira publicação a partir de APROVADO ou republicação a partir de
        // ALTERADO) e revogar exigem portaria e BCA de referência -- cada uma vira um
        // registro próprio em PortariaPublicacao (ver abaixo), nunca mesclada com o PDF
        // do documento (mesclar invalidaria uma eventual assinatura digital futura na
        // portaria). A aprovação em si (MINUTA -> APROVADO, EM_ALTERACAO -> ALTERADO) não
        // exige nada além da confirmação de status.
        boolean requerPortaria = novoStatus == DocumentoStatusEnum.PUBLICADO || novoStatus == DocumentoStatusEnum.REVOGADO;
        if (requerPortaria) {
            String orgaoPortaria = request.orgaoPortaria();
            String setorPortaria = request.setorPortaria();
            String numeroPortaria = request.numeroPortaria();
            LocalDate dataPortaria = request.dataPortaria();
            Integer numeroBca = request.numeroBca();
            LocalDate dataBca = request.dataBca();

            if (orgaoPortaria == null || orgaoPortaria.isBlank()
                    || setorPortaria == null || setorPortaria.isBlank()
                    || numeroPortaria == null || numeroPortaria.isBlank() || dataPortaria == null
                    || numeroBca == null || dataBca == null || isBlank(request.portariaPdfUrl())) {
                throw new StatusCannotBeUpdatedException(
                        "É obrigatório informar a portaria, o BCA de referência e o PDF da portaria.");
            }
            // A parte preliminar (epígrafe/ementa/preâmbulo/fecho/assinatura) só passa a
            // existir de fato com a publicação -- por isso é coletada aqui, não durante a
            // edição (ver Documento.java). Revogar não republica o conteúdo do documento,
            // então não exige esses campos.
            boolean publicando = novoStatus == DocumentoStatusEnum.PUBLICADO;
            if (publicando && (isBlank(request.epigrafe()) || isBlank(request.ementa()) || isBlank(request.preambulo())
                    || isBlank(request.fecho()) || isBlank(request.assinatura()))) {
                throw new StatusCannotBeUpdatedException(
                        "Para publicar um documento é obrigatório informar epígrafe, ementa, preâmbulo, "
                        + "fecho e assinatura.");
            }
            // O BCA é publicado apenas em dias úteis, então nunca passa de 366 (dias do ano).
            if (numeroBca < 1 || numeroBca > 366) {
                throw new StatusCannotBeUpdatedException(
                        "O número do BCA deve estar entre 1 e 366.");
            }
            if (documento.getDtPortariaReferencia() != null
                    && dataPortaria.isBefore(documento.getDtPortariaReferencia().toLocalDateTime().toLocalDate())) {
                throw new StatusCannotBeUpdatedException(
                        "A data da portaria não pode ser anterior à da alteração anterior.");
            }
            if (documento.getDtBcaReferencia() != null
                    && dataBca.isBefore(documento.getDtBcaReferencia().toLocalDateTime().toLocalDate())) {
                throw new StatusCannotBeUpdatedException(
                        "A data do BCA não pode ser anterior à da alteração anterior.");
            }

            String orgaoSetor = (setorPortaria != null && !setorPortaria.isBlank())
                    ? orgaoPortaria.strip() + "/" + setorPortaria.strip()
                    : orgaoPortaria.strip();
            documento.setPortariaReferencia("Portaria " + orgaoSetor + " n° " + numeroPortaria.strip()
                    + ", de " + formatarDataPorExtenso(dataPortaria));
            documento.setBcaReferencia("BCA n° " + numeroBca + ", de " + formatarDataPorExtenso(dataBca));
            documento.setDtPortariaReferencia(Timestamp.valueOf(dataPortaria.atStartOfDay()));
            documento.setDtBcaReferencia(Timestamp.valueOf(dataBca.atStartOfDay()));

            // Tipo da portaria: revogação é sempre REVOGACAO; publicar a partir de
            // ALTERADO é uma alteração (numerada automaticamente); publicar a partir de
            // APROVADO é a edição original do documento.
            TipoPortariaPublicacaoEnum tipoPortaria = !publicando ? TipoPortariaPublicacaoEnum.REVOGACAO
                    : (current == DocumentoStatusEnum.ALTERADO ? TipoPortariaPublicacaoEnum.ALTERACAO : TipoPortariaPublicacaoEnum.EDICAO);
            portariaPublicacaoService.registrar(documento, tipoPortaria, orgaoPortaria, setorPortaria,
                    numeroPortaria, dataPortaria, numeroBca, dataBca, request.portariaPdfUrl());

            if (publicando) {
                // Substitui a parte preliminar do documento pelo conteúdo informado
                // nesta publicação (mesma lógica de "apaga tudo e recria" já usada
                // por DocumentoParteNormativaService.salvarSecoes durante a edição,
                // só que agora só roda aqui).
                documentoParteNormativaService.salvarItensPreliminares(documento, List.of(
                        new SecaoItemRequestDto(SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.EPIGRAFE, 1, null, request.epigrafe(), null, null),
                        new SecaoItemRequestDto(SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.EMENTA, 2, null, request.ementa(), null, null),
                        new SecaoItemRequestDto(SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.PREAMBULO, 3, null, request.preambulo(), null, null),
                        new SecaoItemRequestDto(SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.FECHO, 4, null, request.fecho(), null, null),
                        new SecaoItemRequestDto(SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.ASSINATURA, 5, null, request.assinatura(), null, null)
                ));

                // Só há emendas pendentes a consolidar quando vem de ALTERADO (ciclo de
                // alteração concluído); a primeira publicação (a partir de APROVADO) nunca
                // passou por EM_ALTERACAO, então não há nada para consolidar.
                if (current == DocumentoStatusEnum.ALTERADO) {
                    emendaService.consolidarPublicacao(id, documento.getPortariaReferencia(), documento.getBcaReferencia());
                }
            }
        }

        Timestamp agora = Timestamp.from(Instant.now());
        switch (novoStatus) {
            case MINUTA       -> documento.setDtMinuta(agora);
            case APROVADO    -> documento.setDtAprovacao(agora);
            case ALTERADO    -> documento.setDtAlterado(agora);
            case PUBLICADO    -> documento.setDtPublicacao(agora);
            case EM_ALTERACAO -> documento.setDtEmAlteracao(agora);
            case ARQUIVADO    -> documento.setDtArquivamento(agora);
            case REVOGADO     -> documento.setDtRevogacao(agora);
            case CANCELADO    -> documento.setDtCancelamento(agora);
            default           -> { }
        }

        documento.setDocumentoStatus(novoStatus);
        // saveAndFlush, não save: o @Version só incrementa no flush, que por
        // padrão só aconteceria no commit -- depois deste método já ter
        // retornado o DTO. Sem o flush explícito, o DTO de resposta carrega a
        // versão ANTIGA, e o próximo salvamento do editor usa essa versão
        // desatualizada como versaoEsperada, gerando um 409 mesmo sendo o
        // mesmo usuário -- ver DocumentoConcorrenciaService. Isso é
        // especialmente comum aqui: RASCUNHO->MINUTA dispara em toda primeira
        // edição de um documento novo (ver editor.js save()).
        documentoRepository.saveAndFlush(documento);

        // Ao entrar em EM_ALTERACAO, espaça os elementOrder (×100) para que novos
        // elementos incluídos por emenda possam ser inseridos em posições intermediárias.
        if (novoStatus == DocumentoStatusEnum.EM_ALTERACAO) {
            normativaRepository.respacarElementOrders(id);
            preliminarRepository.respacarElementOrders(id);
            finalRepository.respacarElementOrders(id);
        }

        // O PDF é gerado e salvo no MinIO nestas transições, e só nelas: exportações
        // subsequentes (independente da tela/botão) sempre servem essa cópia em vez de
        // renderizar de novo — ver DocumentoPdfService.streamPdf. PUBLICADO precisa
        // regenerar mesmo que ALTERADO já tenha uma cópia, pois é só na publicação que
        // portaria/BCA reais substituem o placeholder e consolidarPublicacao (acima)
        // congela as cláusulas de emenda — o conteúdo efetivamente muda.
        if (novoStatus == DocumentoStatusEnum.APROVADO
                || novoStatus == DocumentoStatusEnum.ALTERADO
                || novoStatus == DocumentoStatusEnum.PUBLICADO) {
            try {
                String urlPdf = documentoPdfService.gerarEArmazenarPdf(documento);
                documento.setUrlPdf(urlPdf);
                documentoRepository.saveAndFlush(documento);
            } catch (Exception e) {
                // Não-fatal: a mudança de status não pode falhar por causa do PDF —
                // streamPdf cai de volta para renderização ao vivo quando urlPdf
                // está ausente. Mas o erro precisa ficar visível, senão a causa de um
                // PDF armazenado desatualizado/ausente é impossível de diagnosticar.
                log.error("Falha ao gerar/armazenar PDF do documento {} na transição para {}",
                        documento.getId(), novoStatus, e);
            }
        }

        documentoHistoricoService.registrar(documento, TipoAlteracaoEnum.ALTERACAO_STATUS,
                current.name() + " → " + novoStatus.name(), current, novoStatus);

        // MINUTA e EM_ALTERACAO são as únicas situações que aguardam uma ação de
        // aprovador (ver DocumentoAcessoService.podeAprovarNaOm/STATUS_REQUER_APROVADOR)
        // -- as demais transições são só do próprio redator/autor, ninguém mais precisa
        // ser avisado.
        if (novoStatus == DocumentoStatusEnum.MINUTA || novoStatus == DocumentoStatusEnum.EM_ALTERACAO) {
            String descricao = String.format("%s %s-%d",
                    documento.getEspecieNormativa().getSigla(),
                    documento.getAssuntoBasico().getCodigo(),
                    documento.getNumeroSecundario());
            String acao = novoStatus == DocumentoStatusEnum.MINUTA ? "aguarda aprovação" : "aguarda aprovação da alteração";
            notificacaoService.notificarAprovadoresPendencia(documento.getOm().getId(), documento.getId(), descricao,
                    "O documento " + descricao + " " + acao + ".");
        }

        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(documento);
    }

    private static final String[] MESES = {
            "janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    private static String formatarDataPorExtenso(LocalDate data) {
        return data.getDayOfMonth() + " de " + MESES[data.getMonthValue() - 1] + " de " + data.getYear();
    }

    private static boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
