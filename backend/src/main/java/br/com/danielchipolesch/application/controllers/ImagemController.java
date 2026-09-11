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

    // "urlAssinada" evita o round-trip extra a /urls-assinadas logo após o upload --
    // quem exibe a imagem pela primeira vez (ex. FigureView) já recebe uma URL pronta
    // pra usar, em vez de ter que resolver a "url" canônica antes de poder mostrar
    // algo. A "url" continua sendo a única persistida no documento (a assinada expira
    // em ImagemService.EXPIRY_MINUTES).
    @PostMapping(value = "/upload", produces = "application/json")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("arquivo") MultipartFile arquivo) {
        try {
            String url = imagemService.uploadImagem(arquivo);
            String urlAssinada = imagemService.gerarUrlAssinada(url);
            return ResponseEntity.ok(urlAssinada != null
                    ? Map.of("url", url, "urlAssinada", urlAssinada)
                    : Map.of("url", url));
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
