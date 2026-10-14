package intraer.fablegis.application.controllers;

import intraer.fablegis.application.dtos.usuarioDtos.RevisaoDeAcessoRequestDto;
import intraer.fablegis.application.dtos.usuarioDtos.RevisaoDeAcessoResponseDto;
import intraer.fablegis.domain.services.RevisaoDeAcessoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Revisão periódica de acessos: o admin geral confere os admins locais (e o CENDOC); cada admin local, os demais usuários da
// própria OM -- ver RevisaoDeAcesso. O servico decide quem revisa quem; aqui só se exige ser administrador.
@RestController
@RequestMapping(value = "/v1/revisao-de-acessos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Revisão de acessos", description = "Revisão periódica dos papéis elevados")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RevisaoDeAcessoController {

    private final RevisaoDeAcessoService revisaoDeAcessoService;

    @GetMapping
    public ResponseEntity<List<RevisaoDeAcessoResponseDto>> listar() {
        return ResponseEntity.ok(revisaoDeAcessoService.listar());
    }

    // "Manter" (os mesmos papéis) ou "retirar" (um subconjunto): grava a data da revisão e registra na auditoria de acessos.
    @PostMapping("/{usuarioId}")
    public ResponseEntity<RevisaoDeAcessoResponseDto> revisar(@PathVariable Long usuarioId,
                                                              @RequestBody @Valid RevisaoDeAcessoRequestDto pedido) {
        return ResponseEntity.ok(revisaoDeAcessoService.revisar(usuarioId, pedido.papeis()));
    }
}
