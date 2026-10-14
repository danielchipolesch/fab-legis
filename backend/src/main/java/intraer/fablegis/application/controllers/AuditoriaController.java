package intraer.fablegis.application.controllers;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.dtos.auditoriaDtos.LogAuditoriaResponseDto;
import intraer.fablegis.domain.entities.auditoria.AcaoAuditoriaEnum;
import intraer.fablegis.domain.entities.auditoria.AcaoDeAcessoEnum;
import intraer.fablegis.application.dtos.auditoriaDtos.LogDeAcessoResponseDto;
import intraer.fablegis.domain.services.AuditoriaDeAcessoService;
import intraer.fablegis.domain.services.LogAuditoriaService;
import intraer.fablegis.domain.regras.EscopoDeAcesso;
import intraer.fablegis.infrastructure.security.AutenticacaoUtil;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.tags.Tag;
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
// AUDITOR. Perfil independente de ADMIN: um admin só enxerga a auditoria
// se também tiver o perfil de auditor marcado no próprio cadastro.
@RestController
@RequestMapping(value = "/v1/auditoria", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auditoria", description = "Log de acesso/ações a nível de documento")
@PreAuthorize("hasAnyRole('AUD_OM', 'AUD_GER')")
@RequiredArgsConstructor
public class AuditoriaController {

    private final LogAuditoriaService logAuditoriaService;
    private final AuditoriaDeAcessoService auditoriaDeAcessoService;

    @GetMapping
    public ResponseEntity<Page<LogAuditoriaResponseDto>> filtrar(
            @RequestParam(required = false) Long documentoId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AcaoAuditoriaEnum acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size, Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dtOcorrencia").descending());
        Timestamp inicio = dataInicio != null ? Timestamp.valueOf(dataInicio) : null;
        Timestamp fim = dataFim != null ? Timestamp.valueOf(dataFim) : null;
        var ator = AutenticacaoUtil.usuarioDe(authentication);
        Long omDoEscopo = EscopoDeAcesso.temEscopoGeral(ator) ? null : ator.getOm().getId();
        return ResponseEntity.ok(logAuditoriaService.filtrar(documentoId, usuarioId, acao, inicio, fim, omDoEscopo, pageable));
    }

    // Trilha dos acessos: quem criou usuário, concedeu ou retirou perfil, ativou/desativou, trocou de OM ou redefiniu senha.
    @GetMapping("/acessos")
    public ResponseEntity<Page<LogDeAcessoResponseDto>> acessos(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) AcaoDeAcessoEnum acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size, Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dtOcorrencia").descending());
        Timestamp inicio = dataInicio != null ? Timestamp.valueOf(dataInicio) : null;
        Timestamp fim = dataFim != null ? Timestamp.valueOf(dataFim) : null;
        var ator = AutenticacaoUtil.usuarioDe(authentication);
        String omDoEscopo = EscopoDeAcesso.temEscopoGeral(ator) ? null : ator.getOm().getSigla();
        return ResponseEntity.ok(auditoriaDeAcessoService.filtrar(cpf, acao, inicio, fim, omDoEscopo, pageable));
    }
}
