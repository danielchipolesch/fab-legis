package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.usuarioDtos.PostoGraduacaoResponseDto;
import br.com.danielchipolesch.infrastructure.repositories.PostoGraduacaoRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Listagem simples para popular o seletor de posto/graduação na tela de
// usuários -- catálogo fixo (ver migração V14), sem CRUD nesta fase.
@RestController
@RequestMapping(value = "/v1/postos-graduacoes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Posto/Graduação", description = "Consulta do catálogo de postos e graduações")
public class PostoGraduacaoController {

    @Autowired
    private PostoGraduacaoRepository postoGraduacaoRepository;

    @GetMapping
    public ResponseEntity<List<PostoGraduacaoResponseDto>> listar() {
        var lista = postoGraduacaoRepository.findAllOrdenado().stream()
                .map(PostoGraduacaoResponseDto::from)
                .toList();
        return ResponseEntity.ok(lista);
    }
}
