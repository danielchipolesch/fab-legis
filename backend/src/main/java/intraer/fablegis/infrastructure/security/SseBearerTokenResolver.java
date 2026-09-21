package intraer.fablegis.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

// EventSource (API nativa do browser para SSE, usada pelos endpoints de
// notificação e de presença de edição) não permite setar headers customizados
// -- só cookie ou query param. Como o resto da API é 100% stateless via
// header Authorization, o token chega por query param só nesses dois
// endpoints (mesma restrição que JwtAuthenticationFilter.PATH_PRESENCA_STREAM
// já aplicava) -- token em URL normalmente vaza em log de acesso, então a
// exceção fica restrita a esses paths específicos.
@Component
public class SseBearerTokenResolver implements BearerTokenResolver {

    private static final Pattern PATH_PRESENCA_STREAM =
            Pattern.compile("^/v1/documentos/\\d+/presenca/stream$");

    private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        String viaHeader = headerResolver.resolve(request);
        if (viaHeader != null) return viaHeader;

        String uri = request.getRequestURI();
        if (uri.endsWith("/v1/notificacoes/stream") || PATH_PRESENCA_STREAM.matcher(uri).matches()) {
            return request.getParameter("token");
        }
        return null;
    }
}
