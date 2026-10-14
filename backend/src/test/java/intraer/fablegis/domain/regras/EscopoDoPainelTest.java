package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Painel Gerencial: o Painel Gerencial Geral vê todas as OMs (e filtra por uma); o Painel Gerencial da OM, só a própria, sem visão geral.
class EscopoDoPainelTest {

    private static Usuario usuario(long omId, PapelEnum... papeis) {
        var om = new OrganizacaoMilitar();
        om.setId(omId);
        var u = new Usuario();
        u.setOm(om);
        var conjunto = EnumSet.noneOf(PapelEnum.class);
        conjunto.addAll(java.util.List.of(papeis));
        u.setPapeis(conjunto);
        return u;
    }

    @Test
    void soOsPapeisDoPainelTemAcesso() {
        assertThat(EscopoDoPainel.temAcesso(usuario(1, PapelEnum.PNL_GER))).isTrue();
        assertThat(EscopoDoPainel.temAcesso(usuario(2, PapelEnum.PNL_OM))).isTrue();
        assertThat(EscopoDoPainel.temAcesso(usuario(2, PapelEnum.ELB, PapelEnum.ADM_OM, PapelEnum.AUD_OM, PapelEnum.APR_OM))).isFalse();
    }

    @Test
    void oPapelGeralDoPainelVeTodasAsOmsOuFiltraPorUma() {
        var geral = usuario(1, PapelEnum.PNL_GER);
        assertThat(EscopoDoPainel.ehGeral(geral)).isTrue();
        assertThat(EscopoDoPainel.omDosIndicadores(geral, null)).isNull();
        assertThat(EscopoDoPainel.omDosIndicadores(geral, 7L)).isEqualTo(7L);
    }

    @Test
    void oPapelDaOmDoPainelTemSempreAPropriaOm() {
        var daOm = usuario(2, PapelEnum.PNL_OM);
        assertThat(EscopoDoPainel.ehGeral(daOm)).isFalse();
        assertThat(EscopoDoPainel.omDosIndicadores(daOm, null)).isEqualTo(2L);
        assertThat(EscopoDoPainel.omDosIndicadores(daOm, 2L)).isEqualTo(2L);
    }

    @Test
    void oPapelDaOmDoPainelNaoPedeOutraOm() {
        assertThatThrownBy(() -> EscopoDoPainel.omDosIndicadores(usuario(2, PapelEnum.PNL_OM), 9L))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("própria OM");
    }

    @Test
    void quemNaoTemPapelDoPainelNaoTemIndicadores() {
        assertThatThrownBy(() -> EscopoDoPainel.omDosIndicadores(usuario(2, PapelEnum.ELB), null))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("Painel Gerencial");
    }
}
