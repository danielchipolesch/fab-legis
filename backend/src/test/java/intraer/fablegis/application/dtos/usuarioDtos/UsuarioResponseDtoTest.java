package intraer.fablegis.application.dtos.usuarioDtos;

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

// A revisão de acesso só é oferecida (revisavel) enquanto há pendência: revisto o acesso, o botão some até um dos três motivos voltar.
class UsuarioResponseDtoTest {

    private static Timestamp haDias(long dias) {
        return Timestamp.from(Instant.now().minus(dias, ChronoUnit.DAYS));
    }

    private static Usuario usuario(Long id, String sigla, Timestamp revisao, PapelEnum... papeis) {
        var om = new OrganizacaoMilitar();
        om.setId(1L);
        om.setSigla(sigla);
        om.setNome(sigla);
        var u = new Usuario();
        u.setId(id);
        u.setNome("Fulano");
        u.setOm(om);
        var conjunto = EnumSet.noneOf(PapelEnum.class);
        conjunto.addAll(java.util.List.of(papeis));
        u.setPapeis(conjunto);
        u.setDtRevisaoAcesso(revisao);
        u.setDtUltimoAcesso(haDias(1));
        return u;
    }

    private static final Usuario ADMIN_LOCAL = usuario(1L, "BAGL", haDias(1), PapelEnum.ELB, PapelEnum.ADM_OM);

    @Test
    void comRevisaoVencidaQuemConfereVeOsBotoes() {
        var dto = UsuarioResponseDto.from(usuario(2L, "BAGL", haDias(200), PapelEnum.ELB, PapelEnum.APR_OM), ADMIN_LOCAL);
        assertThat(dto.motivoDaRevisao()).isEqualTo(Motivo.REVISAO_VENCIDA);
        assertThat(dto.revisavel()).isTrue();
    }

    @Test
    void revistoEmDiaOsBotoesSomem() {
        var dto = UsuarioResponseDto.from(usuario(2L, "BAGL", haDias(1), PapelEnum.ELB, PapelEnum.APR_OM), ADMIN_LOCAL);
        assertThat(dto.motivoDaRevisao()).isEqualTo(Motivo.NENHUM);
        assertThat(dto.revisavel()).isFalse();
    }

    @Test
    void trocarDeOmVoltaAOferecerARevisao() {
        var dto = UsuarioResponseDto.from(usuario(2L, "BAGL", null, PapelEnum.ELB, PapelEnum.APR_OM), ADMIN_LOCAL);
        assertThat(dto.motivoDaRevisao()).isEqualTo(Motivo.TROCA_DE_OM);
        assertThat(dto.revisavel()).isTrue();
    }

    @Test
    void quemNaoConfereNuncaVeOsBotoesMesmoComPendencia() {
        var vencido = usuario(2L, "BABE", haDias(200), PapelEnum.ELB, PapelEnum.APR_OM);
        assertThat(UsuarioResponseDto.from(vencido, ADMIN_LOCAL).revisavel()).isFalse();
        assertThat(UsuarioResponseDto.from(vencido).revisavel()).isFalse();
    }
}
