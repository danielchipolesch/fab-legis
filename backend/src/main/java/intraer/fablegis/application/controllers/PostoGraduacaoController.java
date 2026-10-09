package intraer.fablegis.application.controllers;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.dtos.usuarioDtos.PostoGraduacaoResponseDto;
import intraer.fablegis.infrastructure.repositories.PostoGraduacaoRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Listagem simples para popular o seletor de posto/graduação na tela de
// usuários -- catálogo fixo (ver db/migration/V1__initial.sql), sem CRUD nesta fase.
@RestController
@RequestMapping(value = "/v1/postos-graduacoes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Posto/Graduação", description = "Consulta do catálogo de postos e graduações")
@RequiredArgsConstructor
public class PostoGraduacaoController {

    private final PostoGraduacaoRepository postoGraduacaoRepository;

    @GetMapping
    public ResponseEntity<List<PostoGraduacaoResponseDto>> listar() {
        var lista = postoGraduacaoRepository.findAllOrdenado().stream()
                .map(PostoGraduacaoResponseDto::from)
                .toList();
        return ResponseEntity.ok(lista);
    }
}
