package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.domain.services.AnexoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/documentos/{documentoId}/anexos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Anexo", description = "Gerenciamento de anexos de documentos")
public class AnexoController {

    @Autowired
    private AnexoService anexoService;

    @GetMapping
    public ResponseEntity<List<AnexoResponseDto>> listar(
            @PathVariable Long documentoId) {
        return ResponseEntity.ok(anexoService.listar(documentoId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnexoResponseDto> adicionar(
            @PathVariable Long documentoId,
            @RequestParam("titulo") String titulo,
            @RequestParam("arquivo") MultipartFile arquivo) throws Exception {
        AnexoResponseDto dto = anexoService.adicionar(documentoId, titulo, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("{anexoId}")
    public ResponseEntity<Void> remover(
            @PathVariable Long documentoId,
            @PathVariable Long anexoId) {
        anexoService.remover(documentoId, anexoId);
        return ResponseEntity.noContent().build();
    }
}
