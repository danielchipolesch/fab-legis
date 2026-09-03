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
    @Autowired UsuarioRepository usuarioRepository;

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
        // Alguém já revisou este documento antes (mesmo que em ciclo anterior de emenda)?
        // Discrimina fluxo normal (nunca publicado) de ciclo de alteração (já publicado
        // ao menos uma vez) em todos os pontos que precisam saber -- ver comentários abaixo.
        boolean jaPublicadoAntes = documento.getDtPublicacao() != null;

        boolean transicaoValida = switch (novoStatus) {
            // MINUTA nunca é alcançável a partir de ALTERADO/EM_REVISAO-de-alteração: um
            // documento que passou por EM_ALTERACAO carrega numeração com sufixo de letra e
            // elementos marcados (INCLUIDO/ALTERADO/REVOGADO) que a renumeração simples de
            // MINUTA não entende — só é permitido a partir do fluxo normal.
            case MINUTA            -> current == DocumentoStatusEnum.RASCUNHO
                    || (current == DocumentoStatusEnum.EM_REVISAO && !jaPublicadoAntes)
                    || (current == DocumentoStatusEnum.EM_PUBLICACAO && !jaPublicadoAntes);
            case EM_REVISAO        -> current == DocumentoStatusEnum.MINUTA || current == DocumentoStatusEnum.EM_ALTERACAO;
            // Só alcançável a partir de EM_REVISAO -- e só quando a "identidade" da revisão
            // bate com o status: nunca publicado -> aprova pro fluxo normal; já publicado
            // antes -> aprova pra ciclo de alteração.
            case APROVADO          -> current == DocumentoStatusEnum.EM_REVISAO && !jaPublicadoAntes;
            case ALTERADO          -> current == DocumentoStatusEnum.EM_REVISAO && jaPublicadoAntes;
            // Nunca pedido diretamente pelo cliente -- consequência interna e imediata de
            // aprovar (ver cascatearParaPublicacao abaixo). Listado aqui só pra
            // transicaoValida aceitar a escrita que o próprio serviço faz.
            case EM_PUBLICACAO     -> current == DocumentoStatusEnum.APROVADO || current == DocumentoStatusEnum.ALTERADO;
            case PUBLICADO         -> current == DocumentoStatusEnum.EM_PUBLICACAO || current == DocumentoStatusEnum.EM_REVOGACAO;
            // PUBLICADO -> aqui é a ação livre "Iniciar Alteração" (papel APROV da OM, sem
            // atribuição prévia); as demais são "devolver" de dentro do ciclo de alteração já
            // em andamento (só quando já publicado antes, ver MINUTA acima pro espelho).
            case EM_ALTERACAO      -> current == DocumentoStatusEnum.PUBLICADO
                    || ((current == DocumentoStatusEnum.EM_REVISAO || current == DocumentoStatusEnum.EM_PUBLICACAO) && jaPublicadoAntes);
            case ANALISE_REVOGACAO -> current == DocumentoStatusEnum.PUBLICADO;
            case EM_REVOGACAO      -> current == DocumentoStatusEnum.ANALISE_REVOGACAO;
            case REVOGADO          -> current == DocumentoStatusEnum.EM_REVOGACAO;
            case CANCELADO         -> current == DocumentoStatusEnum.RASCUNHO || current == DocumentoStatusEnum.MINUTA;
            default                -> false;
        };

        if (!transicaoValida) {
            throw new StatusCannotBeUpdatedException(DocumentoException.CANNOT_BE_UPDATED.getMessage());
        }

        // "Devolver" (EM_REVISAO/EM_PUBLICACAO -> MINUTA/EM_ALTERACAO, ANALISE_REVOGACAO/
        // EM_REVOGACAO -> PUBLICADO) limpa a atribuição -- a próxima vez que o documento for
        // enviado, alguém (talvez outra pessoa) é escolhido de novo.
        boolean devolvendo = (current == DocumentoStatusEnum.EM_REVISAO || current == DocumentoStatusEnum.EM_PUBLICACAO)
                && (novoStatus == DocumentoStatusEnum.MINUTA || novoStatus == DocumentoStatusEnum.EM_ALTERACAO)
                || (current == DocumentoStatusEnum.ANALISE_REVOGACAO || current == DocumentoStatusEnum.EM_REVOGACAO)
                        && novoStatus == DocumentoStatusEnum.PUBLICADO;
        if (devolvendo) {
            documento.setRevisorAtribuido(null);
            documento.setPublicadorAtribuido(null);
        }

        // Enviar para revisão/análise de revogação: exige a pessoa escolhida (papel APROV,
        // validado em DocumentoAcessoService.podeMudarStatus -- aqui só resolve o registro).
        if (novoStatus == DocumentoStatusEnum.EM_REVISAO || novoStatus == DocumentoStatusEnum.ANALISE_REVOGACAO) {
            documento.setRevisorAtribuido(buscarUsuario(request.revisorId(),
                    "É obrigatório escolher quem vai revisar."));
        }

        // Aprovar a revogação: exige a pessoa escolhida (papel PUBLIC) que vai formalizá-la.
        if (novoStatus == DocumentoStatusEnum.EM_REVOGACAO) {
            documento.setPublicadorAtribuido(buscarUsuario(request.publicadorId(),
                    "É obrigatório escolher quem vai publicar a revogação."));
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

            // Tipo da portaria: revogação é sempre REVOGACAO; publicar um documento já
            // publicado antes (veio do ciclo de alteração) é uma alteração (numerada
            // automaticamente); publicar pela primeira vez é a edição original.
            TipoPortariaPublicacaoEnum tipoPortaria = !publicando ? TipoPortariaPublicacaoEnum.REVOGACAO
                    : (jaPublicadoAntes ? TipoPortariaPublicacaoEnum.ALTERACAO : TipoPortariaPublicacaoEnum.EDICAO);
            portariaPublicacaoService.registrar(documento, tipoPortaria, orgaoPortaria, setorPortaria,
                    numeroPortaria, dataPortaria, numeroBca, dataBca, request.portariaPdfUrl());

            if (publicando) {
                // Substitui a parte preliminar do documento pelo conteúdo informado
                // nesta publicação (mesma lógica de "apaga tudo e recria" já usada
                // por DocumentoParteNormativaService.salvarSecoes durante a edição,
                // só que agora só roda aqui).
                documentoParteNormativaService.salvarItensPreliminares(documento, List.of(
                        new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.EPIGRAFE, 1, null, request.epigrafe(), null, null),
                        new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.EMENTA, 2, null, request.ementa(), null, null),
                        new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.PREAMBULO, 3, null, request.preambulo(), null, null),
                        new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.FECHO, 4, null, request.fecho(), null, null),
                        new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.ASSINATURA, 5, null, request.assinatura(), null, null)
                ));

                // Só há emendas pendentes a consolidar quando esta publicação é de um ciclo
                // de alteração (já publicado antes); a primeira publicação nunca passou por
                // EM_ALTERACAO, então não há nada para consolidar.
                if (jaPublicadoAntes) {
                    emendaService.consolidarPublicacao(id, documento.getPortariaReferencia(), documento.getBcaReferencia());
                }
            }
        }

        Timestamp agora = Timestamp.from(Instant.now());
        switch (novoStatus) {
            case MINUTA            -> documento.setDtMinuta(agora);
            case EM_REVISAO        -> documento.setDtEmRevisao(agora);
            case APROVADO          -> documento.setDtAprovacao(agora);
            case ALTERADO          -> documento.setDtAlterado(agora);
            case EM_PUBLICACAO     -> documento.setDtEmPublicacao(agora);
            case PUBLICADO         -> documento.setDtPublicacao(agora);
            case EM_ALTERACAO      -> documento.setDtEmAlteracao(agora);
            case ANALISE_REVOGACAO -> documento.setDtAnaliseRevogacao(agora);
            case EM_REVOGACAO      -> documento.setDtEmRevogacao(agora);
            case REVOGADO          -> documento.setDtRevogacao(agora);
            case CANCELADO         -> documento.setDtCancelamento(agora);
            default                -> { }
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
        // renderizar de novo — ver DocumentoPdfService.streamPdf. PUBLICADO/REVOGADO
        // precisam regenerar mesmo que ALTERADO já tenha uma cópia, pois é só aí que
        // portaria/BCA reais substituem o placeholder e (na publicação)
        // consolidarPublicacao acima congela as cláusulas de emenda — o conteúdo muda.
        if (novoStatus == DocumentoStatusEnum.APROVADO
                || novoStatus == DocumentoStatusEnum.ALTERADO
                || novoStatus == DocumentoStatusEnum.PUBLICADO
                || novoStatus == DocumentoStatusEnum.REVOGADO) {
            regenerarPdf(documento, novoStatus);
        }

        String descricao = String.format("%s %s-%d",
                documento.getEspecieNormativa().getSigla(),
                documento.getAssuntoBasico().getCodigo(),
                documento.getNumeroSecundario());

        documentoHistoricoService.registrar(documento, TipoAlteracaoEnum.ALTERACAO_STATUS,
                current.name() + " → " + novoStatus.name(), current, novoStatus);

        // Cada transição de atribuição avisa só a pessoa escolhida -- nunca uma OM
        // inteira (ver PapelEnum: o modelo agora é de atribuição pessoal, não mais
        // "qualquer Aprovador pega").
        if (novoStatus == DocumentoStatusEnum.EM_REVISAO || novoStatus == DocumentoStatusEnum.ANALISE_REVOGACAO) {
            notificacaoService.notificarAtribuicao(documento.getRevisorAtribuido().getId(), documento.getId(), descricao,
                    "O documento " + descricao + " foi atribuído a você para revisão.");
        }
        if (novoStatus == DocumentoStatusEnum.EM_REVOGACAO) {
            notificacaoService.notificarAtribuicao(documento.getPublicadorAtribuido().getId(), documento.getId(), descricao,
                    "O documento " + descricao + " foi atribuído a você para publicar a revogação.");
        }

        // Aprovar (fluxo normal ou de alteração) nunca fica parado em APROVADO/ALTERADO
        // esperando uma ação separada -- o próprio Aprovador já escolhe, no mesmo ato,
        // quem vai publicar (ver DocumentoStatusRequestDto.publicadorId), então o
        // serviço cascateia direto para EM_PUBLICACAO na mesma transação.
        if (novoStatus == DocumentoStatusEnum.APROVADO || novoStatus == DocumentoStatusEnum.ALTERADO) {
            cascatearParaPublicacao(documento, request.publicadorId(), descricao);
        }

        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(documento);
    }

    private void regenerarPdf(Documento documento, DocumentoStatusEnum novoStatus) {
        try {
            String urlPdf = documentoPdfService.gerarEArmazenarPdf(documento);
            documento.setUrlPdf(urlPdf);
            documentoRepository.saveAndFlush(documento);
        } catch (Exception e) {
            // Não-fatal: a mudança de status não pode falhar por causa do PDF —
            // streamPdf cai de volta para renderização ao vivo quando urlPdf está
            // ausente. Mas o erro precisa ficar visível, senão a causa de um PDF
            // armazenado desatualizado/ausente é impossível de diagnosticar.
            log.error("Falha ao gerar/armazenar PDF do documento {} na transição para {}",
                    documento.getId(), novoStatus, e);
        }
    }

    private void cascatearParaPublicacao(Documento documento, Long publicadorId, String descricao) {
        DocumentoStatusEnum anterior = documento.getDocumentoStatus();
        documento.setPublicadorAtribuido(buscarUsuario(publicadorId,
                "É obrigatório escolher quem vai publicar."));
        documento.setDtEmPublicacao(Timestamp.from(Instant.now()));
        documento.setDocumentoStatus(DocumentoStatusEnum.EM_PUBLICACAO);
        documentoRepository.saveAndFlush(documento);

        documentoHistoricoService.registrar(documento, TipoAlteracaoEnum.ALTERACAO_STATUS,
                anterior.name() + " → " + DocumentoStatusEnum.EM_PUBLICACAO.name(), anterior, DocumentoStatusEnum.EM_PUBLICACAO);

        notificacaoService.notificarAtribuicao(documento.getPublicadorAtribuido().getId(), documento.getId(), descricao,
                "O documento " + descricao + " foi atribuído a você para publicação.");
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
