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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/especie-normativa")
//@CrossOrigin(origins = "*")
@Tag(name = "Espécie Normativa", description = "Inserir descrição")
public class EspecieNormativaController {

    @Autowired
    EspecieNormativaService especieNormativaService;

    @PostMapping
    public ResponseEntity<DocumentationTypeResponseDto> post(@RequestBody @Valid DocumentationTypeRequestCreateDto request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(especieNormativaService.create(request));
    }

    @GetMapping("{id}")
    public ResponseEntity<DocumentationTypeResponseDto>  getById(@PathVariable(value = "id") Long id) throws Exception {
        return  ResponseEntity.status(HttpStatus.OK).body(especieNormativaService.getById(id));
    }

    @GetMapping("obter-todos")
    public ResponseEntity<List<DocumentationTypeResponseDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) throws Exception {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return ResponseEntity.status(HttpStatus.OK).body(especieNormativaService.getAll(pageable));
    }

    @PutMapping("{id}")
    public ResponseEntity<DocumentationTypeResponseDto> put(@PathVariable(value = "id") Long id,
                                                            @RequestBody DocumentationTypeRequestUpdateDto request) throws Exception {
        return ResponseEntity.status(HttpStatus.OK).body(especieNormativaService.update(id, request));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<DocumentationTypeResponseDto> delete(@PathVariable(value = "id") Long id) throws Exception {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(especieNormativaService.delete(id));
    }
}
