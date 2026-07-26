package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.domain.services.ImagemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/v1/imagens")
public class ImagemController {

    @Autowired
    private ImagemService imagemService;

    @PostMapping("/upload")
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
}
