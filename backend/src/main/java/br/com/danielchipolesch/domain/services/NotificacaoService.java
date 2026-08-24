package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.notificacaoDtos.NotificacaoResponseDto;
import br.com.danielchipolesch.domain.entities.notificacao.Notificacao;
import br.com.danielchipolesch.domain.entities.notificacao.TipoNotificacaoEnum;
import br.com.danielchipolesch.domain.entities.usuario.PapelEnum;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.infrastructure.notificacao.NotificacaoEmitterRegistry;
import br.com.danielchipolesch.infrastructure.repositories.NotificacaoRepository;
import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

// Gravada na mesma transação da ação que a gera (compartilhamento,
// documento entrando em situação que aguarda aprovação -- ver
// DocumentoCompartilhamentoService/DocumentoStatusService), entregue ao
// vivo por SSE via NotificacaoEmitterRegistry. A tabela é sempre a fonte
// de verdade (cobre quem estava offline); o push é só uma otimização de
// latência por cima dela, por isso só dispara DEPOIS do commit -- se a
// transação inteira der rollback, a notificação nunca existiu de verdade
// e não pode ter sido empurrada antes disso.
@Service
public class NotificacaoService {

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacaoEmitterRegistry emitterRegistry;

    @Transactional
    public void criar(Usuario destinatario, TipoNotificacaoEnum tipo, String mensagem, Long documentoId, String documentoDescricao) {
        var notificacao = new Notificacao();
        notificacao.setDestinatario(destinatario);
        notificacao.setTipo(tipo);
        notificacao.setMensagem(mensagem);
        notificacao.setDocumentoId(documentoId);
        notificacao.setDocumentoDescricao(documentoDescricao);
        Notificacao salva = notificacaoRepository.save(notificacao);

        agendarPushAposCommit(destinatario.getId(), salva);
    }

    // Notifica todo APROVADOR da OM do documento (mais ADMIN, que aprova em
    // qualquer OM) -- usado quando um documento entra em MINUTA ou
    // EM_ALTERACAO, situações que aguardam uma ação de aprovação.
    @Transactional
    public void notificarAprovadoresPendencia(Long omId, Long documentoId, String documentoDescricao, String mensagem) {
        List<Usuario> aprovadores = usuarioRepository.findAprovadoresDaOmOuAdmins(
                omId, List.of(PapelEnum.APROVADOR, PapelEnum.ADMIN));
        for (Usuario aprovador : aprovadores) {
            criar(aprovador, TipoNotificacaoEnum.APROVACAO_PENDENTE, mensagem, documentoId, documentoDescricao);
        }
    }

    private void agendarPushAposCommit(Long destinatarioId, Notificacao notificacao) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emitterRegistry.enviar(destinatarioId, NotificacaoResponseDto.from(notificacao));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emitterRegistry.enviar(destinatarioId, NotificacaoResponseDto.from(notificacao));
            }
        });
    }

    public List<NotificacaoResponseDto> listarNaoLidas(Usuario usuario) {
        return notificacaoRepository.findByDestinatarioIdAndLidaFalseOrderByDtCriacaoDesc(usuario.getId()).stream()
                .map(NotificacaoResponseDto::from)
                .toList();
    }

    public Page<NotificacaoResponseDto> listarTodas(Usuario usuario, Pageable pageable) {
        return notificacaoRepository.findByDestinatarioIdOrderByDtCriacaoDesc(usuario.getId(), pageable)
                .map(NotificacaoResponseDto::from);
    }

    @Transactional
    public void marcarComoLida(Long notificacaoId, Usuario usuario) {
        Notificacao notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada."));
        if (!notificacao.getDestinatario().getId().equals(usuario.getId())) {
            throw new ResourceNotFoundException("Notificação não encontrada.");
        }
        notificacao.setLida(true);
        notificacao.setDtLeitura(Timestamp.from(Instant.now()));
        notificacaoRepository.save(notificacao);
    }

    @Transactional
    public void marcarTodasComoLidas(Usuario usuario) {
        notificacaoRepository.marcarTodasComoLidas(usuario.getId(), Timestamp.from(Instant.now()));
    }
}
