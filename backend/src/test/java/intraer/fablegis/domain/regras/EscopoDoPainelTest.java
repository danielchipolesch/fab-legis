package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PerfilEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Painel Gerencial: o Painel Gerencial Geral vê todas as OMs (e filtra por uma); o Painel Gerencial da OM, só a própria, sem visão geral.
class EscopoDoPainelTest {

    private static Usuario usuario(long omId, PerfilEnum... perfis) {
        var om = new OrganizacaoMilitar();
        om.setId(omId);
        var u = new Usuario();
        u.setOm(om);
        var conjunto = EnumSet.noneOf(PerfilEnum.class);
        conjunto.addAll(java.util.List.of(perfis));
        u.setPerfis(conjunto);
        return u;
    }

    @Test
    void soOsPerfisDoPainelTemAcesso() {
        assertThat(EscopoDoPainel.temAcesso(usuario(1, PerfilEnum.PNL_GER))).isTrue();
        assertThat(EscopoDoPainel.temAcesso(usuario(2, PerfilEnum.PNL_OM))).isTrue();
        assertThat(EscopoDoPainel.temAcesso(usuario(2, PerfilEnum.ELB, PerfilEnum.ADM_OM, PerfilEnum.AUD_OM, PerfilEnum.APR_OM))).isFalse();
    }

    @Test
    void oPerfilGeralDoPainelVeTodasAsOmsOuFiltraPorUma() {
        var geral = usuario(1, PerfilEnum.PNL_GER);
        assertThat(EscopoDoPainel.ehGeral(geral)).isTrue();
        assertThat(EscopoDoPainel.omDosIndicadores(geral, null)).isNull();
        assertThat(EscopoDoPainel.omDosIndicadores(geral, 7L)).isEqualTo(7L);
    }

    @Test
    void oPerfilDaOmDoPainelTemSempreAPropriaOm() {
        var daOm = usuario(2, PerfilEnum.PNL_OM);
        assertThat(EscopoDoPainel.ehGeral(daOm)).isFalse();
        assertThat(EscopoDoPainel.omDosIndicadores(daOm, null)).isEqualTo(2L);
        assertThat(EscopoDoPainel.omDosIndicadores(daOm, 2L)).isEqualTo(2L);
    }

    @Test
    void oPerfilDaOmDoPainelNaoPedeOutraOm() {
        assertThatThrownBy(() -> EscopoDoPainel.omDosIndicadores(usuario(2, PerfilEnum.PNL_OM), 9L))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("própria OM");
    }

    @Test
    void quemNaoTemPerfilDoPainelNaoTemIndicadores() {
        assertThatThrownBy(() -> EscopoDoPainel.omDosIndicadores(usuario(2, PerfilEnum.ELB), null))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("Painel Gerencial");
    }
}
