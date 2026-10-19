package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

// O que o Painel Gerencial mostra a quem pergunta (docs/funcionalidades.md, "Painel Gerencial"): o Painel Gerencial Geral (PNL_GER, só no CENDOC)
// vê os indicadores de todas as OMs e pode filtrar por uma; o Painel Gerencial da OM (PNL_OM) vê SÓ os da própria OM, sem visão geral -- o servidor
// fixa a OM, o filtro que a tela mandar não vale. As NPAs são de uso interno de cada OM (RegrasDaEspecieNormativa.deUsoInternoDaOm): só o painel
// da OM as conta -- o Geral nunca, nem filtrando por uma OM. (Isso é dos indicadores: qualquer OM continua vendo qualquer documento.)
public final class EscopoDoPainel {

    private EscopoDoPainel() {}

    public static boolean temAcesso(Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.PNL_GER) || usuario.getPapeis().contains(PapelEnum.PNL_OM);
    }

    public static boolean ehGeral(Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.PNL_GER);
    }

    // A OM pela qual os indicadores são filtrados: a pedida (ou nenhuma = todas) para o Painel Gerencial Geral; sempre a própria para o da OM
    // (pedir outra é 403 -- não há como espiar a visão geral nem a de outra OM).
    public static Long omDosIndicadores(Usuario usuario, Long omPedida) {
        if (ehGeral(usuario)) return omPedida;
        if (!usuario.getPapeis().contains(PapelEnum.PNL_OM)) {
            throw new OperacaoNaoAutorizadaException("O Painel Gerencial é dos papéis " + PapelEnum.PNL_OM.nome() + " e " + PapelEnum.PNL_GER.nome() + ".");
        }
        Long propria = usuario.getOm().getId();
        if (omPedida != null && !omPedida.equals(propria)) {
            throw new OperacaoNaoAutorizadaException("O " + PapelEnum.PNL_OM.nome() + " só vê os indicadores da própria OM.");
        }
        return propria;
    }

    // Os TIPOS de espécie que os indicadores contam: para o Painel Gerencial Geral, os de âmbito do COMAER (todos menos os de uso interno
    // da OM); para o da OM, todos ou o módulo que ele pedir. O Geral pedir um módulo de uso interno é 403.
    public static Set<TipoDeEspecie> tiposDosIndicadores(Usuario usuario, TipoDeEspecie pedido, Collection<TipoDeEspecie> deUsoInternoDaOm) {
        if (!ehGeral(usuario)) {
            return pedido == null ? EnumSet.allOf(TipoDeEspecie.class) : EnumSet.of(pedido);
        }
        if (pedido != null && deUsoInternoDaOm.contains(pedido)) {
            throw new OperacaoNaoAutorizadaException("O " + PapelEnum.PNL_GER.nome() + " não considera as NPAs: são de uso interno de cada OM e aparecem no "
                    + PapelEnum.PNL_OM.nome() + ".");
        }
        Set<TipoDeEspecie> tipos = pedido == null ? EnumSet.allOf(TipoDeEspecie.class) : EnumSet.of(pedido);
        tipos.removeAll(deUsoInternoDaOm);
        return tipos;
    }
}
