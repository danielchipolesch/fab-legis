package intraer.fablegis.application.controllers;

import intraer.fablegis.application.dtos.usuarioDtos.RevisaoDeAcessoRequestDto;
import intraer.fablegis.application.dtos.usuarioDtos.UsuarioResponseDto;
import intraer.fablegis.domain.services.RevisaoDeAcessoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Revisão periódica de acessos: o admin geral confere os admins locais (e o CENDOC); cada admin local, os demais usuários da
// própria OM -- ver RevisaoDeAcesso. O servico decide quem revisa quem; aqui só se exige ser administrador.
@RestController
@RequestMapping(value = "/v1/revisao-de-acessos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Revisão de acessos", description = "Revisão periódica dos papéis elevados")
@PreAuthorize("hasAnyRole('ADM_OM', 'ADM_GER')")
@RequiredArgsConstructor
public class RevisaoDeAcessoController {

    private final RevisaoDeAcessoService revisaoDeAcessoService;

    // "Manter" (os mesmos papéis) ou "retirar" (um subconjunto): grava a data da revisão e registra na auditoria de acessos.
    @PostMapping("/{usuarioId}")
    public ResponseEntity<UsuarioResponseDto> revisar(@PathVariable Long usuarioId,
                                                              @RequestBody @Valid RevisaoDeAcessoRequestDto pedido) {
        return ResponseEntity.ok(revisaoDeAcessoService.revisar(usuarioId, pedido.papeis()));
    }
}
