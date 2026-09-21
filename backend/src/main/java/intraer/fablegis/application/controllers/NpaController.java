package intraer.fablegis.application.controllers;

import intraer.fablegis.application.dtos.npaDtos.CamposDaNpaDto;
import intraer.fablegis.domain.entities.auditoria.AcaoAuditoriaEnum;
import intraer.fablegis.domain.regras.comunicacaooficialpadronizada.CamposDeNpa;
import intraer.fablegis.domain.services.DocumentoService;
import intraer.fablegis.domain.services.LogAuditoriaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Campos do cabeçalho e do fecho que só a NPA tem (setor emissor, local, assinaturas). Só documentos de espécie NPA
// respondem; para os demais é 404.
@RestController
@RequestMapping(value = "/v1/documentos/{id}/npa", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "NPA", description = "Campos específicos da Norma Padrão de Ação")
public class NpaController {

    @Autowired
    private CamposDeNpa camposDeNpa;

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    // Sem @PreAuthorize: visualizar é liberado para qualquer usuário autenticado (a distribuição da NPA é ostensiva).
    @GetMapping
    public ResponseEntity<CamposDaNpaDto> obter(@PathVariable("id") Long id) {
        return ResponseEntity.ok(camposDeNpa.obter(id));
    }

    @PreAuthorize("@documentoAcessoService.podeEditar(#id, authentication)")
    @PutMapping
    public ResponseEntity<CamposDaNpaDto> atualizar(@PathVariable("id") Long id,
                                                    @RequestBody @Valid CamposDaNpaDto request) {
        CamposDaNpaDto salvos = camposDeNpa.atualizar(id, request);
        var documento = documentoService.getById(id);
        logAuditoriaService.registrar(id, documento.getIdentificacao(), AcaoAuditoriaEnum.EDITOU,
                "Cabeçalho e assinaturas da NPA");
        return ResponseEntity.ok(salvos);
    }
}
