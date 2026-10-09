package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Ponto único para obter o usuário autenticado (antes, 8 controllers/services faziam o cast em UsuarioPrincipal).
class AutenticacaoUtilTest {

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private static Usuario usuario(long id) {
        var u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    void usuarioDeDevolveOUsuarioDoPrincipal() {
        var auth = new UsernamePasswordAuthenticationToken(new UsuarioPrincipal(usuario(7)), null);

        assertThat(AutenticacaoUtil.usuarioDe(auth).getId()).isEqualTo(7L);
    }

    @Test
    void usuarioAtualLeDoSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UsuarioPrincipal(usuario(9)), null));

        assertThat(AutenticacaoUtil.usuarioAtual().getId()).isEqualTo(9L);
    }

    @Test
    void semAutenticacaoOuComPrincipalDeOutroTipoELancaErroClaro() {
        assertThatThrownBy(() -> AutenticacaoUtil.usuarioDe(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Nenhum usuário autenticado");
        assertThatThrownBy(() -> AutenticacaoUtil.usuarioDe(new UsernamePasswordAuthenticationToken("anonimo", null)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(AutenticacaoUtil::usuarioAtual).isInstanceOf(IllegalStateException.class);
    }
}
