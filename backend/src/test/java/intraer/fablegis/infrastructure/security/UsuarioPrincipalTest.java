package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PerfilEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

// Um authority por perfil, pelo CÓDIGO (docs/autenticacao.md): o escopo geral/local já está no próprio perfil (ADM_GER, AUD_GER só no
// CENDOC; ADM_OM, AUD_OM nas demais OMs) -- nada é deduzido da lotação aqui.
class UsuarioPrincipalTest {

    private static Usuario usuario(PerfilEnum... perfis) {
        var o = new OrganizacaoMilitar();
        o.setSigla("CENDOC");
        var u = new Usuario();
        u.setOm(o);
        var conjunto = EnumSet.noneOf(PerfilEnum.class);
        conjunto.addAll(java.util.List.of(perfis));
        u.setPerfis(conjunto);
        return u;
    }

    private static java.util.List<String> authorities(Usuario u) {
        return new UsuarioPrincipal(u).getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void cadaPerfilViraUmRolePeloCodigo() {
        assertThat(authorities(usuario(PerfilEnum.ELB, PerfilEnum.ADM_GER, PerfilEnum.AUD_GER, PerfilEnum.PUB_BCA)))
                .containsExactlyInAnyOrder("ROLE_ELB", "ROLE_ADM_GER", "ROLE_AUD_GER", "ROLE_PUB_BCA");
        assertThat(authorities(usuario(PerfilEnum.ELB, PerfilEnum.APR_OM, PerfilEnum.ADM_OM, PerfilEnum.AUD_OM)))
                .containsExactlyInAnyOrder("ROLE_ELB", "ROLE_APR_OM", "ROLE_ADM_OM", "ROLE_AUD_OM");
    }

    @Test
    void semPerfilNaoHaAuthority() {
        assertThat(authorities(usuario())).isEmpty();
    }

    @Test
    void oCodigoEoNomePorExtensoDeCadaPerfil() {
        assertThat(PerfilEnum.ELB.codigo()).isEqualTo("ELB");
        assertThat(PerfilEnum.ELB.nome()).isEqualTo("Elaborador");
        assertThat(PerfilEnum.APR_OM.nome()).isEqualTo("Aprovador da OM");
        assertThat(PerfilEnum.PUB_BCA.nome()).isEqualTo("Publicador do BCA");
        assertThat(PerfilEnum.AUD_OM.nome()).isEqualTo("Auditor da OM");
        assertThat(PerfilEnum.AUD_GER.nome()).isEqualTo("Auditor Geral");
        assertThat(PerfilEnum.ADM_OM.nome()).isEqualTo("Administrador da OM");
        assertThat(PerfilEnum.ADM_GER.nome()).isEqualTo("Administrador Geral");
        for (var p : PerfilEnum.values()) {
            assertThat(p.descricao()).as("descrição de %s", p).isNotBlank();
        }
    }
}
