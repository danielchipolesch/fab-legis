package br.com.danielchipolesch.infrastructure.notificacao;

import br.com.danielchipolesch.application.dtos.usuarioDtos.PresencaResponseDto;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// Quem está editando um documento agora = quem tem uma conexão SSE aberta
// para ele, ponto -- nada de heartbeat nem janela de tempo (ver
// DocumentoPresencaService/migração V13, que removeu a tabela anterior). A
// desconexão (fechar aba, navegar pra outro documento, cair a rede) já
// dispara onCompletion/onError/onTimeout do próprio SseEmitter, que é
// exatamente o sinal "não está mais editando" de que precisamos.
@Component
public class DocumentoPresencaEmitterRegistry {

    private record Conexao(Long usuarioId, String nome, SseEmitter emitter) {}

    private static final long SEM_TIMEOUT = 0L;

    private final Map<Long, List<Conexao>> conexoesPorDocumento = new ConcurrentHashMap<>();

    public SseEmitter conectar(Long documentoId, Long usuarioId, String nome) {
        SseEmitter emitter = new SseEmitter(SEM_TIMEOUT);
        List<Conexao> lista = conexoesPorDocumento.computeIfAbsent(documentoId, k -> new CopyOnWriteArrayList<>());
        Conexao conexao = new Conexao(usuarioId, nome, emitter);
        lista.add(conexao);

        Runnable aoDesconectar = () -> {
            lista.remove(conexao);
            conexoesPorDocumento.computeIfPresent(documentoId, (k, l) -> l.isEmpty() ? null : l);
            transmitir(documentoId);
        };
        emitter.onCompletion(aoDesconectar);
        emitter.onTimeout(aoDesconectar);
        emitter.onError(e -> aoDesconectar.run());

        // Avisa todo mundo (inclusive quem acabou de entrar -- o frontend
        // filtra a si mesmo da lista recebida) que a composição mudou.
        transmitir(documentoId);

        return emitter;
    }

    private void transmitir(Long documentoId) {
        List<Conexao> lista = conexoesPorDocumento.getOrDefault(documentoId, List.of());
        List<PresencaResponseDto> presentes = lista.stream()
                .map(c -> new PresencaResponseDto(c.usuarioId(), c.nome()))
                .distinct() // mesma pessoa em duas abas conta uma vez só
                .toList();

        for (Conexao conexao : lista) {
            try {
                conexao.emitter().send(SseEmitter.event().name("presenca").data(presentes, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                // onError/onCompletion do próprio emitter cuida da remoção.
            }
        }
    }
}
