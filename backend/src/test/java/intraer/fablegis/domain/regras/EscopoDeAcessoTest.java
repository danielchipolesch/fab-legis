package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Escopo de acesso (docs/autenticacao.md): só o CENDOC tem o escopo geral; o admin local só gerencia a própria OM e nunca
// concede ADMIN nem PUBLIC; PUBLIC só existe no CENDOC.
class EscopoDeAcessoTest {

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

    private static final Set<PapelEnum> EDIT = EnumSet.of(PapelEnum.EDIT);

    @Test
    void soOCendocTemEscopoGeral() {
        assertThat(EscopoDeAcesso.temEscopoGeral(usuario("CENDOC"))).isTrue();
        assertThat(EscopoDeAcesso.temEscopoGeral(usuario("cendoc"))).isTrue();
        assertThat(EscopoDeAcesso.temEscopoGeral(usuario("GABAER"))).isFalse();
        assertThat(EscopoDeAcesso.ehAdminGeral(usuario("CENDOC", PapelEnum.ADMIN))).isTrue();
        assertThat(EscopoDeAcesso.ehAdminGeral(usuario("BAGL", PapelEnum.ADMIN))).isFalse();
        assertThat(EscopoDeAcesso.ehAdminLocal(usuario("BAGL", PapelEnum.ADMIN))).isTrue();
        assertThat(EscopoDeAcesso.ehAdminLocal(usuario("CENDOC", PapelEnum.ADMIN))).isFalse();
    }

    @Test
    void oGeralEnxergaTodosOLocalSoAPropriaOm() {
        var geral = usuario("CENDOC", PapelEnum.ADMIN);
        var local = usuario("BAGL", PapelEnum.ADMIN);
        assertThat(EscopoDeAcesso.enxerga(geral, usuario("BAGL"))).isTrue();
        assertThat(EscopoDeAcesso.enxerga(local, usuario("BAGL"))).isTrue();
        assertThat(EscopoDeAcesso.enxerga(local, usuario("BABE"))).isFalse();
        assertThat(EscopoDeAcesso.enxerga(local, usuario("CENDOC"))).isFalse();
    }

    @Test
    void oGeralGerenciaQualquerOmEConcedeAdminLocal() {
        var geral = usuario("CENDOC", PapelEnum.ADMIN);
        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(geral, null, "BAGL", EnumSet.of(PapelEnum.EDIT, PapelEnum.ADMIN)))
                .doesNotThrowAnyException();
        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(geral, usuario("BABE", PapelEnum.ADMIN), "BABE", EDIT))
                .doesNotThrowAnyException();
    }

    @Test
    void oLocalCriaEConcedeAprovadorEAuditorNaPropriaOm() {
        var local = usuario("BAGL", PapelEnum.ADMIN);
        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(local, null, "BAGL",
                EnumSet.of(PapelEnum.EDIT, PapelEnum.APROV, PapelEnum.AUDITOR))).doesNotThrowAnyException();
        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(local, usuario("BAGL", PapelEnum.EDIT), "BAGL",
                EnumSet.of(PapelEnum.EDIT, PapelEnum.APROV))).doesNotThrowAnyException();
    }

    @Test
    void oLocalNaoExtrapolaAPropriaOm() {
        var local = usuario("BAGL", PapelEnum.ADMIN);
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, null, "BABE", EDIT))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("própria OM");
        // nem puxar um usuário de outra OM para a sua, nem mexer em quem é de outra
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, usuario("BABE", PapelEnum.EDIT), "BAGL", EDIT))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("própria OM");
        // nem mover o próprio usuário da OM para outra
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, usuario("BAGL", PapelEnum.EDIT), "BABE", EDIT))
                .isInstanceOf(OperacaoNaoAutorizadaException.class);
    }

    @Test
    void oLocalNaoConcedeAdminNemMexeEmOutroAdmin() {
        var local = usuario("BAGL", PapelEnum.ADMIN);
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, null, "BAGL", EnumSet.of(PapelEnum.EDIT, PapelEnum.ADMIN)))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("administrador geral");
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, usuario("BAGL", PapelEnum.ADMIN), "BAGL", EDIT))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("administradores locais");
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeAdministrar(local, usuario("BAGL", PapelEnum.ADMIN)))
                .isInstanceOf(OperacaoNaoAutorizadaException.class);
        assertThatCode(() -> EscopoDeAcesso.exigirPodeAdministrar(local, usuario("BAGL", PapelEnum.EDIT))).doesNotThrowAnyException();
    }

    @Test
    void publicadorSoExisteNoCendocParaQualquerAdmin() {
        var geral = usuario("CENDOC", PapelEnum.ADMIN);
        var local = usuario("BAGL", PapelEnum.ADMIN);
        var publicador = EnumSet.of(PapelEnum.EDIT, PapelEnum.PUBLIC);

        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(geral, null, "CENDOC", publicador)).doesNotThrowAnyException();
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(geral, null, "BAGL", publicador))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("exclusiva do CENDOC");
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, null, "BAGL", publicador))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("exclusiva do CENDOC");
    }

    // ─── O CENDOC nunca fica sem administrador geral ─────────────────────────

    @Test
    void oUltimoAdminGeralNaoDeixaDeSer() {
        assertThatThrownBy(() -> EscopoDeAcesso.exigirAoMenosUmAdminGeral(true, false, 1))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("ao menos um administrador geral");
    }

    @Test
    void haOutroAdminGeralOuElePermanece() {
        assertThatCode(() -> EscopoDeAcesso.exigirAoMenosUmAdminGeral(true, false, 2)).doesNotThrowAnyException();
        assertThatCode(() -> EscopoDeAcesso.exigirAoMenosUmAdminGeral(true, true, 1)).doesNotThrowAnyException();
    }

    @Test
    void quemNaoEraAdminGeralNaoEntraNaConta() {
        assertThatCode(() -> EscopoDeAcesso.exigirAoMenosUmAdminGeral(false, false, 1)).doesNotThrowAnyException();
    }
}
