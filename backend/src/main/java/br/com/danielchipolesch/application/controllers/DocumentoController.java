package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoHistoricoResponseDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestUpdateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.application.dtos.emendaDtos.MapaAlteracaoItemResponseDto;
import br.com.danielchipolesch.application.dtos.emendaDtos.MapaAlteracaoPdfRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.NumeracaoElementoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.SecoesSaveRequestDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.CompartilharDocumentoRequestDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.CompartilhamentoResponseDto;
import br.com.danielchipolesch.domain.entities.auditoria.AcaoAuditoriaEnum;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.domain.services.DocumentoCompartilhamentoService;
import br.com.danielchipolesch.domain.services.DocumentoHistoricoService;
import br.com.danielchipolesch.domain.services.DocumentoParteNormativaService;
import br.com.danielchipolesch.domain.services.DocumentoPdfService;
import br.com.danielchipolesch.domain.services.DocumentoPresencaService;
import br.com.danielchipolesch.domain.services.DocumentoService;
import br.com.danielchipolesch.domain.services.DocumentoStatusService;
import br.com.danielchipolesch.domain.services.EmendaService;
import br.com.danielchipolesch.domain.services.LogAuditoriaService;
import br.com.danielchipolesch.domain.services.MapaAlteracaoPdfService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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

    @Autowired
    private DocumentoHistoricoService documentoHistoricoService;

    @Autowired
    private DocumentoPdfService documentoPdfService;

    @Autowired
    private EmendaService emendaService;

    @Autowired
    private MapaAlteracaoPdfService mapaAlteracaoPdfService;

    @Autowired
    private DocumentoCompartilhamentoService compartilhamentoService;

    @Autowired
    private DocumentoPresencaService presencaService;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    private EntityModel<DocumentoResponseSemAnexoTextualDto> toModel(DocumentoResponseSemAnexoTextualDto dto) {
        Long id = dto.idDocumento();
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
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.CRIOU, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(dto));
    }

    @PostMapping("{id}/clonar")
    public ResponseEntity<EntityModel<DocumentoResponseSemAnexoTextualDto>> clone(
            @PathVariable(value = "id") Long id) throws RuntimeException {
        DocumentoResponseSemAnexoTextualDto dto = documentoService.clone(id);
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.CLONOU, "Clonado do documento " + id);
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(dto));
    }

    @GetMapping("{id}")
    public ResponseEntity<EntityModel<DocumentoResponseComAnexoTextualDto>> getById(
            @PathVariable(value = "id") Long id) throws RuntimeException {
        DocumentoResponseComAnexoTextualDto dto = documentoParteNormativaService.getDocumentoComAnexoTextualDtoById(id);
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.VISUALIZOU, null);
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
                .getAll(pageable).stream()
                .map(DocumentoMapper::documentoToDocumentoSemAnexoTextualResponseDto)
                .map(this::toModel)
                .toList();
        return ResponseEntity.ok(models);
    }

    @PreAuthorize("@documentoAcessoService.podeMudarStatus(#id, #request.status, authentication)")
    @PatchMapping("{id}/status")
    public ResponseEntity<EntityModel<DocumentoResponseSemAnexoTextualDto>> changeStatus(
            @PathVariable(value = "id") Long id,
            @RequestBody @Valid DocumentoStatusRequestDto request) throws RuntimeException {
        DocumentoResponseSemAnexoTextualDto dto = documentoStatusService.changeStatus(id, request);
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.MUDOU_STATUS,
                "Nova situação: " + request.status());
        return ResponseEntity.ok(toModel(dto));
    }

    @PreAuthorize("@documentoAcessoService.podeEditar(#id, authentication)")
    @PutMapping("{id}")
    public ResponseEntity<EntityModel<DocumentoResponseSemAnexoTextualDto>> update(
            @PathVariable(value = "id") Long id,
            @RequestBody @Valid DocumentoRequestUpdateDto request) throws RuntimeException {
        DocumentoResponseSemAnexoTextualDto dto = documentoService.update(id, request);
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.EDITOU, "Dados do documento");
        return ResponseEntity.ok(toModel(dto));
    }

    @PreAuthorize("@documentoAcessoService.podeEditar(#idDocumento, authentication)")
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

    @PreAuthorize("@documentoAcessoService.podeEditar(#id, authentication)")
    @PutMapping("{id}/secoes")
    public ResponseEntity<Void> saveSecoes(
            @PathVariable(value = "id") Long id,
            @RequestBody SecoesSaveRequestDto request) throws RuntimeException {
        documentoParteNormativaService.salvarSecoes(id, request);
        DocumentoResponseSemAnexoTextualDto dto = DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(
                documentoService.getById(id));
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.EDITOU, "Conteúdo do documento");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{id}/numeracao")
    public ResponseEntity<List<NumeracaoElementoResponseDto>> getNumeracao(
            @PathVariable(value = "id") Long id) {
        return ResponseEntity.ok(documentoParteNormativaService.listarNumeracao(id));
    }

    @GetMapping("com-historico-emenda")
    public ResponseEntity<List<Long>> getDocumentosComHistoricoEmenda() {
        return ResponseEntity.ok(emendaService.listarDocumentosComHistorico());
    }

    @GetMapping("{id}/historico")
    public ResponseEntity<List<DocumentoHistoricoResponseDto>> getHistorico(
            @PathVariable(value = "id") Long id) {
        return ResponseEntity.ok(documentoHistoricoService.listarPorDocumento(id));
    }

    @GetMapping("{id}/mapa-alteracao")
    public ResponseEntity<List<MapaAlteracaoItemResponseDto>> getMapaAlteracao(
            @PathVariable(value = "id") Long id) {
        return ResponseEntity.ok(emendaService.listarMapaAlteracao(id));
    }

    @PostMapping(value = "{id}/mapa-alteracao/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> getMapaAlteracaoPdf(
            @PathVariable(value = "id") Long id,
            @RequestBody MapaAlteracaoPdfRequestDto request) {
        byte[] pdfBytes = mapaAlteracaoPdfService.gerarPdf(request);
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"mapa-alteracao-" + id + ".pdf\"")
                .header("Cache-Control", "no-store")
                .body(pdfBytes);
    }

    @GetMapping(value = "{id}/pdf", produces = "application/pdf")
    public ResponseEntity<StreamingResponseBody> getPdf(@PathVariable(value = "id") Long id) {
        StreamingResponseBody body = documentoPdfService.streamPdf(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"documento-" + id + ".pdf\"")
                .header("Cache-Control", "no-store")
                .body(body);
    }

    @PreAuthorize("@documentoAcessoService.podeExcluir(#id, authentication)")
    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable(value = "id") Long id) throws RuntimeException {
        DocumentoResponseSemAnexoTextualDto dto = DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(
                documentoService.getById(id));
        documentoService.delete(id);
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.EXCLUIU, null);
        return ResponseEntity.noContent().build();
    }

    // ─── Compartilhamento (coautoria) ──────────────────────────────────────────

    @PreAuthorize("@documentoAcessoService.podeCompartilhar(#id, authentication)")
    @GetMapping("{id}/compartilhamentos")
    public ResponseEntity<List<CompartilhamentoResponseDto>> listarCompartilhamentos(
            @PathVariable(value = "id") Long id) {
        return ResponseEntity.ok(compartilhamentoService.listar(id));
    }

    @PreAuthorize("@documentoAcessoService.podeCompartilhar(#id, authentication)")
    @PostMapping("{id}/compartilhamentos")
    public ResponseEntity<CompartilhamentoResponseDto> compartilhar(
            @PathVariable(value = "id") Long id,
            @RequestBody @Valid CompartilharDocumentoRequestDto request) {
        CompartilhamentoResponseDto resultado = compartilhamentoService.compartilhar(id, request);
        DocumentoResponseSemAnexoTextualDto dto = DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(
                documentoService.getById(id));
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.COMPARTILHOU,
                "Compartilhado com " + resultado.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PreAuthorize("@documentoAcessoService.podeCompartilhar(#id, authentication)")
    @DeleteMapping("{id}/compartilhamentos/{usuarioId}")
    public ResponseEntity<Void> removerCompartilhamento(
            @PathVariable(value = "id") Long id,
            @PathVariable(value = "usuarioId") Long usuarioId) {
        compartilhamentoService.remover(id, usuarioId);
        DocumentoResponseSemAnexoTextualDto dto = DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(
                documentoService.getById(id));
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.REMOVEU_COMPARTILHAMENTO, null);
        return ResponseEntity.noContent().build();
    }

    // ─── Presença de edição (aviso de edição concorrente, via SSE) ─────────────

    @PreAuthorize("@documentoAcessoService.podeEditar(#id, authentication)")
    @GetMapping(value = "{id}/presenca/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPresenca(@PathVariable(value = "id") Long id) {
        return presencaService.conectar(id);
    }
}
