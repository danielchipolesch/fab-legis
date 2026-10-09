package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Ponto único para puxar o Usuario autenticado, para não espalhar o cast em (UsuarioPrincipal) por controllers e
// services: do SecurityContext (usuarioAtual) ou do Authentication que o Spring injeta nos métodos (usuarioDe).
public final class AutenticacaoUtil {

    private AutenticacaoUtil() {}

    public static Usuario usuarioAtual() {
        return usuarioDe(SecurityContextHolder.getContext().getAuthentication());
    }

    // O Authentication que chega a um controller ou a um método guardado por @PreAuthorize. Sem autenticação (ou com um
    // principal que não é o do sistema), é erro de programação ou de configuração de segurança, não de quem chama.
    public static Usuario usuarioDe(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto atual.");
        }
        return principal.getUsuario();
    }
}
