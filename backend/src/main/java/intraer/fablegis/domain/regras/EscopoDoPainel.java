package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.usuario.PerfilEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;

// O que o Painel Gerencial mostra a quem pergunta (docs/funcionalidades.md, "Painel Gerencial"): o Painel Gerencial Geral (PNL_GER, só no CENDOC)
// vê os indicadores de todas as OMs e pode filtrar por uma; o Painel Gerencial da OM (PNL_OM) vê SÓ os da própria OM, sem visão geral -- o servidor
// fixa a OM, o filtro que a tela mandar não vale.
public final class EscopoDoPainel {

    private EscopoDoPainel() {}

    public static boolean temAcesso(Usuario usuario) {
        return usuario.getPerfis().contains(PerfilEnum.PNL_GER) || usuario.getPerfis().contains(PerfilEnum.PNL_OM);
    }

    public static boolean ehGeral(Usuario usuario) {
        return usuario.getPerfis().contains(PerfilEnum.PNL_GER);
    }

    // A OM pela qual os indicadores são filtrados: a pedida (ou nenhuma = todas) para o Painel Gerencial Geral; sempre a própria para o da OM
    // (pedir outra é 403 -- não há como espiar a visão geral nem a de outra OM).
    public static Long omDosIndicadores(Usuario usuario, Long omPedida) {
        if (ehGeral(usuario)) return omPedida;
        if (!usuario.getPerfis().contains(PerfilEnum.PNL_OM)) {
            throw new OperacaoNaoAutorizadaException("O Painel Gerencial é dos perfis " + PerfilEnum.PNL_OM.nome() + " e " + PerfilEnum.PNL_GER.nome() + ".");
        }
        Long propria = usuario.getOm().getId();
        if (omPedida != null && !omPedida.equals(propria)) {
            throw new OperacaoNaoAutorizadaException("O " + PerfilEnum.PNL_OM.nome() + " só vê os indicadores da própria OM.");
        }
        return propria;
    }
}
