package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.assuntoBasicoDtos.AssuntoBasicoRequestCreateDto;
import br.com.danielchipolesch.application.dtos.assuntoBasicoDtos.AssuntoBasicoRequestUpdateDto;
import br.com.danielchipolesch.application.dtos.assuntoBasicoDtos.AssuntoBasicoResponseDto;
import br.com.danielchipolesch.domain.services.AssuntoBasicoService;
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
@RequestMapping(value = "/v1/assunto-basico", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Assunto Básico", description = "Colocar descrição")
public class AssuntoBasicoController {

    private static final String BASE = "/v1/assunto-basico";

    @Autowired
    AssuntoBasicoService assuntoBasicoService;

    private EntityModel<AssuntoBasicoResponseDto> toModel(AssuntoBasicoResponseDto dto) {
        return EntityModel.of(dto,
                Link.of(BASE + "/" + dto.getIdAssuntoBasico()).withSelfRel(),
                Link.of(BASE + "/obter-todos").withRel("assunto-basico")
        );
    }

    @PostMapping
    public ResponseEntity<EntityModel<AssuntoBasicoResponseDto>> post(
            @RequestBody @Valid AssuntoBasicoRequestCreateDto request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(assuntoBasicoService.create(request)));
    }

    @PutMapping("{id}")
    public ResponseEntity<EntityModel<AssuntoBasicoResponseDto>> put(
            @PathVariable(value = "id") Long id,
            @RequestBody AssuntoBasicoRequestUpdateDto request) throws Exception {
        return ResponseEntity.ok(toModel(assuntoBasicoService.update(id, request)));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable(value = "id") Long id) throws Exception {
        assuntoBasicoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{id}")
    public ResponseEntity<EntityModel<AssuntoBasicoResponseDto>> getById(
            @PathVariable(value = "id") Long id) throws Exception {
        return ResponseEntity.ok(toModel(assuntoBasicoService.getById(id)));
    }

    @GetMapping("/obter-por-codigo-assunto-basico/{code}")
    public ResponseEntity<EntityModel<AssuntoBasicoResponseDto>> getByNumber(
            @PathVariable(value = "code") String code) throws Exception {
        return ResponseEntity.ok(toModel(assuntoBasicoService.getByNumber(code)));
    }

    @GetMapping("obter-todos")
    public ResponseEntity<List<EntityModel<AssuntoBasicoResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) throws Exception {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        List<EntityModel<AssuntoBasicoResponseDto>> models = assuntoBasicoService.getAll(pageable)
                .stream().map(this::toModel).toList();
        return ResponseEntity.ok(models);
    }
}
