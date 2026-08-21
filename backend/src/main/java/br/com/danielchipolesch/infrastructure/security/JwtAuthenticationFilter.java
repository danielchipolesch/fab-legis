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

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(7);
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
}
