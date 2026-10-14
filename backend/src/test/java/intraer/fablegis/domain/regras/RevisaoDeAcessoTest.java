package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.regras.RevisaoDeAcesso.Motivo;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

// Revisão periódica de acessos (docs/autenticacao.md): a cada 6 meses, ao trocar de OM e com 180 dias sem entrar; o geral confere
// só os admins locais (e o CENDOC), cada admin local os demais da própria OM, e ninguém o próprio acesso.
class RevisaoDeAcessoTest {

    private static final Instant AGORA = Instant.parse("2026-10-13T12:00:00Z");
    private static final Timestamp AGORA_TS = Timestamp.from(AGORA);

    private static Timestamp haDias(long dias) {
        return Timestamp.from(AGORA.minus(dias, ChronoUnit.DAYS));
    }

    private static Usuario usuario(Long id, String om, PapelEnum... papeis) {
        var o = new OrganizacaoMilitar();
        o.setSigla(om);
        var u = new Usuario();
        u.setId(id);
        u.setOm(o);
        var conjunto = EnumSet.noneOf(PapelEnum.class);
        conjunto.addAll(java.util.List.of(papeis));
        u.setPapeis(conjunto);
        u.setDtRevisaoAcesso(haDias(10));
        u.setDtUltimoAcesso(haDias(1));
        return u;
    }

    // ─── Quando há pendência ─────────────────────────────────────────────────

    @Test
    void soQuemTemPapelElevadoPrecisaDeRevisao() {
        var soEditor = usuario(1L, "BAGL", PapelEnum.EDIT);
        soEditor.setDtRevisaoAcesso(haDias(400));
        assertThat(RevisaoDeAcesso.temPapelElevado(soEditor)).isFalse();
        assertThat(RevisaoDeAcesso.motivo(soEditor, AGORA_TS)).isEqualTo(Motivo.NENHUM);

        assertThat(RevisaoDeAcesso.temPapelElevado(usuario(2L, "BAGL", PapelEnum.EDIT, PapelEnum.APROV))).isTrue();
    }

    @Test
    void dentroDoPrazoNaoHaPendencia() {
        var u = usuario(1L, "BAGL", PapelEnum.EDIT, PapelEnum.APROV);
        u.setDtRevisaoAcesso(haDias(179));
        assertThat(RevisaoDeAcesso.pendente(u, AGORA_TS)).isFalse();
    }

    @Test
    void depoisDe180DiasARevisaoVence() {
        var u = usuario(1L, "BAGL", PapelEnum.EDIT, PapelEnum.APROV);
        u.setDtRevisaoAcesso(haDias(181));
        assertThat(RevisaoDeAcesso.motivo(u, AGORA_TS)).isEqualTo(Motivo.REVISAO_VENCIDA);
    }

    @Test
    void trocarDeOmDeixaARevisaoPendenteDeImediato() {
        var u = usuario(1L, "BAGL", PapelEnum.EDIT, PapelEnum.AUDITOR);
        u.setDtRevisaoAcesso(null);
        assertThat(RevisaoDeAcesso.motivo(u, AGORA_TS)).isEqualTo(Motivo.TROCA_DE_OM);
    }

    @Test
    void contaSemEntrarHa180DiasEDormente() {
        var u = usuario(1L, "BAGL", PapelEnum.EDIT, PapelEnum.APROV);
        u.setDtUltimoAcesso(haDias(181));
        assertThat(RevisaoDeAcesso.motivo(u, AGORA_TS)).isEqualTo(Motivo.CONTA_DORMENTE);
        u.setDtUltimoAcesso(haDias(179));
        assertThat(RevisaoDeAcesso.pendente(u, AGORA_TS)).isFalse();
        u.setDtUltimoAcesso(null); // ainda nunca entrou: não é "dormente", só não entrou
        assertThat(RevisaoDeAcesso.pendente(u, AGORA_TS)).isFalse();
    }

    @Test
    void papeisElevadosSaoTodosMenosOEditor() {
        assertThat(RevisaoDeAcesso.papeisElevados(EnumSet.allOf(PapelEnum.class)))
                .containsExactlyInAnyOrder(PapelEnum.APROV, PapelEnum.PUBLIC, PapelEnum.ADMIN, PapelEnum.AUDITOR);
        assertThat(RevisaoDeAcesso.papeisElevados(null)).isEmpty();
    }

    // ─── Quem revisa quem ────────────────────────────────────────────────────

    @Test
    void oAdminGeralRevisaOsAdminsLocais() {
        var geral = usuario(1L, "CENDOC", PapelEnum.EDIT, PapelEnum.ADMIN);
        assertThat(RevisaoDeAcesso.podeRevisar(geral, usuario(2L, "BAGL", PapelEnum.EDIT, PapelEnum.ADMIN))).isTrue();
    }

    @Test
    void oAdminGeralNaoRevisaOsDemaisUsuariosDeOutraOm() {
        var geral = usuario(1L, "CENDOC", PapelEnum.EDIT, PapelEnum.ADMIN);
        assertThat(RevisaoDeAcesso.podeRevisar(geral, usuario(2L, "BAGL", PapelEnum.EDIT, PapelEnum.APROV))).isFalse();
    }

    @Test
    void oAdminLocalRevisaOsDemaisDaPropriaOm() {
        var local = usuario(1L, "BAGL", PapelEnum.EDIT, PapelEnum.ADMIN);
        assertThat(RevisaoDeAcesso.podeRevisar(local, usuario(2L, "BAGL", PapelEnum.EDIT, PapelEnum.APROV))).isTrue();
        assertThat(RevisaoDeAcesso.podeRevisar(local, usuario(3L, "BABE", PapelEnum.EDIT, PapelEnum.APROV))).isFalse();
    }

    @Test
    void oAdminLocalNaoRevisaOutroAdminNemOPessoalDoCendoc() {
        var local = usuario(1L, "BAGL", PapelEnum.EDIT, PapelEnum.ADMIN);
        assertThat(RevisaoDeAcesso.podeRevisar(local, usuario(2L, "BAGL", PapelEnum.EDIT, PapelEnum.ADMIN))).isFalse();
        assertThat(RevisaoDeAcesso.podeRevisar(local, usuario(3L, "CENDOC", PapelEnum.EDIT, PapelEnum.PUBLIC))).isFalse();
    }

    @Test
    void oPessoalDoCendocERevisadoPeloAdminGeralMasNuncaPeloProprio() {
        var geral = usuario(1L, "CENDOC", PapelEnum.EDIT, PapelEnum.ADMIN);
        assertThat(RevisaoDeAcesso.podeRevisar(geral, usuario(2L, "CENDOC", PapelEnum.EDIT, PapelEnum.PUBLIC))).isTrue();
        assertThat(RevisaoDeAcesso.podeRevisar(geral, usuario(3L, "CENDOC", PapelEnum.EDIT, PapelEnum.ADMIN))).isTrue();
        assertThat(RevisaoDeAcesso.podeRevisar(geral, geral)).isFalse();
    }

    @Test
    void quemNaoEAdminNaoRevisaNinguem() {
        var aprovador = usuario(1L, "BAGL", PapelEnum.EDIT, PapelEnum.APROV);
        assertThat(RevisaoDeAcesso.podeRevisar(aprovador, usuario(2L, "BAGL", PapelEnum.EDIT, PapelEnum.APROV))).isFalse();
    }
}
