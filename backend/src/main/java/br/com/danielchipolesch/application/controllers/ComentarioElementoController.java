package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.comentarioDtos.ComentarioElementoCreateRequestDto;
import br.com.danielchipolesch.application.dtos.comentarioDtos.ComentarioElementoResponseDto;
import br.com.danielchipolesch.domain.entities.auditoria.AcaoAuditoriaEnum;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.domain.services.ComentarioElementoService;
import br.com.danielchipolesch.domain.services.DocumentoService;
import br.com.danielchipolesch.domain.services.LogAuditoriaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Comentários em linha sobre um elemento (artigo, parágrafo, inciso...) -- revisão
// assíncrona sem editar o texto do elemento, ver ComentarioElementoService e
// docs/funcionalidades.md. Todas as rotas exigem o mesmo conjunto de posse
// (autor/coautor/revisor ou publicador atribuído -- DocumentoAcessoService.podeComentar).
@RestController
@RequestMapping(value = "/v1/documentos/{docId}/comentarios", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Comentário", description = "Comentários em linha sobre elementos do documento")
public class ComentarioElementoController {

    @Autowired
    private ComentarioElementoService comentarioService;

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    @PreAuthorize("@documentoAcessoService.podeComentar(#docId, authentication)")
    @GetMapping
    public ResponseEntity<List<ComentarioElementoResponseDto>> listar(@PathVariable Long docId) {
        return ResponseEntity.ok(comentarioService.listar(docId));
    }

    @PreAuthorize("@documentoAcessoService.podeComentar(#docId, authentication)")
    @PostMapping
    public ResponseEntity<ComentarioElementoResponseDto> criar(
            @PathVariable Long docId, @RequestBody @Valid ComentarioElementoCreateRequestDto request) {
        var criado = comentarioService.criar(docId, request);
        var dto = DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(documentoService.getById(docId));
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.COMENTOU,
                "Comentário no elemento " + criado.elementoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PreAuthorize("@documentoAcessoService.podeComentar(#docId, authentication)")
    @PatchMapping("{comentarioId}/resolver")
    public ResponseEntity<ComentarioElementoResponseDto> resolver(
            @PathVariable Long docId, @PathVariable Long comentarioId) {
        return ResponseEntity.ok(comentarioService.marcarResolvido(docId, comentarioId, true));
    }

    @PreAuthorize("@documentoAcessoService.podeComentar(#docId, authentication)")
    @PatchMapping("{comentarioId}/reabrir")
    public ResponseEntity<ComentarioElementoResponseDto> reabrir(
            @PathVariable Long docId, @PathVariable Long comentarioId) {
        return ResponseEntity.ok(comentarioService.marcarResolvido(docId, comentarioId, false));
    }
}
