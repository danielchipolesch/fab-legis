package br.com.danielchipolesch.infrastructure.configurations;

import br.com.danielchipolesch.infrastructure.security.UsuarioDetailsService;
import br.com.danielchipolesch.infrastructure.security.UsuarioPrincipal;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

// Authorization Server embutido no próprio backend (sem novo serviço no
// docker-compose) -- emite tokens RSA assinados, validados via JWKS
// (/oauth2/jwks) pelo resource server (SecurityConfig, @Order(2)) e pelo
// collab (server.js, via jose/createRemoteJWKSet). Ver CLAUDE.md, seção
// "PLANO TEMPORÁRIO -- migração JWT caseiro -> OAuth2" para o racional
// completo: essa rota deixa uma futura troca pro Keycloak restrita a
// issuer-uri + re-registro do client, sem tocar frontend/collab de novo.
@Configuration
public class AuthorizationServerConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    @Value("${app.oauth2.issuer}")
    private String issuer;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${app.frontend.login-url}")
    private String frontendLoginUrl;

    @Autowired
    private UsuarioDetailsService usuarioDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    // formLogin (chain 2 abaixo) precisa de um AuthenticationManager próprio --
    // reaproveita o mesmo UsuarioDetailsService/PasswordEncoder que a
    // autenticação caseira já usava, sem tocar em Usuario/hash de senha.
    @Bean
    public AuthenticationManager authorizationServerAuthenticationManager() {
        var provider = new DaoAuthenticationProvider((UserDetailsService) usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }

    // Chain 1: só os endpoints do Authorization Server em si
    // (/oauth2/authorize, /oauth2/token, /oauth2/jwks, /.well-known/**).
    // Sessão baseada em cookie (não STATELESS) -- é assim que, depois do login
    // na chain 2 (abaixo), o redirect de volta pra cá já chega autenticado.
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        var authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher());

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .with(authorizationServerConfigurer, configurer -> configurer
                .oidc(oidc -> {}) // habilita /.well-known/openid-configuration, além do /oauth2/jwks
            )
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            // Não-autenticado tentando /oauth2/authorize: manda pra tela de
            // login da SPA (Vue), não pra um form HTML gerado pelo backend.
            // ExceptionTranslationFilter guarda a requisição original
            // (/oauth2/authorize?...) na RequestCache antes de redirecionar --
            // é pra lá que o formLogin da chain 2 volta depois do login OK.
            //
            // O matcher aqui tem que ser por MEDIA TYPE (text/html), não pelos
            // endpoints do Authorization Server -- usar getEndpointsMatcher()
            // aplicaria esse redirect a QUALQUER falha de autenticação sob
            // /oauth2/**, inclusive chamadas fetch() (Accept: application/json)
            // pro /oauth2/token feitas por auth.js (troca de code/refresh) --
            // essas precisam do erro OAuth2 padrão (JSON, ver
            // OAuth2ErrorResponseHandler interno do configurer), não de um
            // redirect HTML que o fetch não consegue seguir sem CORS (era
            // exatamente esse o motivo do refresh() falhar com "Failed to
            // fetch"). Só a navegação de verdade do navegador pro
            // /oauth2/authorize (essa sim pede text/html) deve ser redirecionada.
            .exceptionHandling(ex -> {
                var htmlMatcher = new org.springframework.security.web.util.matcher.MediaTypeRequestMatcher(
                        org.springframework.http.MediaType.TEXT_HTML);
                // "*/*" (Accept default de fetch()/curl sem header explícito)
                // é "compatível" com text/html por regra de negociação de
                // conteúdo -- sem ignorá-lo aqui, ele também bate no matcher
                // acima e QUALQUER chamada programática sem Accept explícito
                // (é o caso comum) volta a cair no redirect HTML por engano.
                htmlMatcher.setIgnoredMediaTypes(java.util.Set.of(org.springframework.http.MediaType.ALL));
                // "?continuar=1" (não "?erro=1", que é só pra falha de senha)
                // é o único jeito confiável de LoginPage.vue saber que este GET
                // /login é uma navegação DE VERDADE originada aqui -- o
                // ExceptionTranslationFilter já salvou a /oauth2/authorize
                // pendente na sessão antes de chamar este entry point, então é
                // este redirect (não o sessionStorage do navegador) que marca
                // "existe uma autorização pendente pra retomar". Usar só
                // sessionStorage.oauth2.state como esse sinal (como era antes)
                // é frágil: um valor de uma tentativa anterior abandonada (outra
                // aba, outra origem localhost/127.0.0.1, um refresh no meio do
                // caminho) sobrevive e engana a LoginPage a achar que já existe
                // um fluxo em andamento, pulando a chamada a /oauth2/authorize
                // -- o POST /login subsequente autentica numa sessão SEM
                // nenhuma autorização pendente salva, e o SavedRequestAware...
                // cai no fallback (redireciona pra "/"), nunca emitindo "code"
                // nenhum. Foi exatamente esse o bug relatado (POST 302 pra "/",
                // sem nunca passar por /oauth2/authorize).
                ex.defaultAuthenticationEntryPointFor(
                        (request, response, authException) -> response.sendRedirect(frontendLoginUrl + "?continuar=1"),
                        htmlMatcher);
            });

        return http.build();
    }

    // Chain 2: só /login e /logout -- processa o POST do formulário (username=
    // CPF, password=senha) que a LoginPage.vue envia via <form> tradicional
    // (não fetch, pro cookie de sessão e o redirect em cadeia funcionarem).
    // CSRF desligado só aqui: o form vem de origem cruzada (frontend em outra
    // porta), sem round-trip prévio pra buscar um token CSRF -- risco
    // remanescente é login CSRF (vítima logada na conta do atacante), não
    // account takeover; ver docs/autenticacao.md.
    @Bean
    @Order(2)
    public SecurityFilterChain loginSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/login", "/logout")
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .authenticationManager(authorizationServerAuthenticationManager())
            // Ordem importa: loginProcessingUrl() precisa vir ANTES de
            // loginPage() -- loginPage() só herda o processingUrl default se
            // ele ainda não tiver sido setado; chamado na ordem errada, tenta
            // validar a própria loginPage (uma URL absoluta) como um pattern
            // de path local (que precisa começar com "/") e quebra o boot.
            .formLogin(form -> form
                .loginProcessingUrl("/login")
                .loginPage(frontendLoginUrl)
                .failureUrl(frontendLoginUrl + "?erro=1")
            )
            // GET além do POST padrão: precisa aceitar uma navegação de página
            // inteira (window.location.href, não fetch) -- o cookie de sessão
            // é SameSite=Lax (padrão do Spring Security), então um POST via
            // fetch() cross-origin (frontend noutra porta) NUNCA leva o cookie
            // (Lax só manda cookie em navegação de topo, não em fetch/XHR
            // subresource) -- o /logout "funcionava" (200/302) mas sem nunca
            // achar sessão nenhuma pra matar. auth.js's logout() por isso
            // navega de verdade, como iniciarLogin() já fazia.
            .logout(logout -> logout
                .logoutRequestMatcher(new org.springframework.security.web.util.matcher.OrRequestMatcher(
                        org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
                                .pathPattern(org.springframework.http.HttpMethod.GET, "/logout"),
                        org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
                                .pathPattern(org.springframework.http.HttpMethod.POST, "/logout")
                ))
                .logoutSuccessUrl(frontendLoginUrl));

        return http.build();
    }

    // ─── Client registrado ──────────────────────────────────────────────────────

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // Sem REFRESH_TOKEN aqui de propósito: o Spring Authorization Server
        // nunca emite refresh token pra um client público
        // (ClientAuthenticationMethod.NONE) -- decisão de segurança do próprio
        // framework (um refresh token é um credential de longa duração, dar um
        // pra um client que não consegue se autenticar de novo depois seria
        // reintroduzir o mesmo risco que o PKCE existe pra evitar).
        //
        // auth.js refresh() tenta uma renovação silenciosa via iframe oculto
        // reautorizando /oauth2/authorize com o cookie de sessão -- mas em dev
        // (HTTP puro) o cookie de sessão do Spring Security é SameSite=Lax por
        // padrão, e navegação de SUB-frame (iframe) não manda cookies Lax,
        // então essa renovação hoje falha sempre em localhost (cai em
        // /login), só funcionaria em produção com HTTPS + SameSite=None
        // explícito no cookie. accessTokenTimeToLive maior (30min, era 15
        // antes) é o mitigante real por ora -- quando o token expira sem a
        // renovação silenciosa funcionar, client.js simplesmente desloga e
        // manda pro login de novo (comportamento seguro, só menos
        // conveniente que antes).
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("fab-legis-frontend")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // client público (SPA), sem secret
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope(OidcScopes.OPENID)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true) // PKCE obrigatório -- único jeito seguro de um client público
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(client);
    }

    // ─── Chaves RSA + JWKS ────────────────────────────────────────────────────

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = gerarChaveRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    // Em produção, uma chave gerada a cada boot invalida todos os tokens
    // emitidos anteriormente a cada deploy/restart -- aceitável por ora
    // (mesmo comportamento efetivo do jwt.secret caseiro, que também não
    // sobrevivia a troca manual), mas fica registrado aqui como o próximo
    // passo óbvio (carregar de arquivo/variável de ambiente) se isso incomodar
    // em produção real.
    private RSAKey gerarChaveRsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            log.warn("Authorization Server: par de chaves RSA gerado em memória (efêmero) -- " +
                    "tokens emitidos antes de um restart deixam de ser válidos depois dele.");
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar par de chaves RSA para o Authorization Server.", e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    // ─── Claims do access token ─────────────────────────────────────────────────

    // Espelha exatamente o que JwtService.gerarToken() produzia -- sub = id
    // numérico do usuário (não o principal name default do Authorization
    // Server, que é o CPF -- ver DaoAuthenticationProvider/UsuarioPrincipal.
    // getUsername()), preferred_username (CPF), nome, om_id, realm_access.roles
    // -- pra não exigir nenhuma mudança em PapelEnum/@PreAuthorize/
    // DocumentoAcessoService/JwtToUsuarioAuthenticationConverter (que lê "sub"
    // como o id pra recarregar o Usuario do banco).
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")
                    && context.getPrincipal().getPrincipal() instanceof UsuarioPrincipal principal) {
                var usuario = principal.getUsuario();
                List<String> roles = principal.getPapeis().stream().map(Enum::name).toList();

                context.getClaims()
                        .subject(String.valueOf(usuario.getId()))
                        .claim("preferred_username", usuario.getCpf())
                        .claim("nome", usuario.getNome())
                        .claim("om_id", String.valueOf(usuario.getOm().getId()))
                        .claim("realm_access", java.util.Map.of("roles", roles));
            }
        };
    }
}
