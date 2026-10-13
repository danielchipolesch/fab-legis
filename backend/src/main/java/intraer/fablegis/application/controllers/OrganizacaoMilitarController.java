package intraer.fablegis.application.controllers;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.dtos.usuarioDtos.OrganizacaoMilitarResponseDto;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import intraer.fablegis.domain.services.DistintivoDaOmService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

// Listagem simples para popular o seletor de OM na tela de usuários -- sem
// CRUD de OM nesta fase (as OMs vêm seedadas nas migrações).
@RestController
@RequestMapping(value = "/v1/organizacoes-militares", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Organização Militar", description = "Consulta de organizações militares")
@RequiredArgsConstructor
public class OrganizacaoMilitarController {

    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;
    private final DistintivoDaOmService distintivoDaOmService;

    // Mesma razão do EspecieNormativaController.getAll: lista de referência que muda raramente
    // (as OMs vêm seedadas nas migrações).
    @GetMapping
    public ResponseEntity<List<OrganizacaoMilitarResponseDto>> listar() {
        var lista = organizacaoMilitarRepository.findAll().stream()
                .map(OrganizacaoMilitarResponseDto::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(lista);
    }

    // DOM (distintivo) da OM -- cadastro exclusivo do administrador (ver DistintivoDaOmService).
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "{id}/distintivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrganizacaoMilitarResponseDto> definirDistintivo(
            @PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo) throws Exception {
        return ResponseEntity.ok(distintivoDaOmService.definir(id, arquivo));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("{id}/distintivo")
    public ResponseEntity<OrganizacaoMilitarResponseDto> removerDistintivo(@PathVariable Long id) {
        return ResponseEntity.ok(distintivoDaOmService.remover(id));
    }
}
