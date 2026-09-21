package intraer.fablegis.application.controllers;

import intraer.fablegis.application.dtos.buscaDtos.ItemBuscaResponseDto;
import intraer.fablegis.domain.services.DocumentoBuscaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Busca full-text sobre o conteúdo dos dispositivos (Artigo/Parágrafo/
// Inciso/...), não só metadados do documento -- ver docs/funcionalidades.md.
// Sem @PreAuthorize de propósito, mesmo padrão de GET /{id}: visualizar é
// liberado a qualquer usuário autenticado, de qualquer OM, em qualquer
// situação (ver DocumentoAcessoService) -- a busca não é mais restritiva que
// abrir o documento diretamente.
@RestController
@RequestMapping(value = "/v1/documentos/busca", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Busca")
public class DocumentoBuscaController {

    @Autowired
    private DocumentoBuscaService documentoBuscaService;

    @GetMapping
    public Page<ItemBuscaResponseDto> buscar(
            @RequestParam("q") String termo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return documentoBuscaService.buscar(termo, PageRequest.of(page, size));
    }
}
