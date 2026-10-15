package intraer.fablegis.application.controllers;

import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto;
import intraer.fablegis.domain.regras.TipoDeEspecie;
import intraer.fablegis.domain.services.PainelGerencialService;
import intraer.fablegis.infrastructure.security.AutenticacaoUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Painel Gerencial: os indicadores da gestão normativa -- só para quem tem o papel do Painel Gerencial (da OM ou Geral). A OM pela qual se filtra
// é decidida no servidor (EscopoDoPainel): o Painel Gerencial da OM nunca vê além da própria.
@RestController
@RequestMapping(value = "/v1/painel-gerencial", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Painel Gerencial", description = "Indicadores da gestão normativa, por OM")
@PreAuthorize("hasAnyRole('PNL_OM', 'PNL_GER')")
@RequiredArgsConstructor
public class PainelGerencialController {

    private final PainelGerencialService painelGerencialService;

    @GetMapping
    public ResponseEntity<PainelGerencialResponseDto> indicadores(
            @RequestParam(required = false) Long omId,
            @RequestParam(required = false) Long especieId,
            @RequestParam(required = false) TipoDeEspecie tipoDeEspecie,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        return ResponseEntity.ok(painelGerencialService.indicadores(
                AutenticacaoUtil.usuarioDe(authentication), omId, especieId, tipoDeEspecie, dataInicio, dataFim));
    }
}
