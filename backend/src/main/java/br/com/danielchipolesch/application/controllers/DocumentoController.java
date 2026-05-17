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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/documentos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Documento", description = "Gerenciamento de documentos normativos")
public class DocumentoController {

    private static final String BASE = "/v1/documentos";

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private DocumentoStatusService documentoStatusService;

    @Autowired
    private DocumentoParteNormativaService documentoParteNormativaService;

    private EntityModel<DocumentoResponseSemAnexoTextualDto> toModel(DocumentoResponseSemAnexoTextualDto dto) {
        Long id = dto.getIdDocumento();
        return EntityModel.of(dto,
                Link.of(BASE + "/" + id).withSelfRel(),
                Link.of(BASE + "/obter-todos").withRel("documentos"),
                Link.of(BASE + "/" + id + "/status").withRel("status"),
                Link.of(BASE + "/" + id + "/clonar").withRel("clonar")
        );
    }

    @PostMapping
    public ResponseEntity<EntityModel<DocumentoResponseSemAnexoTextualDto>> post(
            @RequestBody @Valid DocumentoRequestCreateDto request) throws RuntimeException {
        DocumentoResponseSemAnexoTextualDto dto = documentoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(dto));
    }

    @PostMapping("{id}/clonar")
    public ResponseEntity<EntityModel<DocumentoResponseSemAnexoTextualDto>> clone(
            @PathVariable(value = "id") Long id) throws RuntimeException {
        DocumentoResponseSemAnexoTextualDto dto = documentoService.clone(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(dto));
    }

    @GetMapping("{id}")
    public ResponseEntity<EntityModel<DocumentoResponseComAnexoTextualDto>> getById(
            @PathVariable(value = "id") Long id) throws RuntimeException {
        DocumentoResponseComAnexoTextualDto dto = documentoParteNormativaService.getDocumentoComAnexoTextualDtoById(id);
        EntityModel<DocumentoResponseComAnexoTextualDto> model = EntityModel.of(dto,
                Link.of(BASE + "/" + id).withSelfRel(),
                Link.of(BASE + "/obter-todos").withRel("documentos"),
                Link.of(BASE + "/" + id + "/clonar").withRel("clonar"),
                Link.of(BASE + "/" + id + "/status").withRel("status")
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("filtrar")
    public ResponseEntity<List<EntityModel<DocumentoResponseSemAnexoTextualDto>>> getByDocumentationTypeAndBasicSubject(
            @RequestParam(value = "especie-normativa") Long documentTypeId,
            @RequestParam(value = "assunto-basico") Long basicSubjectId) throws RuntimeException {
        List<EntityModel<DocumentoResponseSemAnexoTextualDto>> models = documentoService
                .getByDocumentationTypeAndBasicSubject(documentTypeId, basicSubjectId)
                .stream().map(this::toModel).toList();
        return ResponseEntity.ok(models);
    }

    @GetMapping("/obter-todos")
    public ResponseEntity<List<EntityModel<DocumentoResponseSemAnexoTextualDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) throws RuntimeException {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        List<EntityModel<DocumentoResponseSemAnexoTextualDto>> models = documentoService
                .getAllAsDtos(pageable).stream().map(this::toModel).toList();
        return ResponseEntity.ok(models);
    }

    @PutMapping("{id}/aprovar")
    public ResponseEntity<EntityModel<DocumentoResponseSemAnexoTextualDto>> setDocumentAsApproved(
            @PathVariable(value = "id") Long id) {
        return ResponseEntity.ok(toModel(documentoStatusService.approveDocument(id)));
    }

    @PatchMapping("{id}/status")
    public ResponseEntity<EntityModel<DocumentoResponseSemAnexoTextualDto>> changeStatus(
            @PathVariable(value = "id") Long id,
            @RequestBody @Valid DocumentoStatusRequestDto request) throws RuntimeException {
        DocumentoResponseSemAnexoTextualDto dto = documentoStatusService.changeStatus(id, request.getStatus());
        return ResponseEntity.ok(toModel(dto));
    }

    @PutMapping("{id}")
    public ResponseEntity<EntityModel<DocumentoResponseSemAnexoTextualDto>> update(
            @PathVariable(value = "id") Long id,
            @RequestBody @Valid DocumentoRequestUpdateDto request) throws RuntimeException {
        return ResponseEntity.ok(toModel(documentoService.update(id, request)));
    }

    @PutMapping("{idDocumento}/adicionar-item-anexo-parte-textual")
    public ResponseEntity<EntityModel<DocumentoResponseComAnexoTextualDto>> addItemAnexoParteNormativa(
            @PathVariable(value = "idDocumento") Long idDocumento,
            @RequestBody ItemAnexoParteNormativaRequestDto request) throws RuntimeException {
        DocumentoResponseComAnexoTextualDto dto = documentoParteNormativaService.adicionarItemAoDocumento(idDocumento, request);
        EntityModel<DocumentoResponseComAnexoTextualDto> model = EntityModel.of(dto,
                Link.of(BASE + "/" + idDocumento).withSelfRel(),
                Link.of(BASE + "/obter-todos").withRel("documentos")
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable(value = "id") Long id) throws RuntimeException {
        documentoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
