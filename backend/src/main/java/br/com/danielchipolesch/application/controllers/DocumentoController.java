package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestUpdateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaRequestDto;
import br.com.danielchipolesch.domain.services.DocumentoParteNormativaService;
import br.com.danielchipolesch.domain.services.DocumentoService;
import br.com.danielchipolesch.domain.services.DocumentoStatusService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/documentos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Documento", description = "Gerenciamento de documentos normativos")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private DocumentoStatusService documentoStatusService;

    @Autowired
    private DocumentoParteNormativaService documentoParteNormativaService;

    @PostMapping
    public ResponseEntity<DocumentoResponseSemAnexoTextualDto> post(
            @RequestBody @Valid DocumentoRequestCreateDto request) throws RuntimeException {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.create(request));
    }

    @PostMapping("{id}/clonar")
    public ResponseEntity<DocumentoResponseSemAnexoTextualDto> clone(
            @PathVariable(value = "id") Long id) throws RuntimeException {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.clone(id));
    }

    @GetMapping("{id}")
    public ResponseEntity<DocumentoResponseComAnexoTextualDto> getById(
            @PathVariable(value = "id") Long id) throws RuntimeException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(documentoParteNormativaService.getDocumentoComAnexoTextualDtoById(id));
    }

    @GetMapping("filtrar")
    public ResponseEntity<List<DocumentoResponseSemAnexoTextualDto>> getByDocumentationTypeAndBasicSubject(
            @RequestParam(value = "especie-normativa") Long documentTypeId,
            @RequestParam(value = "assunto-basico") Long basicSubjectId) throws RuntimeException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(documentoService.getByDocumentationTypeAndBasicSubject(documentTypeId, basicSubjectId));
    }

    @GetMapping("/obter-todos")
    public ResponseEntity<List<DocumentoResponseSemAnexoTextualDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) throws RuntimeException {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return ResponseEntity.status(HttpStatus.OK).body(documentoService.getAllAsDtos(pageable));
    }

    @PutMapping("{id}/aprovar")
    public ResponseEntity<DocumentoResponseSemAnexoTextualDto> setDocumentAsApproved(
            @PathVariable(value = "id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(documentoStatusService.approveDocument(id));
    }

    @PatchMapping("{id}/status")
    public ResponseEntity<DocumentoResponseSemAnexoTextualDto> changeStatus(
            @PathVariable(value = "id") Long id,
            @RequestBody @Valid DocumentoStatusRequestDto request) throws RuntimeException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(documentoStatusService.changeStatus(id, request.getStatus()));
    }

    @PutMapping("{id}")
    public ResponseEntity<DocumentoResponseSemAnexoTextualDto> update(
            @PathVariable(value = "id") Long id,
            @RequestBody @Valid DocumentoRequestUpdateDto request) throws RuntimeException {
        return ResponseEntity.status(HttpStatus.OK).body(documentoService.update(id, request));
    }

    @PutMapping("{idDocumento}/adicionar-item-anexo-parte-textual")
    public ResponseEntity<DocumentoResponseComAnexoTextualDto> addItemAnexoParteNormativa(
            @PathVariable(value = "idDocumento") Long idDocumento,
            @RequestBody ItemAnexoParteNormativaRequestDto request) throws RuntimeException {
        return ResponseEntity.status(HttpStatus.OK)
                .body(documentoParteNormativaService.adicionarItemAoDocumento(idDocumento, request));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable(value = "id") Long id) throws RuntimeException {
        documentoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
