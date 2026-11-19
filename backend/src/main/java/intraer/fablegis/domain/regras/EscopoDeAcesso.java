package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;

import java.util.EnumSet;
import java.util.Set;

// Escopo de acesso (docs/autenticacao.md, "Escopo de acesso"): só o CENDOC tem o escopo GERAL; toda outra OM tem escopo LOCAL. O
// escopo está no próprio papel, e o papel só combina com a lotação certa:
//
//   Administrador Geral (ADM_GER) e Auditor Geral (AUD_GER): só no CENDOC. Os de outra OM são ADM_OM e AUD_OM, que não existem no CENDOC.
//   Administrador da OM (ADM_OM): existe em TODA OM, o CENDOC inclusive (o CENDOC também é uma unidade, com o seu administrador local ao lado do
//                Administrador Geral). Só vê e gerencia os usuários da própria OM; concede só os papéis locais (CONCEDIVEIS_PELO_ADMIN_LOCAL) e
//                nunca os do escopo geral nem ADM_OM; não altera outros administradores (esses são mantidos pelo administrador geral).
//   Publicador do BCA (PUB_BCA): só no CENDOC -- a publicação no BCA é do CENDOC.
//   Painel Gerencial Geral (PNL_GER): só no CENDOC. O Painel Gerencial da OM (PNL_OM) existe em TODA OM, o CENDOC inclusive: ele é uma OM sujeita
//                às mesmas regras das demais, e a visão geral é a exceção inerente à sua função de órgão central (quem tem os dois escolhe a visão).
public final class EscopoDeAcesso {

    public static final String OM_GERAL = "CENDOC";

    // Papéis que só existem lotados no CENDOC, e os que só existem fora dele.
    public static final Set<PapelEnum> SO_NO_CENDOC = EnumSet.of(PapelEnum.PUB_BCA, PapelEnum.ADM_GER, PapelEnum.AUD_GER, PapelEnum.PNL_GER);
    public static final Set<PapelEnum> SO_FORA_DO_CENDOC = EnumSet.of(PapelEnum.AUD_OM);

    // Os papéis que o administrador LOCAL concede e retira; os demais (ADM_GER, ADM_OM, PUB_BCA, AUD_GER, PNL_GER) são do administrador geral.
    public static final Set<PapelEnum> CONCEDIVEIS_PELO_ADMIN_LOCAL = EnumSet.of(
            PapelEnum.ELB, PapelEnum.APR_OM, PapelEnum.PUB_BI, PapelEnum.AUD_OM, PapelEnum.PNL_OM);

    private EscopoDeAcesso() {}

    public static boolean ehOmGeral(String siglaDaOm) {
        return OM_GERAL.equalsIgnoreCase(siglaDaOm);
    }

    public static String siglaDaOm(Usuario usuario) {
        return usuario != null && usuario.getOm() != null ? usuario.getOm().getSigla() : null;
    }

    // Lotado no CENDOC (o escopo geral).
    public static boolean temEscopoGeral(Usuario usuario) {
        return ehOmGeral(siglaDaOm(usuario));
    }

    public static boolean ehAdminGeral(Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.ADM_GER);
    }

    public static boolean ehAdminLocal(Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.ADM_OM);
    }

    // Administrador de qualquer escopo.
    public static boolean ehAdmin(Usuario usuario) {
        return ehAdminGeral(usuario) || ehAdminLocal(usuario);
    }

    // Quem age enxerga (lista, consulta) este usuário? O administrador geral vê todos; o local (o do CENDOC inclusive), só a própria OM.
    public static boolean enxerga(Usuario ator, Usuario alvo) {
        return ehAdminGeral(ator) || mesmaOm(ator, alvo);
    }

    public static boolean mesmaOm(Usuario a, Usuario b) {
        var sa = siglaDaOm(a);
        return sa != null && sa.equalsIgnoreCase(String.valueOf(siglaDaOm(b)));
    }

    // O administrador da OM só administra usuários da própria OM que não sejam administradores (esses são do admin geral); o geral, todos.
    public static void exigirPodeAdministrar(Usuario ator, Usuario alvo) {
        if (ehAdminGeral(ator)) return;
        if (!mesmaOm(ator, alvo)) {
            throw new OperacaoNaoAutorizadaException("O " + PapelEnum.ADM_OM.nome() + " só gerencia usuários da própria OM.");
        }
        if (ehAdmin(alvo)) {
            throw new OperacaoNaoAutorizadaException("Os administradores são mantidos pelo " + PapelEnum.ADM_GER.nome() + " (CENDOC).");
        }
    }

    // O CENDOC nunca fica sem administrador geral (ADM_GER): quem hoje é administrador geral ativo só deixa de ser -- por perder o papel,
    // ser desativado ou mudar de OM -- se houver outro ativo. `adminsGeraisAtivos` conta os de agora, ele incluído.
    public static void exigirAoMenosUmAdminGeral(boolean eraAdminGeralAtivo, boolean continuaAdminGeralAtivo, long adminsGeraisAtivos) {
        if (eraAdminGeralAtivo && !continuaAdminGeralAtivo && adminsGeraisAtivos <= 1) {
            throw new OperacaoNaoAutorizadaException("É preciso manter ao menos um " + PapelEnum.ADM_GER.nome()
                    + " ativo (lotado no CENDOC). Conceda o papel a outra pessoa do CENDOC antes.");
        }
    }

    // Cada papel só combina com a lotação certa: os do CENDOC (ADM_GER, AUD_GER, PUB_BCA, PNL_GER) só nele; AUD_OM nunca nele (ADM_OM existe em toda OM).
    public static void exigirPapeisDaLotacao(String siglaDaOm, Set<PapelEnum> papeis) {
        boolean cendoc = ehOmGeral(siglaDaOm);
        for (PapelEnum papel : papeis) {
            if (!cendoc && SO_NO_CENDOC.contains(papel)) {
                throw new OperacaoNaoAutorizadaException("O papel " + papel.nome() + " é exclusivo do CENDOC: só quem é lotado no CENDOC o recebe.");
            }
            if (cendoc && SO_FORA_DO_CENDOC.contains(papel)) {
                throw new OperacaoNaoAutorizadaException("No CENDOC não existe o papel " + papel.nome() + ": use "
                        + geralCorrespondente(papel).nome() + ".");
            }
        }
    }

    // O papel geral que corresponde ao da OM (no CENDOC não existem os da OM).
    private static PapelEnum geralCorrespondente(PapelEnum papelDaOm) {
        return switch (papelDaOm) {
            case ADM_OM -> PapelEnum.ADM_GER;
            case AUD_OM -> PapelEnum.AUD_GER;
            default -> throw new IllegalArgumentException("O papel " + papelDaOm + " existe também no CENDOC.");
        };
    }

    // Valida uma criação (alvo == null) ou alteração de usuário -- OM de destino e papéis que ele passa a ter. Lança
    // OperacaoNaoAutorizadaException (403) com a regra violada; não lança se a operação é permitida.
    public static void exigirPodeGerenciar(Usuario ator, Usuario alvo, String siglaDaOmDeDestino, Set<PapelEnum> papeisDepois) {
        exigirPapeisDaLotacao(siglaDaOmDeDestino, papeisDepois);
        if (ehAdminGeral(ator)) return;

        if (siglaDaOmDeDestino == null || !siglaDaOmDeDestino.equalsIgnoreCase(String.valueOf(siglaDaOm(ator)))) {
            throw new OperacaoNaoAutorizadaException("O " + PapelEnum.ADM_OM.nome() + " só gerencia usuários da própria OM.");
        }
        if (alvo != null) exigirPodeAdministrar(ator, alvo);

        // O administrador local só mexe nos papéis locais: conceder ou retirar qualquer outro é do geral. Os que o alvo já tem e ele não concede
        // podem continuar (não mudam), mas não entram nem saem por ele.
        var antes = EnumSet.noneOf(PapelEnum.class);
        if (alvo != null && alvo.getPapeis() != null) antes.addAll(alvo.getPapeis());
        for (PapelEnum papel : PapelEnum.values()) {
            if (antes.contains(papel) != papeisDepois.contains(papel) && !CONCEDIVEIS_PELO_ADMIN_LOCAL.contains(papel)) {
                throw new OperacaoNaoAutorizadaException("Só o " + PapelEnum.ADM_GER.nome() + " (CENDOC) concede ou retira o papel " + papel.nome() + ".");
            }
        }
    }
}
