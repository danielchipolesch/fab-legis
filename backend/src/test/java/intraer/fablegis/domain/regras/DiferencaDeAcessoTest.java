package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.auditoria.AcaoDeAcessoEnum;
import intraer.fablegis.domain.entities.usuario.PerfilEnum;
import intraer.fablegis.domain.regras.DiferencaDeAcesso.Estado;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

// Auditoria dos acessos (docs/autenticacao.md): só o que de fato mudou vira registro, um por tipo de mudança.
class DiferencaDeAcessoTest {

    private static Estado estado(boolean ativo, String om, PerfilEnum... perfis) {
        var conjunto = EnumSet.noneOf(PerfilEnum.class);
        conjunto.addAll(java.util.List.of(perfis));
        return new Estado(conjunto, ativo, om);
    }

    @Test
    void semMudancaNaoHaEvento() {
        assertThat(DiferencaDeAcesso.entre(estado(true, "CENDOC", PerfilEnum.ELB), estado(true, "CENDOC", PerfilEnum.ELB))).isEmpty();
    }

    @Test
    void concederPerfilRegistraOConcedidoEOEstadoFinal() {
        var eventos = DiferencaDeAcesso.entre(estado(true, "BAGL", PerfilEnum.ELB), estado(true, "BAGL", PerfilEnum.ELB, PerfilEnum.APR_OM));

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).acao()).isEqualTo(AcaoDeAcessoEnum.ALTEROU_PERFIS);
        assertThat(eventos.get(0).detalhe()).contains("Concedido(s): APR_OM").doesNotContain("Retirado").endsWith("Perfis agora: APR_OM, ELB");
    }

    @Test
    void retirarPerfilRegistraORetirado() {
        var eventos = DiferencaDeAcesso.entre(estado(true, "BAGL", PerfilEnum.ELB, PerfilEnum.PUB_BCA), estado(true, "BAGL", PerfilEnum.ELB));

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).detalhe()).contains("Retirado(s): PUB_BCA").doesNotContain("Concedido");
    }

    @Test
    void conceder_e_retirar_na_mesma_edicao_saem_num_so_registro() {
        var eventos = DiferencaDeAcesso.entre(estado(true, "BAGL", PerfilEnum.ELB, PerfilEnum.PUB_BCA),
                estado(true, "BAGL", PerfilEnum.ELB, PerfilEnum.APR_OM));

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).detalhe()).contains("Concedido(s): APR_OM").contains("Retirado(s): PUB_BCA");
    }

    @Test
    void ativarEDesativarSaoEventosProprios() {
        assertThat(DiferencaDeAcesso.entre(estado(true, "BAGL"), estado(false, "BAGL")))
                .extracting(DiferencaDeAcesso.Evento::acao).containsExactly(AcaoDeAcessoEnum.DESATIVOU_USUARIO);
        assertThat(DiferencaDeAcesso.entre(estado(false, "BAGL"), estado(true, "BAGL")))
                .extracting(DiferencaDeAcesso.Evento::acao).containsExactly(AcaoDeAcessoEnum.ATIVOU_USUARIO);
    }

    @Test
    void trocarDeOmRegistraDeOndeParaOnde() {
        var eventos = DiferencaDeAcesso.entre(estado(true, "BAGL"), estado(true, "CENDOC"));

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).acao()).isEqualTo(AcaoDeAcessoEnum.TROCOU_OM);
        assertThat(eventos.get(0).detalhe()).isEqualTo("BAGL → CENDOC");
    }

    @Test
    void varias_mudancas_geram_um_evento_de_cada_tipo() {
        var eventos = DiferencaDeAcesso.entre(estado(true, "BAGL", PerfilEnum.ELB), estado(false, "CENDOC", PerfilEnum.ELB, PerfilEnum.AUD_GER));

        assertThat(eventos).extracting(DiferencaDeAcesso.Evento::acao)
                .containsExactly(AcaoDeAcessoEnum.ALTEROU_PERFIS, AcaoDeAcessoEnum.DESATIVOU_USUARIO, AcaoDeAcessoEnum.TROCOU_OM);
    }

    @Test
    void listaDePerfisEmOrdemAlfabeticaOuNenhum() {
        assertThat(DiferencaDeAcesso.lista(EnumSet.of(PerfilEnum.PUB_BCA, PerfilEnum.ADM_OM, PerfilEnum.ELB))).isEqualTo("ADM_OM, ELB, PUB_BCA");
        assertThat(DiferencaDeAcesso.lista(EnumSet.noneOf(PerfilEnum.class))).isEqualTo("nenhum");
        assertThat(DiferencaDeAcesso.lista(null)).isEqualTo("nenhum");
    }
}
