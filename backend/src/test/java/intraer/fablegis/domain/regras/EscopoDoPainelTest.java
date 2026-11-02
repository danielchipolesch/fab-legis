package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;
import intraer.fablegis.domain.regras.EscopoDoPainel.Visao;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Painel Gerencial: a visão Geral (papel Painel Gerencial Geral, só no CENDOC) vê todas as OMs e filtra por uma; a visão da OM (papel Painel Gerencial
// da OM, que existe em toda OM, o CENDOC inclusive) vê só a própria. Quem tem os dois papéis (o CENDOC) escolhe a visão.
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

    // ─── Qual visão cada papel tem ───────────────────────────────────────────────────────────────

    @Test
    void quemTemSoUmPapelTemSoAVisaoDele() {
        var geral = usuario(1, PapelEnum.PNL_GER);
        var daOm = usuario(2, PapelEnum.PNL_OM);
        assertThat(EscopoDoPainel.visaoDe(geral, null)).isEqualTo(Visao.GERAL);
        assertThat(EscopoDoPainel.visaoDe(geral, Visao.GERAL)).isEqualTo(Visao.GERAL);
        assertThat(EscopoDoPainel.visaoDe(daOm, null)).isEqualTo(Visao.OM);
        assertThat(EscopoDoPainel.visaoDe(daOm, Visao.OM)).isEqualTo(Visao.OM);
    }

    @Test
    void quemTemSoUmPapelNaoPedeAVisaoDoOutro() {
        assertThatThrownBy(() -> EscopoDoPainel.visaoDe(usuario(2, PapelEnum.PNL_OM), Visao.GERAL))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("Painel Gerencial da OM");
        assertThatThrownBy(() -> EscopoDoPainel.visaoDe(usuario(1, PapelEnum.PNL_GER), Visao.OM))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("Painel Gerencial Geral");
    }

    @Test
    void oCendocComOsDoisPapeisEscolheAVisaoEOPadraoEAGeral() {
        var cendoc = usuario(1, PapelEnum.PNL_GER, PapelEnum.PNL_OM);
        assertThat(EscopoDoPainel.visaoDe(cendoc, null)).isEqualTo(Visao.GERAL);
        assertThat(EscopoDoPainel.visaoDe(cendoc, Visao.GERAL)).isEqualTo(Visao.GERAL);
        assertThat(EscopoDoPainel.visaoDe(cendoc, Visao.OM)).isEqualTo(Visao.OM);
    }

    @Test
    void quemNaoTemPapelDoPainelNaoTemVisao() {
        assertThatThrownBy(() -> EscopoDoPainel.visaoDe(usuario(2, PapelEnum.ELB), null))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("Painel Gerencial");
    }

    // ─── Por qual OM os indicadores são filtrados ────────────────────────────────────────────────

    @Test
    void aVisaoGeralVeTodasAsOmsOuFiltraPorUma() {
        var geral = usuario(1, PapelEnum.PNL_GER);
        assertThat(EscopoDoPainel.omDosIndicadores(geral, Visao.GERAL, null)).isNull();
        assertThat(EscopoDoPainel.omDosIndicadores(geral, Visao.GERAL, 7L)).isEqualTo(7L);
    }

    @Test
    void aVisaoDaOmTemSempreAPropriaOm() {
        var daOm = usuario(2, PapelEnum.PNL_OM);
        assertThat(EscopoDoPainel.omDosIndicadores(daOm, Visao.OM, null)).isEqualTo(2L);
        assertThat(EscopoDoPainel.omDosIndicadores(daOm, Visao.OM, 2L)).isEqualTo(2L);
    }

    @Test
    void aVisaoDaOmNaoPedeOutraOm() {
        assertThatThrownBy(() -> EscopoDoPainel.omDosIndicadores(usuario(2, PapelEnum.PNL_OM), Visao.OM, 9L))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("própria OM");
    }

    @Test
    void aVisaoDaOmDoCendocEOCendocEAGeralTodasAsOms() {
        var cendoc = usuario(1, PapelEnum.PNL_GER, PapelEnum.PNL_OM);
        assertThat(EscopoDoPainel.omDosIndicadores(cendoc, Visao.OM, null)).isEqualTo(1L);
        assertThat(EscopoDoPainel.omDosIndicadores(cendoc, Visao.GERAL, null)).isNull();
        assertThatThrownBy(() -> EscopoDoPainel.omDosIndicadores(cendoc, Visao.OM, 9L))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("própria OM");
    }

    // ─── Quais tipos de espécie os indicadores contam: as NPAs são de uso interno da OM ─────────────

    private static final java.util.Set<TipoDeEspecie> USO_INTERNO = java.util.Set.of(TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA);

    @Test
    void aVisaoGeralNuncaContaAsNpas() {
        assertThat(EscopoDoPainel.tiposDosIndicadores(Visao.GERAL, null, USO_INTERNO)).containsExactly(TipoDeEspecie.CONVENCIONAL);
        assertThat(EscopoDoPainel.tiposDosIndicadores(Visao.GERAL, TipoDeEspecie.CONVENCIONAL, USO_INTERNO)).containsExactly(TipoDeEspecie.CONVENCIONAL);
    }

    @Test
    void aVisaoGeralNaoPedeOModuloDasNpas() {
        assertThatThrownBy(() -> EscopoDoPainel.tiposDosIndicadores(Visao.GERAL, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, USO_INTERNO))
                .isInstanceOf(OperacaoNaoAutorizadaException.class).hasMessageContaining("não considera as NPAs");
    }

    @Test
    void aVisaoDaOmContaTodosOsModulosOuOQuePedir() {
        assertThat(EscopoDoPainel.tiposDosIndicadores(Visao.OM, null, USO_INTERNO))
                .containsExactlyInAnyOrder(TipoDeEspecie.CONVENCIONAL, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA);
        assertThat(EscopoDoPainel.tiposDosIndicadores(Visao.OM, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, USO_INTERNO))
                .containsExactly(TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA);
        assertThat(EscopoDoPainel.tiposDosIndicadores(Visao.OM, TipoDeEspecie.CONVENCIONAL, USO_INTERNO)).containsExactly(TipoDeEspecie.CONVENCIONAL);
    }
}
