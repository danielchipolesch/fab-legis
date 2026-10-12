package intraer.fablegis.infrastructure.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class CorsConfig implements WebMvcConfigurer{

    // Mesma lista de SecurityConfig.corsConfigurationSource (que o Spring Security aplica primeiro): APP_CORS_ALLOWED_ORIGINS.
    @Value("${app.cors.allowed-origins:}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry
                .addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("POST", "PUT", "PATCH", "DELETE", "GET", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("ETag");
    }
}
