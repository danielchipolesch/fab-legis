package intraer.fablegis.infrastructure.configurations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

    private static final String ESQUEMA_BEARER = "bearer-jwt";

    @Bean
    OpenAPI customOpenApi() {
        OpenAPI openAPI = new OpenAPI()
                // Todo endpoint exige o access token OAuth2 (JWT) como Bearer: o botão "Authorize" da Swagger UI aceita o token
                // para "Try it out", e quem consome a API de fora vê no contrato que ela é autenticada.
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER))
                .info(new Info()
                        .title("FAB Legis - Controle de Documentação no COMAER")
                        .description("Backend da aplicação para controlar as legislações no COMAER")
                        .version("v1"));

        return openAPI;
    }
}