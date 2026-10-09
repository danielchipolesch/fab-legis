package intraer.fablegis.application.controllers;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.dtos.usuarioDtos.OrganizacaoMilitarResponseDto;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

// Listagem simples para popular o seletor de OM na tela de usuários -- sem
// CRUD de OM nesta fase (só existe a OM "SISTEMA" seedada na migração inicial).
@RestController
@RequestMapping(value = "/v1/organizacoes-militares", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Organização Militar", description = "Consulta de organizações militares")
@RequiredArgsConstructor
public class OrganizacaoMilitarController {

    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;

    // Mesma razão do EspecieNormativaController.getAll: lista de referência que muda raramente
    // (hoje só a OM "SISTEMA" seedada).
    @GetMapping
    public ResponseEntity<List<OrganizacaoMilitarResponseDto>> listar() {
        var lista = organizacaoMilitarRepository.findAll().stream()
                .map(OrganizacaoMilitarResponseDto::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(lista);
    }
}
