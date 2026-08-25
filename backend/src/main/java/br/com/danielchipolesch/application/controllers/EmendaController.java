package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.emendaDtos.EmendaElementoRequestDto;
import br.com.danielchipolesch.application.dtos.emendaDtos.EmendaIncluirRequestDto;
import br.com.danielchipolesch.domain.entities.auditoria.AcaoAuditoriaEnum;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.domain.services.DocumentoService;
import br.com.danielchipolesch.domain.services.EmendaService;
import br.com.danielchipolesch.domain.services.LogAuditoriaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/v1/documentos/{docId}/emendar", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Emenda", description = "Alteração, revogação e inclusão de elementos em documentos publicados")
public class EmendaController {

    @Autowired
    private EmendaService emendaService;

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    private void registrarEdicao(Long docId, String detalhe) {
        var dto = DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(documentoService.getById(docId));
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.EDITOU, detalhe);
    }

    /**
     * Altera, revoga ou desfaz emenda de um elemento existente.
     * secao: PARTE_PRELIMINAR | PARTE_NORMATIVA | PARTE_FINAL
     */
    @PreAuthorize("@documentoAcessoService.podeEditar(#docId, authentication)")
    @PatchMapping("/{secao}/{elementoId}")
    public ResponseEntity<Void> emendar(
            @PathVariable Long docId,
            @PathVariable String secao,
            @PathVariable Long elementoId,
            @RequestBody @Valid EmendaElementoRequestDto request) {
        emendaService.emendar(docId, secao, elementoId, request);
        registrarEdicao(docId, "Emenda (" + request.acao() + ") em " + secao);
        return ResponseEntity.noContent().build();
    }

    /**
     * Inclui novo elemento no documento em alteração, marcado como INCLUIDO.
     * secao: PARTE_PRELIMINAR | PARTE_NORMATIVA | PARTE_FINAL
     */
    @PreAuthorize("@documentoAcessoService.podeEditar(#docId, authentication)")
    @PostMapping("/{secao}")
    public ResponseEntity<Void> incluir(
            @PathVariable Long docId,
            @PathVariable String secao,
            @RequestBody @Valid EmendaIncluirRequestDto request) {
        emendaService.incluir(docId, secao, request);
        registrarEdicao(docId, "Inclusão de elemento em " + secao);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reordena um artigo incluído por emenda (ainda não aprovado) entre seus irmãos.
     * Só é permitido trocar de posição com outro artigo também incluído — nunca com
     * um artigo original (INALTERADO/ALTERADO/REVOGADO), cuja renumeração é vedada.
     * direcao: CIMA | BAIXO
     */
    @PreAuthorize("@documentoAcessoService.podeEditar(#docId, authentication)")
    @PatchMapping("/{secao}/{elementoId}/reordenar")
    public ResponseEntity<Void> reordenar(
            @PathVariable Long docId,
            @PathVariable String secao,
            @PathVariable Long elementoId,
            @RequestParam String direcao) {
        emendaService.reordenarIncluido(docId, secao, elementoId, direcao);
        return ResponseEntity.noContent().build();
    }
}
