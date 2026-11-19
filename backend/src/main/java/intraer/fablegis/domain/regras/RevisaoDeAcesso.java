package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

// Revisão periódica de acessos (docs/autenticacao.md, "Revisão periódica de acessos"). Quem tem papel ELEVADO -- qualquer um
// além do Editor, que todo usuário tem -- precisa ter o acesso reconfirmado:
//   - a cada 6 meses (180 dias) desde a última revisão;
//   - ao trocar de OM (a revisão fica pendente de imediato);
//   - quando a conta passa 180 dias sem entrar (conta dormente; contados do último acesso ou da última revisão, o que for mais recente).
// Revisto o acesso (Manter ou Retirar), a pendência some e só volta quando um desses três motivos se repete: a tela só oferece a revisão
// enquanto há pendência, e o servidor recusa revisar o que está em dia.
// Quem revisa: o admin GERAL (CENDOC) confere SÓ os admins locais (e o pessoal do próprio CENDOC); cada admin LOCAL (o do CENDOC inclusive) confere os
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

    // Os papéis que exigem revisão: todos menos o Editor.
    public static Set<PapelEnum> papeisElevados(Set<PapelEnum> papeis) {
        var elevados = EnumSet.noneOf(PapelEnum.class);
        if (papeis != null) {
            elevados.addAll(papeis);
        }
        elevados.remove(PapelEnum.ELB);
        return elevados;
    }

    public static boolean temPapelElevado(Usuario usuario) {
        return !papeisElevados(usuario.getPapeis()).isEmpty();
    }

    public static Motivo motivo(Usuario usuario, Timestamp agora) {
        if (!temPapelElevado(usuario)) return Motivo.NENHUM;
        if (usuario.getDtRevisaoAcesso() == null) return Motivo.TROCA_DE_OM;
        if (contaDormente(usuario, agora)) return Motivo.CONTA_DORMENTE;
        if (passou(usuario.getDtRevisaoAcesso(), PRAZO_DE_REVISAO, agora)) return Motivo.REVISAO_VENCIDA;
        return Motivo.NENHUM;
    }

    // 180 dias sem entrar, contados do último acesso ou da última revisão, o que for mais recente: reconfirmar o acesso de uma conta dormente
    // zera a pendência (senão o "Manter acesso" não surtiria efeito e a pendência nunca sairia). Quem ainda nunca entrou não é dormente.
    private static boolean contaDormente(Usuario usuario, Timestamp agora) {
        var ultimoAcesso = usuario.getDtUltimoAcesso();
        if (ultimoAcesso == null) return false;
        var revisao = usuario.getDtRevisaoAcesso();
        var referencia = revisao != null && revisao.after(ultimoAcesso) ? revisao : ultimoAcesso;
        return passou(referencia, PRAZO_DE_CONTA_DORMENTE, agora);
    }

    public static boolean pendente(Usuario usuario, Timestamp agora) {
        return motivo(usuario, agora) != Motivo.NENHUM;
    }

    private static boolean passou(Timestamp desde, Duration prazo, Timestamp agora) {
        return desde != null && desde.toInstant().plus(prazo).isBefore(agora.toInstant());
    }

    // Quem pode revisar o acesso do alvo. Ninguém revisa o próprio; só admin revisa.
    //   - o alvo é admin (local ou geral): só o admin geral;
    //   - o alvo é do CENDOC e não é admin: o admin geral ou o admin local do CENDOC (nunca o próprio);
    //   - o alvo é de outra OM e não é admin: só o admin local da mesma OM (o geral não -- confere só os admins locais).
    public static boolean podeRevisar(Usuario revisor, Usuario alvo) {
        if (revisor == null || alvo == null || revisor.getId() != null && revisor.getId().equals(alvo.getId())) return false;
        if (!EscopoDeAcesso.ehAdmin(revisor)) return false;

        boolean revisorGeral = EscopoDeAcesso.ehAdminGeral(revisor);
        boolean alvoGeral = EscopoDeAcesso.temEscopoGeral(alvo);
        boolean alvoAdmin = EscopoDeAcesso.ehAdmin(alvo);

        if (alvoAdmin) return revisorGeral;
        // O pessoal do CENDOC (que não é administrador) tem dois possíveis revisores: o administrador geral e o administrador local do CENDOC.
        if (revisorGeral) return alvoGeral;
        return EscopoDeAcesso.mesmaOm(revisor, alvo);
    }
}
