package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;

import java.util.Set;

// Escopo de acesso (docs/autenticacao.md, "Escopo de acesso"): só o CENDOC tem o escopo GERAL; toda outra OM tem escopo LOCAL.
// O escopo vem da OM de quem age -- não há um papel a mais para guardar: ADMIN e AUDITOR de uma pessoa do CENDOC são gerais,
// os de qualquer outra OM são locais.
//
//   Admin GERAL (ADMIN no CENDOC): todos os usuários de todas as OMs; é quem mantém os admins locais e cadastra OMs, habilitações e DOM.
//   Admin LOCAL (ADMIN em outra OM): só os usuários da própria OM; concede EDIT, APROV e AUDITOR (local), e nunca ADMIN nem PUBLIC;
//                não altera outros administradores (esses são mantidos pelo admin geral).
//   PUBLIC (publicador): só existe no CENDOC -- a publicação no BCA é do CENDOC.
public final class EscopoDeAcesso {

    public static final String OM_GERAL = "CENDOC";

    private EscopoDeAcesso() {}

    public static boolean ehOmGeral(String siglaDaOm) {
        return OM_GERAL.equalsIgnoreCase(siglaDaOm);
    }

    public static String siglaDaOm(Usuario usuario) {
        return usuario != null && usuario.getOm() != null ? usuario.getOm().getSigla() : null;
    }

    public static boolean temEscopoGeral(Usuario usuario) {
        return ehOmGeral(siglaDaOm(usuario));
    }

    public static boolean ehAdminGeral(Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.ADMIN) && temEscopoGeral(usuario);
    }

    public static boolean ehAdminLocal(Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.ADMIN) && !temEscopoGeral(usuario);
    }

    // Quem age enxerga (lista, consulta) este usuário? O escopo geral vê todos; o local, só a própria OM.
    public static boolean enxerga(Usuario ator, Usuario alvo) {
        return temEscopoGeral(ator) || mesmaOm(ator, alvo);
    }

    public static boolean mesmaOm(Usuario a, Usuario b) {
        var sa = siglaDaOm(a);
        return sa != null && sa.equalsIgnoreCase(String.valueOf(siglaDaOm(b)));
    }

    // O admin local só administra usuários da própria OM que não sejam administradores (esses são do admin geral); o geral, todos.
    public static void exigirPodeAdministrar(Usuario ator, Usuario alvo) {
        if (temEscopoGeral(ator)) return;
        if (!mesmaOm(ator, alvo)) {
            throw new OperacaoNaoAutorizadaException("O administrador local só gerencia usuários da própria OM.");
        }
        if (alvo.getPapeis().contains(PapelEnum.ADMIN)) {
            throw new OperacaoNaoAutorizadaException("Os administradores locais são mantidos pelo administrador geral (CENDOC).");
        }
    }

    // Valida uma criação (alvo == null) ou alteração de usuário -- OM de destino e papéis que ele passa a ter. Lança
    // OperacaoNaoAutorizadaException (403) com a regra violada; não lança se a operação é permitida.
    public static void exigirPodeGerenciar(Usuario ator, Usuario alvo, String siglaDaOmDeDestino, Set<PapelEnum> papeisDepois) {
        if (papeisDepois.contains(PapelEnum.PUBLIC) && !ehOmGeral(siglaDaOmDeDestino)) {
            throw new OperacaoNaoAutorizadaException(
                    "A publicação é exclusiva do CENDOC: só quem é da OM CENDOC pode ter o papel de publicador.");
        }
        if (temEscopoGeral(ator)) return;

        if (siglaDaOmDeDestino == null || !siglaDaOmDeDestino.equalsIgnoreCase(String.valueOf(siglaDaOm(ator)))) {
            throw new OperacaoNaoAutorizadaException("O administrador local só gerencia usuários da própria OM.");
        }
        if (alvo != null) exigirPodeAdministrar(ator, alvo);
        if (papeisDepois.contains(PapelEnum.ADMIN)) {
            throw new OperacaoNaoAutorizadaException("Só o administrador geral (CENDOC) concede o papel de administrador.");
        }
    }
}
