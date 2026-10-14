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

    private static final Set<PapelEnum> EDIT = EnumSet.of(PapelEnum.ELB);

    @Test
    void soOCendocTemEscopoGeral() {
        assertThat(EscopoDeAcesso.temEscopoGeral(usuario("CENDOC"))).isTrue();
        assertThat(EscopoDeAcesso.temEscopoGeral(usuario("cendoc"))).isTrue();
        assertThat(EscopoDeAcesso.temEscopoGeral(usuario("GABAER"))).isFalse();
        assertThat(EscopoDeAcesso.ehAdminGeral(usuario("CENDOC", PapelEnum.ADM_GER))).isTrue();
        assertThat(EscopoDeAcesso.ehAdminGeral(usuario("BAGL", PapelEnum.ADM_OM))).isFalse();
        assertThat(EscopoDeAcesso.ehAdminLocal(usuario("BAGL", PapelEnum.ADM_OM))).isTrue();
        assertThat(EscopoDeAcesso.ehAdminLocal(usuario("CENDOC", PapelEnum.ADM_GER))).isFalse();
    }

    @Test
    void oGeralEnxergaTodosOLocalSoAPropriaOm() {
        var geral = usuario("CENDOC", PapelEnum.ADM_GER);
        var local = usuario("BAGL", PapelEnum.ADM_OM);
        assertThat(EscopoDeAcesso.enxerga(geral, usuario("BAGL"))).isTrue();
        assertThat(EscopoDeAcesso.enxerga(local, usuario("BAGL"))).isTrue();
        assertThat(EscopoDeAcesso.enxerga(local, usuario("BABE"))).isFalse();
        assertThat(EscopoDeAcesso.enxerga(local, usuario("CENDOC"))).isFalse();
    }

    @Test
    void oGeralGerenciaQualquerOmEConcedeAdminLocal() {
        var geral = usuario("CENDOC", PapelEnum.ADM_GER);
        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(geral, null, "BAGL", EnumSet.of(PapelEnum.ELB, PapelEnum.ADM_OM)))
                .doesNotThrowAnyException();
        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(geral, usuario("BABE", PapelEnum.ADM_OM), "BABE", EDIT))
                .doesNotThrowAnyException();
    }

    @Test
    void oLocalCriaEConcedeAprovadorEAuditorNaPropriaOm() {
        var local = usuario("BAGL", PapelEnum.ADM_OM);
        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(local, null, "BAGL",
                EnumSet.of(PapelEnum.ELB, PapelEnum.APR_OM, PapelEnum.AUD_OM))).doesNotThrowAnyException();
        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(local, usuario("BAGL", PapelEnum.ELB), "BAGL",
                EnumSet.of(PapelEnum.ELB, PapelEnum.APR_OM))).doesNotThrowAnyException();
    }

    @Test
    void oLocalNaoExtrapolaAPropriaOm() {
        var local = usuario("BAGL", PapelEnum.ADM_OM);
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, null, "BABE", EDIT))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("própria OM");
        // nem puxar um usuário de outra OM para a sua, nem mexer em quem é de outra
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, usuario("BABE", PapelEnum.ELB), "BAGL", EDIT))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("própria OM");
        // nem mover o próprio usuário da OM para outra
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, usuario("BAGL", PapelEnum.ELB), "BABE", EDIT))
                .isInstanceOf(OperacaoNaoAutorizadaException.class);
    }

    @Test
    void oLocalNaoConcedeAdminNemMexeEmOutroAdmin() {
        var local = usuario("BAGL", PapelEnum.ADM_OM);
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, null, "BAGL", EnumSet.of(PapelEnum.ELB, PapelEnum.ADM_OM)))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("Administrador Geral");
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, usuario("BAGL", PapelEnum.ADM_OM), "BAGL", EDIT))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("administradores são mantidos");
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeAdministrar(local, usuario("BAGL", PapelEnum.ADM_OM)))
                .isInstanceOf(OperacaoNaoAutorizadaException.class);
        assertThatCode(() -> EscopoDeAcesso.exigirPodeAdministrar(local, usuario("BAGL", PapelEnum.ELB))).doesNotThrowAnyException();
    }

    @Test
    void publicadorSoExisteNoCendocParaQualquerAdmin() {
        var geral = usuario("CENDOC", PapelEnum.ADM_GER);
        var local = usuario("BAGL", PapelEnum.ADM_OM);
        var publicador = EnumSet.of(PapelEnum.ELB, PapelEnum.PUB_BCA);

        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(geral, null, "CENDOC", publicador)).doesNotThrowAnyException();
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(geral, null, "BAGL", publicador))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("exclusivo do CENDOC");
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(local, null, "BAGL", publicador))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("exclusivo do CENDOC");
    }

    // ─── O CENDOC nunca fica sem administrador geral ─────────────────────────

    @Test
    void oUltimoAdminGeralNaoDeixaDeSer() {
        assertThatThrownBy(() -> EscopoDeAcesso.exigirAoMenosUmAdminGeral(true, false, 1))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("ao menos um Administrador Geral");
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

    // ─── Cada perfil só combina com a lotação certa ──────────────────────────

    @Test
    void osPerfisDoCendocSoExistemNele() {
        for (var papel : EnumSet.of(PapelEnum.ADM_GER, PapelEnum.AUD_GER, PapelEnum.PUB_BCA, PapelEnum.PNL_GER)) {
            assertThatThrownBy(() -> EscopoDeAcesso.exigirPerfisDaLotacao("BAGL", EnumSet.of(PapelEnum.ELB, papel)))
                    .as(papel.name()).isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("exclusivo do CENDOC");
            assertThatCode(() -> EscopoDeAcesso.exigirPerfisDaLotacao("CENDOC", EnumSet.of(PapelEnum.ELB, papel))).doesNotThrowAnyException();
        }
    }

    @Test
    void osPerfisDaOmNaoExistemNoCendoc() {
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPerfisDaLotacao("CENDOC", EnumSet.of(PapelEnum.ELB, PapelEnum.ADM_OM)))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("use Administrador Geral");
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPerfisDaLotacao("CENDOC", EnumSet.of(PapelEnum.ELB, PapelEnum.AUD_OM)))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("use Auditor Geral");
        assertThatCode(() -> EscopoDeAcesso.exigirPerfisDaLotacao("BAGL", EnumSet.of(PapelEnum.ELB, PapelEnum.ADM_OM, PapelEnum.AUD_OM, PapelEnum.APR_OM)))
                .doesNotThrowAnyException();
    }

    @Test
    void oAdminGeralNaoConcedeAdminGeralForaDoCendoc() {
        var geral = usuario("CENDOC", PapelEnum.ADM_GER);
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPodeGerenciar(geral, null, "BAGL", EnumSet.of(PapelEnum.ELB, PapelEnum.ADM_GER)))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("exclusivo do CENDOC");
    }

    @Test
    void oPerfilGeralDoPainelSoNoCendocEODaOmNuncaNele() {
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPerfisDaLotacao("BAGL", EnumSet.of(PapelEnum.ELB, PapelEnum.PNL_GER)))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("exclusivo do CENDOC");
        assertThatThrownBy(() -> EscopoDeAcesso.exigirPerfisDaLotacao("CENDOC", EnumSet.of(PapelEnum.ELB, PapelEnum.PNL_OM)))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("use Painel Gerencial Geral");
        assertThatCode(() -> EscopoDeAcesso.exigirPerfisDaLotacao("CENDOC", EnumSet.of(PapelEnum.ELB, PapelEnum.PNL_GER))).doesNotThrowAnyException();
        assertThatCode(() -> EscopoDeAcesso.exigirPerfisDaLotacao("BAGL", EnumSet.of(PapelEnum.ELB, PapelEnum.PNL_OM))).doesNotThrowAnyException();
    }

    @Test
    void oAdminDaOmConcedePainelDaOmNaPropriaOm() {
        var local = usuario("BAGL", PapelEnum.ADM_OM);
        assertThatCode(() -> EscopoDeAcesso.exigirPodeGerenciar(local, null, "BAGL", EnumSet.of(PapelEnum.ELB, PapelEnum.PNL_OM)))
                .doesNotThrowAnyException();
    }
}
