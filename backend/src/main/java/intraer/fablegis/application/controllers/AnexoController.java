package intraer.fablegis.application.controllers;

import intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto;
import intraer.fablegis.domain.services.AnexoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // Adicionar e remover seguem a mesma regra de edição do resto do documento (metadados, parte normativa): só quem
    // pode editá-lo -- autor ou coautor com papel EDIT, ou o revisor atribuído em EM_REVISAO. Listar fica livre para
    // qualquer usuário autenticado, como toda visualização (ver DocumentoAcessoService). A etapa é barrada no AnexoService.
    @PreAuthorize("@documentoAcessoService.podeEditar(#documentoId, authentication)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnexoResponseDto> adicionar(
            @PathVariable Long documentoId,
            @RequestParam("titulo") String titulo,
            @RequestParam("arquivo") MultipartFile arquivo,
            // RETRATO | PAISAGEM. Opcional: ausente, o serviço sugere pela proporção da imagem.
            @RequestParam(value = "orientacao", required = false) String orientacao) throws Exception {
        AnexoResponseDto dto = anexoService.adicionar(documentoId, titulo, arquivo, orientacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PreAuthorize("@documentoAcessoService.podeEditar(#documentoId, authentication)")
    @DeleteMapping("{anexoId}")
    public ResponseEntity<Void> remover(
            @PathVariable Long documentoId,
            @PathVariable Long anexoId) {
        anexoService.remover(documentoId, anexoId);
        return ResponseEntity.noContent().build();
    }
}
