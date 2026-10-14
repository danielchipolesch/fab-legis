package intraer.fablegis.infrastructure.security;

import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

// Guarda o momento do último login com sucesso (usuário/senha): é o que a revisão periódica de acessos usa para achar conta
// dormente (180 dias sem entrar). Só o login de verdade conta -- o token Bearer das chamadas à API não passa por aqui.
@Component
@RequiredArgsConstructor
public class RegistroDoUltimoAcesso {

    private final UsuarioRepository usuarioRepository;

    @EventListener
    @Transactional
    public void aoAutenticar(AuthenticationSuccessEvent evento) {
        if (evento.getAuthentication().getPrincipal() instanceof UsuarioPrincipal principal) {
            usuarioRepository.registrarUltimoAcesso(principal.getUsuario().getId(), Timestamp.from(Instant.now()));
        }
    }
}
