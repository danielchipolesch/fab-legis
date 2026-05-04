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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/assunto-basico")
@Tag(name = "Assunto Básico", description = "Colocar descrição")
public class AssuntoBasicoController {

    @Autowired
    AssuntoBasicoService assuntoBasicoService;

    @PostMapping
    public ResponseEntity<AssuntoBasicoResponseDto> post(@RequestBody @Valid AssuntoBasicoRequestCreateDto request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(assuntoBasicoService.create(request));
    }

    @PutMapping("{id}")
    public ResponseEntity<AssuntoBasicoResponseDto> put(@PathVariable(value = "id") Long id,
                                                        @RequestBody AssuntoBasicoRequestUpdateDto request) throws Exception {
        return ResponseEntity.status(HttpStatus.OK).body(assuntoBasicoService.update(id, request));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<AssuntoBasicoResponseDto> delete(@PathVariable(value = "id") Long id) throws Exception {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(assuntoBasicoService.delete(id));
    }

    @GetMapping("{id}")
    public ResponseEntity<AssuntoBasicoResponseDto>  getById(@PathVariable(value = "id") Long id) throws Exception {
        return  ResponseEntity.status(HttpStatus.OK).body(assuntoBasicoService.getById(id));
    }

    @GetMapping("/obter-por-codigo-assunto-basico/{code}")
    public ResponseEntity<AssuntoBasicoResponseDto>  getByNumber(@PathVariable(value = "code") String code) throws Exception {
        return  ResponseEntity.status(HttpStatus.OK).body(assuntoBasicoService.getByNumber(code));
    }

    @GetMapping("obter-todos")
    public ResponseEntity<List<AssuntoBasicoResponseDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) throws Exception {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return ResponseEntity.status(HttpStatus.OK).body(assuntoBasicoService.getAll(pageable));
    }
}
