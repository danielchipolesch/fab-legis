package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.especieNormativaDtos.DocumentationTypeRequestCreateDto;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.DocumentationTypeResponseDto;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.DocumentationTypeRequestUpdateDto;
import br.com.danielchipolesch.domain.services.EspecieNormativaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/especie-normativa", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Espécie Normativa", description = "Inserir descrição")
public class EspecieNormativaController {

    private static final String BASE = "/v1/especie-normativa";

    @Autowired
    EspecieNormativaService especieNormativaService;

    private EntityModel<DocumentationTypeResponseDto> toModel(DocumentationTypeResponseDto dto) {
        return EntityModel.of(dto,
                Link.of(BASE + "/" + dto.getId()).withSelfRel(),
                Link.of(BASE + "/obter-todos").withRel("especie-normativa")
        );
    }

    @PostMapping
    public ResponseEntity<EntityModel<DocumentationTypeResponseDto>> post(
            @RequestBody @Valid DocumentationTypeRequestCreateDto request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(especieNormativaService.create(request)));
    }

    @GetMapping("{id}")
    public ResponseEntity<EntityModel<DocumentationTypeResponseDto>> getById(
            @PathVariable(value = "id") Long id) throws Exception {
        return ResponseEntity.ok(toModel(especieNormativaService.getById(id)));
    }

    @GetMapping("obter-todos")
    public ResponseEntity<List<EntityModel<DocumentationTypeResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) throws Exception {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        List<EntityModel<DocumentationTypeResponseDto>> models = especieNormativaService.getAll(pageable)
                .stream().map(this::toModel).toList();
        return ResponseEntity.ok(models);
    }

    @PutMapping("{id}")
    public ResponseEntity<EntityModel<DocumentationTypeResponseDto>> put(
            @PathVariable(value = "id") Long id,
            @RequestBody DocumentationTypeRequestUpdateDto request) throws Exception {
        return ResponseEntity.ok(toModel(especieNormativaService.update(id, request)));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable(value = "id") Long id) throws Exception {
        especieNormativaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
