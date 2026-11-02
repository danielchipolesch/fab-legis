package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

// O que o Painel Gerencial mostra a quem pergunta (docs/funcionalidades.md, "Painel Gerencial"). Há duas visões:
//   - GERAL (papel Painel Gerencial Geral, PNL_GER, só no CENDOC): os indicadores de todas as OMs, com filtro por uma;
//   - OM (papel Painel Gerencial da OM, PNL_OM): SÓ os da própria OM, sem visão geral -- o servidor fixa a OM, o filtro que a tela mandar não vale.
// O CENDOC é uma OM como as outras e pode ter o PNL_OM; a visão geral é a exceção inerente à sua função de órgão central. Quem tem os dois papéis
// escolhe a visão (padrão: a geral); quem tem só um papel tem só a visão dele (pedir a outra é 403).
// As NPAs são de uso interno de cada OM (RegrasDaEspecieNormativa.deUsoInternoDaOm): só a visão da OM as conta -- a Geral nunca, nem filtrando
// por uma OM. (Isso é dos indicadores: qualquer OM continua vendo qualquer documento.)
public final class EscopoDoPainel {

    public enum Visao { GERAL, OM }

    private EscopoDoPainel() {}

    public static boolean temAcesso(Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.PNL_GER) || usuario.getPapeis().contains(PapelEnum.PNL_OM);
    }

    // A visão em que os indicadores são calculados: a pedida, se o papel a permite; sem pedido, a única que o papel dá (ou a geral, para quem tem as duas).
    public static Visao visaoDe(Usuario usuario, Visao pedida) {
        boolean geral = usuario.getPapeis().contains(PapelEnum.PNL_GER);
        boolean daOm = usuario.getPapeis().contains(PapelEnum.PNL_OM);
        if (!geral && !daOm) {
            throw new OperacaoNaoAutorizadaException("O Painel Gerencial é dos papéis " + PapelEnum.PNL_OM.nome() + " e " + PapelEnum.PNL_GER.nome() + ".");
        }
        if (geral && daOm) return pedida == null ? Visao.GERAL : pedida;
        Visao unica = geral ? Visao.GERAL : Visao.OM;
        if (pedida != null && pedida != unica) {
            throw new OperacaoNaoAutorizadaException("Você tem só o papel " + (geral ? PapelEnum.PNL_GER : PapelEnum.PNL_OM).nome() + ": a outra visão do painel não é sua.");
        }
        return unica;
    }

    // A OM pela qual os indicadores são filtrados: a pedida (ou nenhuma = todas) na visão geral; sempre a própria na visão da OM
    // (pedir outra é 403 -- não há como espiar a visão geral nem a de outra OM por ela).
    public static Long omDosIndicadores(Usuario usuario, Visao visao, Long omPedida) {
        if (visao == Visao.GERAL) return omPedida;
        Long propria = usuario.getOm().getId();
        if (omPedida != null && !omPedida.equals(propria)) {
            throw new OperacaoNaoAutorizadaException("A visão da OM só mostra os indicadores da própria OM.");
        }
        return propria;
    }

    // Os TIPOS de espécie que os indicadores contam: na visão geral, os de âmbito do COMAER (todos menos os de uso interno da OM); na da OM, todos
    // ou o módulo que ela pedir. Pedir, na visão geral, um módulo de uso interno é 403.
    public static Set<TipoDeEspecie> tiposDosIndicadores(Visao visao, TipoDeEspecie pedido, Collection<TipoDeEspecie> deUsoInternoDaOm) {
        if (visao == Visao.OM) {
            return pedido == null ? EnumSet.allOf(TipoDeEspecie.class) : EnumSet.of(pedido);
        }
        if (pedido != null && deUsoInternoDaOm.contains(pedido)) {
            throw new OperacaoNaoAutorizadaException("A visão geral do painel não considera as NPAs: são de uso interno de cada OM e aparecem no "
                    + PapelEnum.PNL_OM.nome() + ".");
        }
        Set<TipoDeEspecie> tipos = pedido == null ? EnumSet.allOf(TipoDeEspecie.class) : EnumSet.of(pedido);
        tipos.removeAll(deUsoInternoDaOm);
        return tipos;
    }
}
