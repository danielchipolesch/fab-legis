package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.SecaoItemRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoPortariaPublicacaoEnum;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentoException;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemParteFinalRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemPartePreliminarRepository;
import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;

// Máquina de etapas do documento (situação LOCAL) e a única que muda a situação BCA.
// A situação BCA é a REAL (espelha o repositório oficial) e só muda ao registrar portaria +
// BCA: EDICAO -> PUBLICADO, REVOGACAO -> REVOGADO; uma ALTERACAO mantém PUBLICADO. Toda
// etapa interna (revisão, alteração em curso, análise de revogação...) é situação local e
// nunca tira o documento de PUBLICADO. Ver docs/ciclo-de-vida.md e SituacaoLocalEnum.
@Service
public class DocumentoStatusService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoStatusService.class);

    @Autowired DocumentoRepository documentoRepository;
    @Autowired DocumentoHistoricoService documentoHistoricoService;
    @Autowired DocumentoPdfService documentoPdfService;
    @Autowired DocumentoHtmlService documentoHtmlService;
    @Autowired ItemAnexoParteNormativaRepository normativaRepository;
    @Autowired ItemPartePreliminarRepository preliminarRepository;
    @Autowired ItemParteFinalRepository finalRepository;
    @Autowired EmendaService emendaService;
    @Autowired NotificacaoService notificacaoService;
    @Autowired DocumentoParteNormativaService documentoParteNormativaService;
    @Autowired PortariaPublicacaoService portariaPublicacaoService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PlatformTransactionManager transactionManager;

    // O que cada pedido de mudança de etapa significa em termos de negócio -- o mesmo
    // destino (ex.: SEM_ETAPA) quer dizer coisas diferentes conforme a origem.
    private enum Acao {
        MINUTAR,              // RASCUNHO -> MINUTA
        ENVIAR_PARA_REVISAO,  // MINUTA/EM_ALTERACAO -> EM_REVISAO (escolhe o revisor)
        APROVAR,              // EM_REVISAO -> EM_PUBLICACAO (escolhe o publicador)
        DEVOLVER,             // EM_REVISAO/EM_PUBLICACAO -> MINUTA ou EM_ALTERACAO
        PUBLICAR,             // EM_PUBLICACAO -> SEM_ETAPA (registra portaria/BCA)
        INICIAR_ALTERACAO,    // SEM_ETAPA -> EM_ALTERACAO (documento PUBLICADO)
        CANCELAR_ALTERACAO,   // EM_ALTERACAO -> SEM_ETAPA (só sem alterações pendentes)
        PEDIR_REVOGACAO,      // SEM_ETAPA -> ANALISE_REVOGACAO (documento PUBLICADO)
        APROVAR_REVOGACAO,    // ANALISE_REVOGACAO -> EM_REVOGACAO (escolhe o publicador)
        DEVOLVER_ANALISE,     // ANALISE_REVOGACAO -> SEM_ETAPA (o documento segue PUBLICADO)
        REVOGAR,              // EM_REVOGACAO -> SEM_ETAPA (registra portaria/BCA; BCA = REVOGADO)
        CANCELAR_DOCUMENTO    // RASCUNHO/MINUTA -> CANCELADO
    }

    // Devolver leva ao começo do trabalho: um documento já PUBLICADO volta para a alteração
    // que estava fazendo; um nunca publicado, para a minuta.
    private static SituacaoLocalEnum destinoDeDevolucao(SituacaoBcaEnum bca) {
        return bca == SituacaoBcaEnum.PUBLICADO ? EM_ALTERACAO : MINUTA;
    }

    // Tabela de transições (docs/ciclo-de-vida.md). Só existe UMA etapa local por vez: por
    // isso não há saída de EM_ALTERACAO para ANALISE_REVOGACAO -- para revogar, primeiro
    // conclui-se ou cancela-se a alteração. EM_REVOGACAO só sai para REVOGADO.
    private static Acao acaoPara(SituacaoLocalEnum atual, SituacaoLocalEnum destino, SituacaoBcaEnum bca) {
        return switch (atual) {
            case RASCUNHO          -> destino == MINUTA ? Acao.MINUTAR
                                    : destino == CANCELADO ? Acao.CANCELAR_DOCUMENTO : null;
            case MINUTA            -> destino == EM_REVISAO ? Acao.ENVIAR_PARA_REVISAO
                                    : destino == CANCELADO ? Acao.CANCELAR_DOCUMENTO : null;
            case EM_REVISAO        -> destino == EM_PUBLICACAO ? Acao.APROVAR
                                    : destino == destinoDeDevolucao(bca) ? Acao.DEVOLVER : null;
            case EM_PUBLICACAO     -> destino == SEM_ETAPA ? Acao.PUBLICAR
                                    : destino == destinoDeDevolucao(bca) ? Acao.DEVOLVER : null;
            case EM_ALTERACAO      -> destino == EM_REVISAO ? Acao.ENVIAR_PARA_REVISAO
                                    : destino == SEM_ETAPA ? Acao.CANCELAR_ALTERACAO : null;
            case ANALISE_REVOGACAO -> destino == EM_REVOGACAO ? Acao.APROVAR_REVOGACAO
                                    : destino == SEM_ETAPA ? Acao.DEVOLVER_ANALISE : null;
            case EM_REVOGACAO      -> destino == SEM_ETAPA ? Acao.REVOGAR : null;
            case SEM_ETAPA         -> bca != SituacaoBcaEnum.PUBLICADO ? null
                                    : destino == EM_ALTERACAO ? Acao.INICIAR_ALTERACAO
                                    : destino == ANALISE_REVOGACAO ? Acao.PEDIR_REVOGACAO : null;
            case CANCELADO         -> null;
        };
    }

    // Atômico de propósito: a mudança de etapa envolve várias tabelas (documento,
    // respaçamento de nr_ordem, portaria, histórico) e não pode ficar parcialmente aplicada
    // se alguma etapa falhar.
    @Transactional
    public DocumentoResponseSemAnexoTextualDto changeStatus(Long id, DocumentoStatusRequestDto request) throws RuntimeException {

        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        SituacaoLocalEnum atual = documento.getSituacaoLocal();
        SituacaoLocalEnum destino = request.situacaoLocal();
        SituacaoBcaEnum bcaAnterior = documento.getSituacaoBca();

        Acao acao = acaoPara(atual, destino, bcaAnterior);
        if (acao == null) {
            throw new StatusCannotBeUpdatedException("Transição não permitida: " + atual + " → " + destino
                    + " (situação BCA: " + bcaAnterior + ").");
        }

        // "Devolver" limpa a atribuição -- a próxima vez que o documento for enviado, alguém
        // (talvez outra pessoa) é escolhido de novo.
        if (acao == Acao.DEVOLVER || acao == Acao.DEVOLVER_ANALISE) {
            documento.setRevisorAtribuido(null);
            documento.setPublicadorAtribuido(null);
        }

        // Enviar para revisão / pedir análise de revogação: exige a pessoa escolhida (papel
        // APROV, validado em DocumentoAcessoService.podeMudarStatus -- aqui só resolve o registro).
        if (acao == Acao.ENVIAR_PARA_REVISAO || acao == Acao.PEDIR_REVOGACAO) {
            documento.setRevisorAtribuido(buscarUsuario(request.revisorId(),
                    "É obrigatório escolher quem vai revisar."));
        }

        // Aprovar (o texto ou a revogação): exige a pessoa escolhida (papel PUBLIC) que vai
        // registrar a portaria/BCA.
        if (acao == Acao.APROVAR) {
            documento.setPublicadorAtribuido(buscarUsuario(request.publicadorId(),
                    "É obrigatório escolher quem vai publicar."));
        }
        if (acao == Acao.APROVAR_REVOGACAO) {
            documento.setPublicadorAtribuido(buscarUsuario(request.publicadorId(),
                    "É obrigatório escolher quem vai publicar a revogação."));
        }

        // Cancelar a alteração só é possível sem alterações pendentes: cada uma se desfaz elemento a
        // elemento (sidebar do editor). Nada é descartado em massa por aqui.
        if (acao == Acao.CANCELAR_ALTERACAO && emendaService.temAlteracoesPendentes(id)) {
            throw new StatusCannotBeUpdatedException("Há alterações pendentes neste documento. Desfaça cada uma "
                    + "no painel lateral do editor (ou envie a alteração para revisão) antes de cancelá-la.");
        }

        // Publicar (EM_PUBLICACAO -> SEM_ETAPA) e revogar (EM_REVOGACAO -> SEM_ETAPA) são os
        // únicos momentos em que uma portaria + BCA são registradas -- e, portanto, os únicos em
        // que a situação BCA muda. Cada portaria vira um registro próprio em PortariaPublicacao,
        // nunca mesclada com o PDF do documento (mesclar invalidaria uma eventual assinatura
        // digital futura na portaria) e nunca substituída: a de publicação é perene e as de
        // alteração e revogação são complementares a ela.
        boolean primeiraPublicacao = acao == Acao.PUBLICAR && bcaAnterior == SituacaoBcaEnum.NAO_PUBLICADO;
        boolean alterando = acao == Acao.PUBLICAR && bcaAnterior == SituacaoBcaEnum.PUBLICADO;
        boolean revogando = acao == Acao.REVOGAR;
        if (acao == Acao.PUBLICAR || revogando) {
            registrarPortariaEBca(documento, request, primeiraPublicacao, alterando, revogando);
        }

        // Situação BCA: a única transição que a altera.
        SituacaoBcaEnum novaBca = switch (acao) {
            case PUBLICAR -> SituacaoBcaEnum.PUBLICADO;
            case REVOGAR  -> SituacaoBcaEnum.REVOGADO;
            default       -> bcaAnterior;
        };
        documento.setSituacaoBca(novaBca);

        Timestamp agora = Timestamp.from(Instant.now());
        switch (acao) {
            case MINUTAR              -> documento.setDtMinuta(agora);
            case ENVIAR_PARA_REVISAO  -> documento.setDtEmRevisao(agora);
            // Timestamp próprio para a aprovação de uma alteração (dtAlterado), para não
            // sobrescrever a aprovação original do fluxo da primeira publicação.
            case APROVAR              -> {
                if (bcaAnterior == SituacaoBcaEnum.NAO_PUBLICADO) documento.setDtAprovacao(agora);
                else documento.setDtAlterado(agora);
                documento.setDtEmPublicacao(agora);
            }
            case DEVOLVER             -> {
                if (destino == MINUTA) documento.setDtMinuta(agora);
                else documento.setDtEmAlteracao(agora);
            }
            case INICIAR_ALTERACAO    -> documento.setDtEmAlteracao(agora);
            // dtPublicacao é a data da PRIMEIRA publicação; as das alterações estão nas portarias
            // (PortariaPublicacao) e nas cláusulas de cada elemento.
            case PUBLICAR             -> { if (primeiraPublicacao) documento.setDtPublicacao(agora); }
            case PEDIR_REVOGACAO      -> documento.setDtAnaliseRevogacao(agora);
            case APROVAR_REVOGACAO    -> documento.setDtEmRevogacao(agora);
            case REVOGAR              -> documento.setDtRevogacao(agora);
            case CANCELAR_DOCUMENTO   -> documento.setDtCancelamento(agora);
            case DEVOLVER_ANALISE, CANCELAR_ALTERACAO -> { }
        }

        documento.setSituacaoLocal(destino);
        // saveAndFlush, não save: o @Version só incrementa no flush, que por padrão só
        // aconteceria no commit -- depois deste método já ter retornado o DTO. Sem o flush
        // explícito, o DTO de resposta carrega a versão ANTIGA, e o próximo salvamento do editor
        // usa essa versão desatualizada como versaoEsperada, gerando um 409 mesmo sendo o mesmo
        // usuário -- ver DocumentoConcorrenciaService. Isso é especialmente comum aqui:
        // RASCUNHO->MINUTA dispara em toda primeira edição de um documento novo (ver editor.js save()).
        documentoRepository.saveAndFlush(documento);

        // Ao entrar em EM_ALTERACAO, espaça os elementOrder (×100) para que novos elementos
        // incluídos por emenda possam ser inseridos em posições intermediárias.
        if (destino == EM_ALTERACAO) {
            normativaRepository.respacarElementOrders(id);
            preliminarRepository.respacarElementOrders(id);
            finalRepository.respacarElementOrders(id);
        }

        // PDF/HTML: o arquivo VIGENTE só é (re)gerado ao registrar portaria/BCA -- é a versão
        // que a situação BCA descreve e não pode mudar por uma etapa interna. A versão EM
        // TRAMITAÇÃO é congelada quando o conteúdo deixa de mudar (aprovar), para o publicador
        // ver exatamente o que será publicado, e descartada ao devolver/cancelar/publicar.
        // Nas etapas em que o texto ainda muda ela é gerada sob demanda, nunca armazenada.
        // Os dois formatos são sempre gerados juntos (ver docs/exportacao-pdf.md).
        switch (acao) {
            case APROVAR, APROVAR_REVOGACAO -> aposCommit(documento, this::gerarVersaoEmTramitacao);
            case PUBLICAR, REVOGAR -> {
                // A versão em tramitação acaba de ser publicada/revogada: descarta já (na transação).
                documento.setUrlPdfTramitacao(null);
                documento.setUrlHtmlTramitacao(null);
                aposCommit(documento, this::gerarVersaoVigente);
            }
            case DEVOLVER, DEVOLVER_ANALISE, CANCELAR_ALTERACAO, CANCELAR_DOCUMENTO -> descartarVersaoEmTramitacao(documento);
            default -> { }
        }

        String descricaoHistorico = atual.name() + " → " + destino.name()
                + (novaBca != bcaAnterior ? " (situação BCA: " + bcaAnterior + " → " + novaBca + ")" : "");
        documentoHistoricoService.registrar(documento, TipoAlteracaoEnum.ALTERACAO_STATUS,
                descricaoHistorico, atual, destino);

        String descricao = String.format("%s %s-%d",
                documento.getEspecieNormativa().getSigla(),
                documento.getAssuntoBasico().getCodigo(),
                documento.getNumeroSecundario());

        // Cada transição de atribuição avisa só a pessoa escolhida -- nunca uma OM inteira
        // (ver PapelEnum: o modelo é de atribuição pessoal, não de "qualquer Aprovador pega").
        if (acao == Acao.ENVIAR_PARA_REVISAO || acao == Acao.PEDIR_REVOGACAO) {
            notificacaoService.notificarAtribuicao(documento.getRevisorAtribuido().getId(), documento.getId(), descricao,
                    "O documento " + descricao + " foi atribuído a você para revisão.");
        }
        if (acao == Acao.APROVAR) {
            notificacaoService.notificarAtribuicao(documento.getPublicadorAtribuido().getId(), documento.getId(), descricao,
                    "O documento " + descricao + " foi atribuído a você para publicação.");
        }
        if (acao == Acao.APROVAR_REVOGACAO) {
            notificacaoService.notificarAtribuicao(documento.getPublicadorAtribuido().getId(), documento.getId(), descricao,
                    "O documento " + descricao + " foi atribuído a você para publicar a revogação.");
        }

        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(documento);
    }

    // Valida e registra a portaria + BCA (publicação ou revogação). A parte preliminar
    // (epígrafe/ementa/preâmbulo/fecho/assinatura) é coletada e gravada SÓ na primeira
    // publicação: a portaria de publicação é a única que aparece nela e nunca é substituída.
    // Alteração e revogação exigem apenas portaria (órgão, setor, número, data), BCA e o PDF, e
    // aparecem por cláusula nos elementos (alteração) ou pelo selo REVOGADO (revogação total).
    private void registrarPortariaEBca(Documento documento, DocumentoStatusRequestDto request,
                                       boolean primeiraPublicacao, boolean alterando, boolean revogando) {
        String orgaoPortaria = request.orgaoPortaria();
        String setorPortaria = request.setorPortaria();
        String numeroPortaria = request.numeroPortaria();
        LocalDate dataPortaria = request.dataPortaria();
        Integer numeroBca = request.numeroBca();
        LocalDate dataBca = request.dataBca();

        if (isBlank(orgaoPortaria) || isBlank(setorPortaria) || isBlank(numeroPortaria) || dataPortaria == null
                || numeroBca == null || dataBca == null || isBlank(request.portariaPdfUrl())) {
            throw new StatusCannotBeUpdatedException(
                    "É obrigatório informar a portaria, o BCA de referência e o PDF da portaria.");
        }
        if (primeiraPublicacao && (isBlank(request.epigrafe()) || isBlank(request.ementa()) || isBlank(request.preambulo())
                || isBlank(request.fecho()) || isBlank(request.assinatura()))) {
            throw new StatusCannotBeUpdatedException(
                    "Para publicar um documento pela primeira vez é obrigatório informar epígrafe, ementa, "
                    + "preâmbulo, fecho e assinatura.");
        }
        // O BCA é publicado apenas em dias úteis, então nunca passa de 366 (dias do ano).
        if (numeroBca < 1 || numeroBca > 366) {
            throw new StatusCannotBeUpdatedException("O número do BCA deve estar entre 1 e 366.");
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

        String orgaoSetor = orgaoPortaria.strip() + "/" + setorPortaria.strip();
        documento.setPortariaReferencia("Portaria " + orgaoSetor + " n° " + numeroPortaria.strip()
                + ", de " + formatarDataPorExtenso(dataPortaria));
        documento.setBcaReferencia("BCA n° " + numeroBca + ", de " + formatarDataPorExtenso(dataBca));
        documento.setDtPortariaReferencia(Timestamp.valueOf(dataPortaria.atStartOfDay()));
        documento.setDtBcaReferencia(Timestamp.valueOf(dataBca.atStartOfDay()));

        // Tipo da portaria: revogação é sempre REVOGACAO; publicar um documento já PUBLICADO é
        // uma alteração (numerada automaticamente); publicar pela primeira vez é a edição original.
        TipoPortariaPublicacaoEnum tipoPortaria = revogando ? TipoPortariaPublicacaoEnum.REVOGACAO
                : (alterando ? TipoPortariaPublicacaoEnum.ALTERACAO : TipoPortariaPublicacaoEnum.EDICAO);
        portariaPublicacaoService.registrar(documento, tipoPortaria, orgaoPortaria, setorPortaria,
                numeroPortaria, dataPortaria, numeroBca, dataBca, request.portariaPdfUrl());

        if (primeiraPublicacao) {
            // A parte preliminar só passa a existir de fato com a primeira publicação -- por isso
            // é coletada aqui, não durante a edição (ver Documento.java). Mesma lógica de "apaga
            // tudo e recria" de DocumentoParteNormativaService.salvarSecoes, só que agora só roda
            // aqui, uma única vez.
            documentoParteNormativaService.salvarItensPreliminares(documento, List.of(
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.EPIGRAFE, 1, null, request.epigrafe(), null, null),
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.EMENTA, 2, null, request.ementa(), null, null),
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.PREAMBULO, 3, null, request.preambulo(), null, null),
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.FECHO, 4, null, request.fecho(), null, null),
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.ASSINATURA, 5, null, request.assinatura(), null, null)
            ));
        }
        if (alterando) {
            // Só há emendas pendentes a consolidar numa ALTERAÇÃO publicada; a primeira publicação
            // nunca passou por EM_ALTERACAO e a revogação não consolida nada.
            emendaService.consolidarPublicacao(documento.getId(), documento.getPortariaReferencia(), documento.getBcaReferencia());
        }
    }

    // ─── Arquivos (PDF + HTML) ───────────────────────────────────────────────────

    // Os arquivos são gerados DEPOIS do commit da mudança de etapa. DocumentoPdfService/
    // DocumentoHtmlService leem o documento numa transação própria (REQUIRES_NEW, readOnly) e só
    // enxergam o que já foi confirmado: gerar dentro da transação em curso produziria um arquivo
    // com as cláusulas de emenda ainda pendentes (placeholder XYZ/ABC) em vez das que
    // EmendaService.consolidarPublicacao acabou de congelar, e com a situação BCA antiga. Sem
    // transação em andamento (testes unitários), gera na hora.
    private void aposCommit(Documento documento, Consumer<Documento> geracao) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            geracao.accept(documento);
            return;
        }
        Long id = documento.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    var tx = new TransactionTemplate(transactionManager);
                    tx.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
                    tx.executeWithoutResult(status -> documentoRepository.findById(id).ifPresent(geracao));
                } catch (Exception e) {
                    log.error("Falha ao gerar os arquivos do documento {} após a mudança de etapa", id, e);
                }
            }
        });
    }


    // Versão VIGENTE: a que a situação BCA descreve. Substitui a anterior e descarta a versão
    // em tramitação (o que estava em tramitação acaba de ser publicado ou revogado).
    private void gerarVersaoVigente(Documento documento) {
        documento.setUrlPdfTramitacao(null);
        documento.setUrlHtmlTramitacao(null);
        try {
            documento.setUrlPdf(documentoPdfService.gerarEArmazenarPdf(documento));
        } catch (Exception e) {
            // Não-fatal: a mudança de etapa não pode falhar por causa do PDF -- streamPdf cai de
            // volta para renderização ao vivo quando a cópia armazenada está ausente. Mas o erro
            // precisa ficar visível, senão a causa de um PDF armazenado desatualizado/ausente é
            // impossível de diagnosticar.
            log.error("Falha ao gerar/armazenar o PDF vigente do documento {}", documento.getId(), e);
        }
        try {
            documento.setUrlHtml(documentoHtmlService.gerarEArmazenarHtml(documento));
        } catch (Exception e) {
            log.error("Falha ao gerar/armazenar o HTML vigente do documento {}", documento.getId(), e);
        }
        documentoRepository.saveAndFlush(documento);
    }

    // Versão EM TRAMITAÇÃO congelada (EM_PUBLICACAO/EM_REVOGACAO): a vigente não é tocada.
    private void gerarVersaoEmTramitacao(Documento documento) {
        try {
            documento.setUrlPdfTramitacao(documentoPdfService.gerarEArmazenarPdf(documento));
        } catch (Exception e) {
            log.error("Falha ao gerar/armazenar o PDF em tramitação do documento {}", documento.getId(), e);
        }
        try {
            documento.setUrlHtmlTramitacao(documentoHtmlService.gerarEArmazenarHtml(documento));
        } catch (Exception e) {
            log.error("Falha ao gerar/armazenar o HTML em tramitação do documento {}", documento.getId(), e);
        }
        documentoRepository.saveAndFlush(documento);
    }

    private void descartarVersaoEmTramitacao(Documento documento) {
        documento.setUrlPdfTramitacao(null);
        documento.setUrlHtmlTramitacao(null);
        documentoRepository.saveAndFlush(documento);
    }

    private Usuario buscarUsuario(Long usuarioId, String mensagemSeAusente) {
        if (usuarioId == null) {
            throw new StatusCannotBeUpdatedException(mensagemSeAusente);
        }
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new StatusCannotBeUpdatedException("Usuário escolhido não encontrado."));
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
