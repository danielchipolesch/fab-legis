package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// O último login com sucesso alimenta a detecção de conta dormente da revisão periódica de acessos.
class RegistroDoUltimoAcessoTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RegistroDoUltimoAcesso registro = new RegistroDoUltimoAcesso(usuarios);

    @Test
    void oLoginComSucessoGravaOMomentoDoAcesso() {
        var usuario = new Usuario();
        usuario.setId(7L);
        var principal = new UsuarioPrincipal(usuario);

        registro.aoAutenticar(new AuthenticationSuccessEvent(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())));

        verify(usuarios).registrarUltimoAcesso(eq(7L), any());
    }

    @Test
    void autenticacaoQueNaoEDeUsuarioDoSistemaEIgnorada() {
        registro.aoAutenticar(new AuthenticationSuccessEvent(new UsernamePasswordAuthenticationToken("anonimo", null)));

        verify(usuarios, never()).registrarUltimoAcesso(any(), any());
    }
}
