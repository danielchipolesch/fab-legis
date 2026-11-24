package intraer.fablegis.infrastructure.configurations;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// As claims do token (papéis, OM, nome) são a fonte do que a TELA mostra. A sessão de login guarda um retrato do usuário de quando ele entrou, e a
// renovação silenciosa reaproveita essa sessão: as claims têm de sair do usuário de AGORA (do banco), senão uma mudança de papel feita pelo
// administrador só chegaria à tela num novo login (docs/autenticacao.md, "Atualização ao vivo").
class ClaimsDoTokenTest {

    private static Usuario usuario(long id, PapelEnum... papeis) {
        var u = new Usuario();
        u.setId(id);
        var conjunto = EnumSet.noneOf(PapelEnum.class);
        conjunto.addAll(java.util.List.of(papeis));
        u.setPapeis(conjunto);
        return u;
    }

    @Test
    void asClaimsSaemDoUsuarioDeAgoraENaoDoRetratoDaSessao() {
        var repositorio = mock(UsuarioRepository.class);
        var retratoDoLogin = usuario(7L, PapelEnum.ELB, PapelEnum.APR_OM, PapelEnum.PNL_OM);
        var deAgora = usuario(7L, PapelEnum.ELB, PapelEnum.APR_OM);   // o administrador retirou o Painel Gerencial da OM
        when(repositorio.findById(7L)).thenReturn(Optional.of(deAgora));

        var lido = AuthorizationServerConfig.usuarioDeAgora(repositorio, retratoDoLogin);

        assertThat(lido).isSameAs(deAgora);
        assertThat(lido.getPapeis()).containsExactlyInAnyOrder(PapelEnum.ELB, PapelEnum.APR_OM);
    }

    @Test
    void seOUsuarioSumiuDoBancoUsaORetratoDaSessao() {
        var repositorio = mock(UsuarioRepository.class);
        var retratoDoLogin = usuario(7L, PapelEnum.ELB);
        when(repositorio.findById(7L)).thenReturn(Optional.empty());

        assertThat(AuthorizationServerConfig.usuarioDeAgora(repositorio, retratoDoLogin)).isSameAs(retratoDoLogin);
    }

    @Test
    void semIdNaoConsultaOBanco() {
        var repositorio = mock(UsuarioRepository.class);
        var semId = usuario(0L);
        semId.setId(null);

        assertThat(AuthorizationServerConfig.usuarioDeAgora(repositorio, semId)).isSameAs(semId);
        assertThat(AuthorizationServerConfig.usuarioDeAgora(repositorio, null)).isNull();
    }
}
