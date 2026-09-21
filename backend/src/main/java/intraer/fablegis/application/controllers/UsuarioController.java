package intraer.fablegis.application.controllers;

import intraer.fablegis.application.dtos.usuarioDtos.RedefinirSenhaRequestDto;
import intraer.fablegis.application.dtos.usuarioDtos.UsuarioBuscaResponseDto;
import intraer.fablegis.application.dtos.usuarioDtos.UsuarioCreateRequestDto;
import intraer.fablegis.application.dtos.usuarioDtos.UsuarioElegivelResponseDto;
import intraer.fablegis.application.dtos.usuarioDtos.UsuarioResponseDto;
import intraer.fablegis.application.dtos.usuarioDtos.UsuarioUpdateRequestDto;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.services.UsuarioService;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import intraer.fablegis.infrastructure.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDto>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    // Candidatos para os seletores de "escolher pessoa" (enviar para revisão/
    // revogação, aprovar escolhendo o publicador -- ver DocumentoStatusService).
    // Sobrescreve o @PreAuthorize de classe: qualquer autenticado pode chamar, não só
    // Admin -- a OM vem sempre de quem está chamando, nunca de parâmetro, então não
    // há como enumerar usuários de outra OM por aqui.
    @GetMapping("/elegiveis")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UsuarioElegivelResponseDto>> elegiveis(
            @RequestParam PapelEnum papel,
            @RequestParam(required = false) String q,
            Authentication authentication) {
        var usuario = ((UsuarioPrincipal) authentication.getPrincipal()).getUsuario();
        var termo = q != null ? q.trim() : "";
        var elegiveis = termo.isEmpty()
                ? usuarioRepository.findByOmIdAndPapel(usuario.getOm().getId(), papel)
                : usuarioRepository.findByOmIdAndPapelAndTermo(usuario.getOm().getId(), papel, termo);
        var candidatos = elegiveis.stream().map(UsuarioElegivelResponseDto::from).toList();
        return ResponseEntity.ok(candidatos);
    }

    // Busca de coautor por nome/nome de guerra (CompartilharDialog.vue) -- autor de
    // documento quase nunca sabe o CPF de um colega de cor, mas sabe o nome; o CPF só
    // é resolvido internamente a partir do candidato escolhido aqui (ver
    // DocumentoCompartilhamentoService.compartilhar, que continua recebendo CPF).
    // Sem filtro de OM/papel de propósito: coautoria não é restrita a isso. Termo
    // mínimo de 2 caracteres evita devolver a base inteira a cada tecla digitada.
    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UsuarioBuscaResponseDto>> buscar(
            @RequestParam String q, Authentication authentication) {
        var usuario = ((UsuarioPrincipal) authentication.getPrincipal()).getUsuario();
        var termo = q.trim();
        if (termo.length() < 2) return ResponseEntity.ok(List.of());
        var candidatos = usuarioRepository.buscarPorNome(termo, usuario.getId()).stream()
                .map(UsuarioBuscaResponseDto::from)
                .toList();
        return ResponseEntity.ok(candidatos);
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
