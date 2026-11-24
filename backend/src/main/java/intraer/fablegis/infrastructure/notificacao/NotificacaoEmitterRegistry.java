package intraer.fablegis.infrastructure.notificacao;

import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// Registro em memória das conexões SSE abertas, por usuário -- suporta mais
// de uma aba/dispositivo por usuário. Em memória de propósito: só existe
// uma instância da aplicação nesta fase (ver comentário sobre Flyway vs
// ddl-auto no design doc de autenticação, mesma lógica de "sem
// infraestrutura extra até o problema aparecer de verdade"). Escalar para
// múltiplas instâncias exigiria mover isso para um pub/sub compartilhado
// (Redis, ou RabbitMQ/Kafka -- ver decisão em NotificacaoService).
@Component
public class NotificacaoEmitterRegistry {

    private static final long SEM_TIMEOUT = 0L;

    private final Map<Long, List<SseEmitter>> emittersPorUsuario = new ConcurrentHashMap<>();

    public SseEmitter registrar(Long usuarioId) {
        SseEmitter emitter = new SseEmitter(SEM_TIMEOUT);
        List<SseEmitter> lista = emittersPorUsuario.computeIfAbsent(usuarioId, k -> new CopyOnWriteArrayList<>());
        lista.add(emitter);

        emitter.onCompletion(() -> remover(usuarioId, emitter));
        emitter.onTimeout(() -> remover(usuarioId, emitter));
        emitter.onError(e -> remover(usuarioId, emitter));

        return emitter;
    }

    public void enviar(Long usuarioId, Object payload) {
        enviarEvento(usuarioId, "notificacao", payload);
    }

    // Mesma conexão do usuário, evento de outro nome: o frontend ouve cada um à parte (ver AppTopBar.vue).
    public void enviarEvento(Long usuarioId, String evento, Object payload) {
        List<SseEmitter> lista = emittersPorUsuario.get(usuarioId);
        if (lista == null) return;

        for (SseEmitter emitter : lista) {
            try {
                emitter.send(SseEmitter.event().name(evento).data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                remover(usuarioId, emitter);
            }
        }
    }

    // Só DEPOIS do commit da transação em curso (ou já, se não há transação): um rollback não pode ter avisado ninguém.
    public void enviarEventoAposCommit(Collection<Long> usuarioIds, String evento, Object payload) {
        if (usuarioIds == null || usuarioIds.isEmpty()) return;
        depoisDoCommit(() -> usuarioIds.forEach(id -> enviarEvento(id, evento, payload)));
    }

    // Para todos os usuários conectados agora (qualquer aba), depois do commit.
    public void enviarATodosAposCommit(String evento, Object payload) {
        depoisDoCommit(() -> List.copyOf(emittersPorUsuario.keySet()).forEach(id -> enviarEvento(id, evento, payload)));
    }

    private void depoisDoCommit(Runnable envio) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            envio.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                envio.run();
            }
        });
    }

    private void remover(Long usuarioId, SseEmitter emitter) {
        List<SseEmitter> lista = emittersPorUsuario.get(usuarioId);
        if (lista != null) lista.remove(emitter);
    }
}
