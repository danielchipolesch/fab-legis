package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.usuarioDtos.RedefinirSenhaRequestDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.UsuarioCreateRequestDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.UsuarioResponseDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.UsuarioUpdateRequestDto;
import br.com.danielchipolesch.domain.services.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Gestão de usuários -- exclusiva de ADMIN (ver PapelEnum). Sem exclusão
// definitiva de propósito: usuários são autores de documento (FK sem ON
// DELETE), então o ciclo de vida é ativar/desativar, nunca apagar.
@RestController
@RequestMapping(value = "/v1/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Usuário", description = "Gestão de usuários (administração)")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDto>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("{id}")
    public ResponseEntity<UsuarioResponseDto> obter(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obter(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDto> criar(@RequestBody @Valid UsuarioCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    @PutMapping("{id}")
    public ResponseEntity<UsuarioResponseDto> atualizar(
            @PathVariable Long id, @RequestBody @Valid UsuarioUpdateRequestDto request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @PatchMapping("{id}/senha")
    public ResponseEntity<Void> redefinirSenha(
            @PathVariable Long id, @RequestBody @Valid RedefinirSenhaRequestDto request) {
        usuarioService.redefinirSenha(id, request);
        return ResponseEntity.noContent().build();
    }
}
