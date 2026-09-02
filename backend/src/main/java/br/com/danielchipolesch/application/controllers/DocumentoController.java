package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoHistoricoResponseDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestUpdateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResumoResponseDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.PortariaPdfResponseDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.PortariaPublicacaoResponseDto;
import br.com.danielchipolesch.application.dtos.emendaDtos.MapaAlteracaoItemResponseDto;
import br.com.danielchipolesch.application.dtos.emendaDtos.MapaAlteracaoPdfRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ElementoConteudoRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.NumeracaoElementoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.SecoesSaveRequestDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.CompartilharDocumentoRequestDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.CompartilhamentoResponseDto;
import br.com.danielchipolesch.domain.entities.auditoria.AcaoAuditoriaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.domain.services.DocumentoCompartilhamentoService;
import br.com.danielchipolesch.domain.services.DocumentoHistoricoService;
import br.com.danielchipolesch.domain.services.DocumentoParteNormativaService;
import br.com.danielchipolesch.domain.services.DocumentoPdfService;
import br.com.danielchipolesch.domain.services.DocumentoPresencaService;
import br.com.danielchipolesch.domain.services.DocumentoService;
import br.com.danielchipolesch.domain.services.DocumentoStatusService;
import br.com.danielchipolesch.domain.services.EmendaService;
import br.com.danielchipolesch.domain.services.ImagemService;
import br.com.danielchipolesch.domain.services.LogAuditoriaService;
import br.com.danielchipolesch.domain.services.MapaAlteracaoPdfService;
import br.com.danielchipolesch.domain.services.PortariaPublicacaoService;
import br.com.danielchipolesch.infrastructure.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;
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

    @Autowired
    private ImagemService imagemService;

    @Autowired
    private PortariaPublicacaoService portariaPublicacaoService;

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
    public ResponseEntity<List<EntityModel<DocumentoResponseSemAnexoTextualDto>>> getByEspecieNormativaAndAssuntoBasico(
            @RequestParam(value = "especie-normativa") Long especieNormativaId,
            @RequestParam(value = "assunto-basico") Long assuntoBasicoId) throws RuntimeException {
        List<EntityModel<DocumentoResponseSemAnexoTextualDto>> models = documentoService
                .getByEspecieNormativaAndAssuntoBasico(especieNormativaId, assuntoBasicoId)
                .stream().map(this::toModel).toList();
        return ResponseEntity.ok(models);
    }

    // Paginação de verdade: até aqui, o frontend chamava isso uma vez com size=200 e
    // filtrava/paginava tudo no navegador (HomePage.vue) -- acima de 200 documentos no
    // acervo, o resto simplesmente nunca aparecia. Devolve Page<T> direto (sem
    // EntityModel por item, igual a AuditoriaController.filtrar) -- o frontend nunca leu
    // _links dos itens da listagem, só os campos planos do DTO, que o Jackson já
    // serializava assim mesmo dentro de EntityModel.
    @GetMapping("/obter-todos")
    public ResponseEntity<Page<DocumentoResponseSemAnexoTextualDto>> getAll(
            @RequestParam(required = false) String aba,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String especieSigla,
            @RequestParam(required = false) DocumentoStatusEnum status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "dtCriacao") String sortBy,
            @RequestParam(defaultValue = "true") boolean descending,
            Authentication authentication) throws RuntimeException {
        Usuario usuario = ((UsuarioPrincipal) authentication.getPrincipal()).getUsuario();
        Sort sort = descending ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Page<Documento> resultado = documentoService.getAllPaginado(
                usuario.getId(), usuario.getOm().getId(), aba, busca, especieSigla, status,
                PageRequest.of(page, size, sort));
        return ResponseEntity.ok(resultado.map(DocumentoMapper::documentoToDocumentoSemAnexoTextualResponseDto));
    }

    // Contagens pros badges das 4 abas e pros chips de situação da HomePage -- ver
    // DocumentoService.getResumo. Mesmos filtros de busca/espécie do getAll acima, pra
    // ficar em sincronia com o que a listagem principal está mostrando no momento.
    @GetMapping("/resumo")
    public ResponseEntity<DocumentoResumoResponseDto> resumo(
            @RequestParam(required = false) String aba,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String especieSigla,
            Authentication authentication) {
        Usuario usuario = ((UsuarioPrincipal) authentication.getPrincipal()).getUsuario();
        return ResponseEntity.ok(documentoService.getResumo(
                usuario.getId(), usuario.getOm().getId(), aba, busca, especieSigla));
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

    // Upload do PDF da portaria de publicação -- separado do PATCH .../status
    // porque acontece ANTES do usuário confirmar a publicação (ver
    // HomePage.vue): só sobe o arquivo e devolve a URL, sem mudar status nem
    // gravar nada no documento. A URL só é persistida quando o formulário de
    // publicação é de fato enviado.
    @PreAuthorize("@documentoAcessoService.podeEditar(#id, authentication)")
    @PostMapping(value = "{id}/portaria-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PortariaPdfResponseDto> uploadPortariaPdf(
            @PathVariable(value = "id") Long id,
            @RequestParam("arquivo") MultipartFile arquivo) throws Exception {
        String url = imagemService.uploadPdf(arquivo.getBytes(),
                "portaria-" + id + "-" + Instant.now().toEpochMilli() + ".pdf");
        return ResponseEntity.ok(new PortariaPdfResponseDto(url));
    }

    // Sem @PreAuthorize: visualizar é liberado para qualquer usuário
    // autenticado, mesmo raciocínio de DocumentoAcessoService para o resto da
    // tela de visualização.
    @GetMapping("{id}/portarias")
    public ResponseEntity<List<PortariaPublicacaoResponseDto>> listarPortarias(
            @PathVariable(value = "id") Long id) {
        return ResponseEntity.ok(portariaPublicacaoService.listar(id));
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

    // PATCH, não PUT: salvarSecoes aplica um diff contra a árvore persistida (casando
    // por id) em vez de apagar/reinserir tudo -- ver comentário em
    // DocumentoParteNormativaService.salvarItensNormativos. Trocar só o verbo sem essa
    // mudança de comportamento não protegeria nada; é a semântica de fato que importa.
    // Retorna a árvore normativa persistida (com os ids reais dos elementos recém-
    // criados) porque o frontend manda elementos novos sem id -- sem devolver o id
    // atribuído, o próximo autosave os trataria como novos de novo, duplicando-os.
    @PreAuthorize("@documentoAcessoService.podeEditar(#id, authentication)")
    @PatchMapping("{id}/secoes")
    public ResponseEntity<List<ItemAnexoParteNormativaResponseDto>> saveSecoes(
            @PathVariable(value = "id") Long id,
            @RequestBody SecoesSaveRequestDto request,
            // Id de sessão gerado uma vez por aba no frontend (ver frontend/src/utils/
            // clientId.js) -- devolvido no broadcast SSE (event: estrutura) pra quem
            // originou a mudança poder ignorar o próprio eco. Opcional: sem ele, o
            // broadcast simplesmente não tem como ser filtrado pelo emissor.
            @RequestHeader(value = "X-Client-Id", required = false) String clientId) throws RuntimeException {
        documentoParteNormativaService.salvarSecoes(id, request, clientId);
        DocumentoResponseSemAnexoTextualDto dto = DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(
                documentoService.getById(id));
        logAuditoriaService.registrar(dto.idDocumento(), dto.codigoDocumento(), AcaoAuditoriaEnum.EDITOU, "Conteúdo do documento");
        List<ItemAnexoParteNormativaResponseDto> normativos = documentoParteNormativaService
                .getItensNormativosByDocumento(id).stream().map(ItemAnexoParteNormativaResponseDto::from).toList();
        return ResponseEntity.ok(normativos);
    }

    // Grava só o conteudo de UM elemento -- ponto de escrita usado pelo serviço de
    // colaboração (Hocuspocus) a cada persistência do Y.Doc, nunca pelo autosave
    // estrutural acima. Ver plano de colaboração em tempo real (CRDT/Yjs).
    @PreAuthorize("@documentoAcessoService.podeEditar(#id, authentication)")
    @PatchMapping("{id}/elementos/{elementoId}/conteudo")
    public ResponseEntity<Void> atualizarConteudoElemento(
            @PathVariable(value = "id") Long id,
            @PathVariable(value = "elementoId") Long elementoId,
            @RequestBody ElementoConteudoRequestDto request) {
        documentoParteNormativaService.atualizarConteudoElemento(id, elementoId, request.conteudo());
        return ResponseEntity.noContent().build();
    }

    // Sem corpo de resposta -- existe só para o serviço de colaboração (Hocuspocus)
    // perguntar, com o JWT de quem está se conectando, "esta pessoa pode editar este
    // documento?" antes de aceitar a conexão a uma sala Yjs. 204 = pode; o
    // @PreAuthorize barra com 403 antes mesmo de o método rodar, caso contrário.
    @PreAuthorize("@documentoAcessoService.podeEditar(#id, authentication)")
    @GetMapping("{id}/pode-editar")
    public ResponseEntity<Void> podeEditar(@PathVariable(value = "id") Long id) {
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
