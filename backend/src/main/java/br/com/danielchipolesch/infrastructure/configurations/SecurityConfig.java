package br.com.danielchipolesch.infrastructure.configurations;

import br.com.danielchipolesch.infrastructure.security.JwtToUsuarioAuthenticationConverter;
import br.com.danielchipolesch.infrastructure.security.SseBearerTokenResolver;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Resource server: valida o access token (RSA, via JWKS -- ver
// AuthorizationServerConfig) a cada requisição, sem guardar sessão nenhuma
// (STATELESS). JwtToUsuarioAuthenticationConverter recarrega o Usuario do
// banco a partir do "sub" do token e devolve um UsuarioPrincipal -- todo o
// resto do sistema (DocumentoAcessoService, AutenticacaoUtil, os @PreAuthorize)
// continua enxergando exatamente o mesmo tipo de principal de antes, sem
// precisar mudar. Visualizar é liberado para qualquer usuário autenticado --
// a restrição por posse (autor/compartilhado) só entra via @PreAuthorize nos
// endpoints de editar/compartilhar/excluir (ver DocumentoAcessoService), não aqui.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtToUsuarioAuthenticationConverter jwtToUsuarioAuthenticationConverter;

    @Autowired
    private SseBearerTokenResolver sseBearerTokenResolver;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("ETag"));
        // allowedOriginPatterns (em vez de allowedOrigins) permite combinar "*"
        // com allowCredentials=true -- precisa disso pro fetch com
        // credentials:'include' que stores/auth.js faz em /logout (encerra a
        // sessão de cookie do Authorization Server no back-channel).
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @Order(3)
    public SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/v1/fab-legis-api/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // Raiz do backend não é rota de API nenhuma -- só existe pra
                // dar um destino gracioso (ver RootRedirectController) quando
                // alguém cai aqui por engano (ex.: um mismatch de host entre
                // APP_OAUTH2_ISSUER e a origem real do navegador faz o
                // Authorization Server não achar a authorization request
                // salva e cair no fallback "/" do formLogin -- sem isso, a
                // pessoa via só {"message":"Não autenticado."} cru, sem
                // nenhuma explicação).
                .requestMatchers(HttpMethod.GET, "/").permitAll()
                .anyRequest().authenticated()
            )
            // Sem isso, o Spring Security cai no Http403ForbiddenEntryPoint
            // padrão e responde 403 a qualquer requisição sem token válido --
            // o frontend só trata 401 como "sessão inválida" (ver client.js),
            // então esse 403 indevido passava batido, sem deslogar nem
            // redirecionar para o login. AccessDeniedException (autenticado
            // mas sem o papel exigido) continua caindo no AccessDeniedHandler
            // padrão, que responde 403 normalmente -- só a falta de
            // autenticação muda.
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Não autenticado.\"}");
            }))
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(sseBearerTokenResolver)
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(jwtToUsuarioAuthenticationConverter)
                )
            );
        return http.build();
    }
}
