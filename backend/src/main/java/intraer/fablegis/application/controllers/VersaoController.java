package intraer.fablegis.application.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Versão do sistema (a do pom.xml, gravada em build-info no empacotamento). Pública: aparece na tela de login.
@RestController
@RequestMapping(value = "/v1/versao", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Versão", description = "Versão do sistema em execução")
public class VersaoController {

    static final String DESCONHECIDA = "desconhecida";

    private final String versao;

    public VersaoController(ObjectProvider<BuildProperties> build) {
        BuildProperties props = build.getIfAvailable();
        this.versao = props != null ? props.getVersion() : DESCONHECIDA;
    }

    @GetMapping
    public Map<String, String> versao() {
        return Map.of("versao", versao);
    }
}
