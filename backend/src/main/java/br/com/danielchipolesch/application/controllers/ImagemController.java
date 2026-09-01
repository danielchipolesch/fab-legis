package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.domain.services.ImagemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/imagens")
public class ImagemController {

    @Autowired
    private ImagemService imagemService;

    @PostMapping(value = "/upload", produces = "application/json")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("arquivo") MultipartFile arquivo) {
        try {
            String url = imagemService.uploadImagem(arquivo);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    public record UrlsAssinadasRequestDto(List<String> urls) { }

    // Bucket do MinIO é privado (ver ImagemService.garantirBucket) -- o navegador
    // nunca acessa o objeto diretamente, precisa passar por aqui pra trocar a URL
    // "canônica" armazenada (em conteúdo TipTap, urlPdf, anexo, portaria...) por uma
    // URL assinada de curta duração. Endpoint universal (sem @PreAuthorize), mesmo
    // raciocínio de DocumentoAcessoService: visualizar é liberado pra qualquer
    // usuário autenticado -- ver docs/autenticacao.md.
    @PostMapping(value = "/urls-assinadas", produces = "application/json")
    public ResponseEntity<Map<String, String>> urlsAssinadas(@RequestBody UrlsAssinadasRequestDto request) {
        return ResponseEntity.ok(imagemService.gerarUrlsAssinadas(request.urls()));
    }
}
