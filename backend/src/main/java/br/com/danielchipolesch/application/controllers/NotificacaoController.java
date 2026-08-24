package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.notificacaoDtos.NotificacaoResponseDto;
import br.com.danielchipolesch.domain.services.NotificacaoService;
import br.com.danielchipolesch.infrastructure.notificacao.NotificacaoEmitterRegistry;
import br.com.danielchipolesch.infrastructure.security.AutenticacaoUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/notificacoes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notificação", description = "Notificações em tempo real (SSE) e histórico por usuário")
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private NotificacaoEmitterRegistry emitterRegistry;

    // EventSource (API nativa do browser para SSE) não permite header
    // customizado -- a autenticação deste request chega via query param
    // (ver JwtAuthenticationFilter), único endpoint com essa exceção.
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return emitterRegistry.registrar(AutenticacaoUtil.usuarioAtual().getId());
    }

    @GetMapping("/nao-lidas")
    public ResponseEntity<List<NotificacaoResponseDto>> listarNaoLidas() {
        return ResponseEntity.ok(notificacaoService.listarNaoLidas(AutenticacaoUtil.usuarioAtual()));
    }

    @GetMapping
    public ResponseEntity<Page<NotificacaoResponseDto>> listarTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dtCriacao").descending());
        return ResponseEntity.ok(notificacaoService.listarTodas(AutenticacaoUtil.usuarioAtual(), pageable));
    }

    @PatchMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id) {
        notificacaoService.marcarComoLida(id, AutenticacaoUtil.usuarioAtual());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas() {
        notificacaoService.marcarTodasComoLidas(AutenticacaoUtil.usuarioAtual());
        return ResponseEntity.noContent().build();
    }
}
