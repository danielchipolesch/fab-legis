package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

// Um authority por papel, pelo CÓDIGO (docs/autenticacao.md): o escopo geral/local já está no próprio papel (ADM_GER, AUD_GER só no
// CENDOC; ADM_OM, AUD_OM nas demais OMs) -- nada é deduzido da lotação aqui.
class UsuarioPrincipalTest {

    private static Usuario usuario(PapelEnum... papeis) {
        var o = new OrganizacaoMilitar();
        o.setSigla("CENDOC");
        var u = new Usuario();
        u.setOm(o);
        var conjunto = EnumSet.noneOf(PapelEnum.class);
        conjunto.addAll(java.util.List.of(papeis));
        u.setPapeis(conjunto);
        return u;
    }

    private static java.util.List<String> authorities(Usuario u) {
        return new UsuarioPrincipal(u).getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void cadaPapelViraUmRolePeloCodigo() {
        assertThat(authorities(usuario(PapelEnum.ELB, PapelEnum.ADM_GER, PapelEnum.AUD_GER, PapelEnum.PUB_BCA)))
                .containsExactlyInAnyOrder("ROLE_ELB", "ROLE_ADM_GER", "ROLE_AUD_GER", "ROLE_PUB_BCA");
        assertThat(authorities(usuario(PapelEnum.ELB, PapelEnum.APR_OM, PapelEnum.ADM_OM, PapelEnum.AUD_OM)))
                .containsExactlyInAnyOrder("ROLE_ELB", "ROLE_APR_OM", "ROLE_ADM_OM", "ROLE_AUD_OM");
    }

    @Test
    void semPapelNaoHaAuthority() {
        assertThat(authorities(usuario())).isEmpty();
    }

    @Test
    void oCodigoEoNomePorExtensoDeCadaPapel() {
        assertThat(PapelEnum.ELB.codigo()).isEqualTo("ELB");
        assertThat(PapelEnum.ELB.nome()).isEqualTo("Elaborador");
        assertThat(PapelEnum.APR_OM.nome()).isEqualTo("Aprovador da OM");
        assertThat(PapelEnum.PUB_BCA.nome()).isEqualTo("Publicador do BCA");
        assertThat(PapelEnum.AUD_OM.nome()).isEqualTo("Auditor da OM");
        assertThat(PapelEnum.AUD_GER.nome()).isEqualTo("Auditor Geral");
        assertThat(PapelEnum.ADM_OM.nome()).isEqualTo("Administrador da OM");
        assertThat(PapelEnum.ADM_GER.nome()).isEqualTo("Administrador Geral");
        for (var p : PapelEnum.values()) {
            assertThat(p.descricao()).as("descrição de %s", p).isNotBlank();
        }
    }
}
