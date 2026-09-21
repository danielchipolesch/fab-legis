package intraer.fablegis.application.controllers;

import intraer.fablegis.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.mappers.DocumentoMapper;
import intraer.fablegis.domain.services.DocumentoCompartilhamentoService;
import intraer.fablegis.domain.services.DocumentoService;
import intraer.fablegis.infrastructure.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

// Os quatro cards do hub (tela inicial do sistema), de qualquer módulo -- ver DocumentoSpecifications: os documentos em
// tramitação de quem chama (autoria ou coautoria), os que aguardam a ação dele (como revisor ou publicador), os em
// tramitação de outras pessoas (qualquer OM) e os publicados e revogados de todas as OMs, sem tramitação. Visualizar é liberado a qualquer usuário autenticado, como no resto do acervo;
// só que cada item é o mesmo DTO da listagem do módulo, para a tela decidir a ação de cada linha.
@RestController
@RequestMapping(value = "/v1/painel", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Painel", description = "Cards do hub: minhas em tramitação, aguardando ação, em tramitação nas OMs, publicados e revogados")
public class PainelController {

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private DocumentoCompartilhamentoService compartilhamentoService;

    // Mais recentemente mexidos primeiro.
    private static PageRequest pagina(int page, int size) {
        return PageRequest.of(page, size, Sort.by("dtAlteracao").descending().and(Sort.by("id").descending()));
    }

    @GetMapping("/minhas-em-tramitacao")
    public ResponseEntity<Page<DocumentoResponseSemAnexoTextualDto>> minhasEmTramitacao(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "8") int size,
            Authentication authentication) {
        Usuario usuario = usuario(authentication);
        return ResponseEntity.ok(comPosse(documentoService.getPainelMinhasEmTramitacao(usuario.getId(), pagina(page, size)), usuario));
    }

    @GetMapping("/aguardando-acao")
    public ResponseEntity<Page<DocumentoResponseSemAnexoTextualDto>> aguardandoAcao(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "8") int size,
            Authentication authentication) {
        Usuario usuario = usuario(authentication);
        return ResponseEntity.ok(comPosse(documentoService.getPainelAguardandoAcao(usuario.getId(), pagina(page, size)), usuario));
    }

    @GetMapping("/em-tramitacao-de-outros")
    public ResponseEntity<Page<DocumentoResponseSemAnexoTextualDto>> emTramitacaoDeOutros(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "8") int size,
            Authentication authentication) {
        Usuario usuario = usuario(authentication);
        return ResponseEntity.ok(comPosse(documentoService.getPainelEmTramitacaoDeOutros(usuario.getId(), pagina(page, size)), usuario));
    }

    @GetMapping("/publicados-e-revogados")
    public ResponseEntity<Page<DocumentoResponseSemAnexoTextualDto>> publicadosERevogados(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "8") int size,
            Authentication authentication) {
        Usuario usuario = usuario(authentication);
        return ResponseEntity.ok(comPosse(documentoService.getPainelPublicadosERevogados(pagina(page, size)), usuario));
    }

    private static Usuario usuario(Authentication authentication) {
        return ((UsuarioPrincipal) authentication.getPrincipal()).getUsuario();
    }

    // 1 consulta para a página inteira (não 1 por linha): marca em quais o usuário é autor ou coautor.
    private Page<DocumentoResponseSemAnexoTextualDto> comPosse(Page<Documento> resultado, Usuario usuario) {
        List<Long> ids = resultado.getContent().stream().map(Documento::getId).toList();
        Set<Long> coautorDe = compartilhamentoService.listarIdsCompartilhadosComUsuario(usuario.getId(), ids);
        return resultado.map(doc -> DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(
                doc, doc.getAutor().getId().equals(usuario.getId()) || coautorDe.contains(doc.getId())));
    }
}
