package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.auditoriaDtos.LogAuditoriaResponseDto;
import br.com.danielchipolesch.domain.entities.auditoria.AcaoAuditoriaEnum;
import br.com.danielchipolesch.domain.services.LogAuditoriaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;

// Trilha de auditoria (quem viu/editou/excluiu/mudou status de cada
// documento, quando -- ver LogAuditoriaService) -- leitura exclusiva de
// AUDITOR. Papel independente de ADMIN: um admin só enxerga a auditoria
// se também tiver o papel de auditor marcado no próprio cadastro.
@RestController
@RequestMapping(value = "/v1/auditoria", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auditoria", description = "Log de acesso/ações a nível de documento")
@PreAuthorize("hasRole('AUDITOR')")
public class AuditoriaController {

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    @GetMapping
    public ResponseEntity<Page<LogAuditoriaResponseDto>> filtrar(
            @RequestParam(required = false) Long documentoId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AcaoAuditoriaEnum acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dtOcorrencia").descending());
        Timestamp inicio = dataInicio != null ? Timestamp.valueOf(dataInicio) : null;
        Timestamp fim = dataFim != null ? Timestamp.valueOf(dataFim) : null;
        return ResponseEntity.ok(logAuditoriaService.filtrar(documentoId, usuarioId, acao, inicio, fim, pageable));
    }
}
