package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.auditoria.AcaoDeAcessoEnum;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.regras.DiferencaDeAcesso.Estado;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

// Auditoria dos acessos (docs/autenticacao.md): só o que de fato mudou vira registro, um por tipo de mudança.
class DiferencaDeAcessoTest {

    private static Estado estado(boolean ativo, String om, PapelEnum... papeis) {
        var conjunto = EnumSet.noneOf(PapelEnum.class);
        conjunto.addAll(java.util.List.of(papeis));
        return new Estado(conjunto, ativo, om);
    }

    @Test
    void semMudancaNaoHaEvento() {
        assertThat(DiferencaDeAcesso.entre(estado(true, "CENDOC", PapelEnum.ELB), estado(true, "CENDOC", PapelEnum.ELB))).isEmpty();
    }

    @Test
    void concederPapelRegistraOConcedidoEOEstadoFinal() {
        var eventos = DiferencaDeAcesso.entre(estado(true, "BAGL", PapelEnum.ELB), estado(true, "BAGL", PapelEnum.ELB, PapelEnum.APR_OM));

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).acao()).isEqualTo(AcaoDeAcessoEnum.ALTEROU_PAPEIS);
        assertThat(eventos.get(0).detalhe()).contains("Concedido(s): APR_OM").doesNotContain("Retirado").endsWith("Papéis agora: APR_OM, ELB");
    }

    @Test
    void retirarPapelRegistraORetirado() {
        var eventos = DiferencaDeAcesso.entre(estado(true, "BAGL", PapelEnum.ELB, PapelEnum.PUB_BCA), estado(true, "BAGL", PapelEnum.ELB));

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).detalhe()).contains("Retirado(s): PUB_BCA").doesNotContain("Concedido");
    }

    @Test
    void conceder_e_retirar_na_mesma_edicao_saem_num_so_registro() {
        var eventos = DiferencaDeAcesso.entre(estado(true, "BAGL", PapelEnum.ELB, PapelEnum.PUB_BCA),
                estado(true, "BAGL", PapelEnum.ELB, PapelEnum.APR_OM));

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
        var eventos = DiferencaDeAcesso.entre(estado(true, "BAGL", PapelEnum.ELB), estado(false, "CENDOC", PapelEnum.ELB, PapelEnum.AUD_GER));

        assertThat(eventos).extracting(DiferencaDeAcesso.Evento::acao)
                .containsExactly(AcaoDeAcessoEnum.ALTEROU_PAPEIS, AcaoDeAcessoEnum.DESATIVOU_USUARIO, AcaoDeAcessoEnum.TROCOU_OM);
    }

    @Test
    void listaDePapeisEmOrdemAlfabeticaOuNenhum() {
        assertThat(DiferencaDeAcesso.lista(EnumSet.of(PapelEnum.PUB_BCA, PapelEnum.ADM_OM, PapelEnum.ELB))).isEqualTo("ADM_OM, ELB, PUB_BCA");
        assertThat(DiferencaDeAcesso.lista(EnumSet.noneOf(PapelEnum.class))).isEqualTo("nenhum");
        assertThat(DiferencaDeAcesso.lista(null)).isEqualTo("nenhum");
    }
}
