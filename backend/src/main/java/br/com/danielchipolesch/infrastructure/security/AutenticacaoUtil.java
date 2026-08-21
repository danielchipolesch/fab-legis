package br.com.danielchipolesch.infrastructure.security;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;

// Ponto único para puxar o Usuario autenticado a partir do SecurityContext,
// para não espalhar o cast em (UsuarioPrincipal) por vários services.
public final class AutenticacaoUtil {

    private AutenticacaoUtil() {}

    public static Usuario usuarioAtual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto atual.");
        }
        return principal.getUsuario();
    }
}
