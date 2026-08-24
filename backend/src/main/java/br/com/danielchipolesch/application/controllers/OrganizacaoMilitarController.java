package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.usuarioDtos.OrganizacaoMilitarResponseDto;
import br.com.danielchipolesch.infrastructure.repositories.OrganizacaoMilitarRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Listagem simples para popular o seletor de OM na tela de usuários -- sem
// CRUD de OM nesta fase (só existe a OM "SISTEMA" seedada pela migração V9).
@RestController
@RequestMapping(value = "/v1/organizacoes-militares", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Organização Militar", description = "Consulta de organizações militares")
public class OrganizacaoMilitarController {

    @Autowired
    private OrganizacaoMilitarRepository organizacaoMilitarRepository;

    @GetMapping
    public ResponseEntity<List<OrganizacaoMilitarResponseDto>> listar() {
        var lista = organizacaoMilitarRepository.findAll().stream()
                .map(OrganizacaoMilitarResponseDto::from)
                .toList();
        return ResponseEntity.ok(lista);
    }
}
