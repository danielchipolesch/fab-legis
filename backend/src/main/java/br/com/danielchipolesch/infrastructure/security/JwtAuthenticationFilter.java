package br.com.danielchipolesch.infrastructure.security;

import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Stateless de propósito: nenhuma sessão fica guardada no servidor, o token
// é revalidado a cada requisição a partir do header Authorization. É o mesmo
// modelo que spring-boot-starter-oauth2-resource-server usa para validar
// tokens do Keycloak -- na migração, este filtro é substituído por aquele
// starter, mas o restante da cadeia de segurança (SecurityConfig,
// DocumentoAcessoService) não muda.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extrairToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var claims = jwtService.validarEExtrairClaims(token);
            Long usuarioId = Long.valueOf(claims.getSubject());

            usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
                var principal = new UsuarioPrincipal(usuario);
                if (principal.isEnabled()) {
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            });
        } catch (Exception ignored) {
            // Token ausente/expirado/inválido: segue sem autenticação -- o
            // endpoint protegido responde 401 por conta própria mais adiante.
        }

        filterChain.doFilter(request, response);
    }

    // EventSource (API nativa do browser para SSE, usada pelos endpoints de
    // notificação e de presença de edição) não permite setar headers
    // customizados -- só cookie ou query param. Como este sistema não usa
    // cookie de sessão (JWT stateless via header em todo o resto da API),
    // o token chega por query param só nesses dois endpoints. Token em URL
    // normalmente vaza em log de acesso, então a exceção fica restrita a
    // esses paths específicos (regex evita casar qualquer outra rota).
    private static final java.util.regex.Pattern PATH_PRESENCA_STREAM =
            java.util.regex.Pattern.compile("^/v1/documentos/\\d+/presenca/stream$");

    private String extrairToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        String uri = request.getRequestURI();
        if (uri.endsWith("/v1/notificacoes/stream") || PATH_PRESENCA_STREAM.matcher(uri).matches()) {
            return request.getParameter("token");
        }
        return null;
    }
}
