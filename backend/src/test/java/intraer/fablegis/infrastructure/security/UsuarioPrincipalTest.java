package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

// Escopo geral só no CENDOC (docs/autenticacao.md): o ADMIN lotado no CENDOC é o administrador GERAL; o ADMIN de qualquer outra OM é local,
// nunca recebe ROLE_ADMIN_GERAL -- o escopo vem da OM, não de um papel que alguém possa conceder a quem é de outra OM.
class UsuarioPrincipalTest {

    private static Usuario usuario(String om, PapelEnum... papeis) {
        var o = new OrganizacaoMilitar();
        o.setSigla(om);
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
    void soOAdminLotadoNoCendocEAdminGeral() {
        assertThat(authorities(usuario("CENDOC", PapelEnum.EDIT, PapelEnum.ADMIN))).contains("ROLE_ADMIN", "ROLE_ADMIN_GERAL");
        assertThat(authorities(usuario("BAGL", PapelEnum.EDIT, PapelEnum.ADMIN))).contains("ROLE_ADMIN").doesNotContain("ROLE_ADMIN_GERAL");
    }

    @Test
    void soOAuditorLotadoNoCendocEAuditorGeral() {
        assertThat(authorities(usuario("CENDOC", PapelEnum.AUDITOR))).contains("ROLE_AUDITOR", "ROLE_AUDITOR_GERAL");
        assertThat(authorities(usuario("BAGL", PapelEnum.AUDITOR))).contains("ROLE_AUDITOR").doesNotContain("ROLE_AUDITOR_GERAL");
    }

    @Test
    void estarNoCendocSemSerAdminNaoDaEscopoGeralDeAdministracao() {
        assertThat(authorities(usuario("CENDOC", PapelEnum.EDIT, PapelEnum.APROV))).doesNotContain("ROLE_ADMIN_GERAL", "ROLE_AUDITOR_GERAL");
    }

    @Test
    void mudarDeOmTiraOEscopoGeral() {
        var admin = usuario("CENDOC", PapelEnum.EDIT, PapelEnum.ADMIN);
        admin.getOm().setSigla("BAGL");
        assertThat(authorities(admin)).doesNotContain("ROLE_ADMIN_GERAL");
    }
}
