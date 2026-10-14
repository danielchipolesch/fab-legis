package intraer.fablegis.application.controllers;

import intraer.fablegis.application.dtos.especieNormativaDtos.EspecieNormativaResponseDto;
import intraer.fablegis.application.dtos.habilitacaoDtos.HabilitacaoDaEspecieRequestDto;
import intraer.fablegis.application.dtos.habilitacaoDtos.HabilitacaoDaEspecieResponseDto;
import intraer.fablegis.application.dtos.habilitacaoDtos.HabilitacaoDaOmRequestDto;
import intraer.fablegis.application.dtos.habilitacaoDtos.HabilitacaoDaOmResponseDto;
import intraer.fablegis.domain.services.HabilitacaoDeCriacaoService;
import intraer.fablegis.infrastructure.security.AutenticacaoUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Habilitação de documentos: quais espécies convencionais cada OM pode criar. Quem gerencia é o administrador (CENDOC,
// papel ADMIN); qualquer pessoa autenticada consulta o que a SUA OM pode criar (a tela esconde o botão de criar do resto).
@RestController
@RequestMapping(value = "/v1/habilitacao-de-documentos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Habilitação de documentos", description = "Quais espécies normativas cada OM pode criar")
@RequiredArgsConstructor
public class HabilitacaoDeDocumentosController {

    private final HabilitacaoDeCriacaoService habilitacaoService;

    @PreAuthorize("hasRole('ADM_GER')")
    @GetMapping
    public ResponseEntity<List<HabilitacaoDaOmResponseDto>> listar() {
        return ResponseEntity.ok(habilitacaoService.listar());
    }

    @PreAuthorize("hasRole('ADM_GER')")
    @PutMapping("/{omId}")
    public ResponseEntity<HabilitacaoDaOmResponseDto> definir(@PathVariable Long omId,
                                                              @RequestBody @Valid HabilitacaoDaOmRequestDto pedido) {
        return ResponseEntity.ok(habilitacaoService.definir(omId, pedido.especieIds()));
    }

    // A visão por espécie (a tela de administração): as OMs habilitadas a criar uma espécie convencional.
    @PreAuthorize("hasRole('ADM_GER')")
    @PutMapping("/por-especie/{especieId}")
    public ResponseEntity<HabilitacaoDaEspecieResponseDto> definirPorEspecie(@PathVariable Long especieId,
                                                                             @RequestBody @Valid HabilitacaoDaEspecieRequestDto pedido) {
        return ResponseEntity.ok(habilitacaoService.definirPorEspecie(especieId, pedido.omIds()));
    }

    // As espécies que a OM de quem pergunta pode criar (as que não dependem de habilitação, como a NPA, mais as habilitadas).
    @GetMapping("/minhas")
    public ResponseEntity<List<EspecieNormativaResponseDto>> minhas() {
        var om = AutenticacaoUtil.usuarioAtual().getOm();
        return ResponseEntity.ok(habilitacaoService.especiesQuePodeCriar(om == null ? null : om.getId()));
    }
}
