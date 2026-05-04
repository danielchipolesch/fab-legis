package br.com.danielchipolesch.infrastructure.configurations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI customOpenApi() {
        OpenAPI openAPI = new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("FAB Legis - Controle de Documentação no COMAER")
                        .description("Backend da aplicação para controlar as legislações no COMAER")
                        .version("v1"));

        return openAPI;
    }
}