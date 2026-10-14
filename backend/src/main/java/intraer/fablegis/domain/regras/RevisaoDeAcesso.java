package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.PerfilEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

// Revisão periódica de acessos (docs/autenticacao.md, "Revisão periódica de acessos"). Quem tem perfil ELEVADO -- qualquer um
// além do Editor, que todo usuário tem -- precisa ter o acesso reconfirmado:
//   - a cada 6 meses (180 dias) desde a última revisão;
//   - ao trocar de OM (a revisão fica pendente de imediato);
//   - quando a conta passa 180 dias sem entrar (conta dormente).
// Quem revisa: o admin GERAL (CENDOC) confere SÓ os admins locais (e o pessoal do próprio CENDOC); cada admin LOCAL confere os
// demais usuários da própria OM. Ninguém revisa o próprio acesso. Por ora só avisa (lista de pendências): nada é suspenso sozinho.
public final class RevisaoDeAcesso {

    public static final Duration PRAZO_DE_REVISAO = Duration.ofDays(180);
    public static final Duration PRAZO_DE_CONTA_DORMENTE = Duration.ofDays(180);

    public enum Motivo {
        // Nada a revisar agora (dentro do prazo) ou nada elevado a revisar.
        NENHUM,
        // Trocou de OM (ou nunca foi revisado): a revisão está pendente.
        TROCA_DE_OM,
        // Passaram-se 180 dias desde a última revisão.
        REVISAO_VENCIDA,
        // 180 dias sem entrar no sistema.
        CONTA_DORMENTE
    }

    private RevisaoDeAcesso() {}

    // Os perfis que exigem revisão: todos menos o Editor.
    public static Set<PerfilEnum> perfisElevados(Set<PerfilEnum> perfis) {
        var elevados = EnumSet.noneOf(PerfilEnum.class);
        if (perfis != null) {
            elevados.addAll(perfis);
        }
        elevados.remove(PerfilEnum.ELB);
        return elevados;
    }

    public static boolean temPerfilElevado(Usuario usuario) {
        return !perfisElevados(usuario.getPerfis()).isEmpty();
    }

    public static Motivo motivo(Usuario usuario, Timestamp agora) {
        if (!temPerfilElevado(usuario)) return Motivo.NENHUM;
        if (usuario.getDtRevisaoAcesso() == null) return Motivo.TROCA_DE_OM;
        if (passou(usuario.getDtUltimoAcesso(), PRAZO_DE_CONTA_DORMENTE, agora)) return Motivo.CONTA_DORMENTE;
        if (passou(usuario.getDtRevisaoAcesso(), PRAZO_DE_REVISAO, agora)) return Motivo.REVISAO_VENCIDA;
        return Motivo.NENHUM;
    }

    public static boolean pendente(Usuario usuario, Timestamp agora) {
        return motivo(usuario, agora) != Motivo.NENHUM;
    }

    private static boolean passou(Timestamp desde, Duration prazo, Timestamp agora) {
        return desde != null && desde.toInstant().plus(prazo).isBefore(agora.toInstant());
    }

    // Quem pode revisar o acesso do alvo. Ninguém revisa o próprio; só admin revisa.
    //   - o alvo é um admin LOCAL (ADMIN fora do CENDOC): só o admin geral;
    //   - o alvo é do CENDOC (escopo geral): o admin geral (outro que não ele mesmo);
    //   - o alvo é de outra OM e não é admin: só o admin local da mesma OM (o geral não -- confere só os admins locais).
    public static boolean podeRevisar(Usuario revisor, Usuario alvo) {
        if (revisor == null || alvo == null || revisor.getId() != null && revisor.getId().equals(alvo.getId())) return false;
        if (!EscopoDeAcesso.ehAdmin(revisor)) return false;

        boolean revisorGeral = EscopoDeAcesso.ehAdminGeral(revisor);
        boolean alvoGeral = EscopoDeAcesso.temEscopoGeral(alvo);
        boolean alvoAdmin = EscopoDeAcesso.ehAdmin(alvo);

        if (alvoGeral || alvoAdmin) return revisorGeral;
        return !revisorGeral && EscopoDeAcesso.mesmaOm(revisor, alvo);
    }
}
