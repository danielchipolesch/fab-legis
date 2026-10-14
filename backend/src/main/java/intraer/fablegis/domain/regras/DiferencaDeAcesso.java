package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.auditoria.AcaoDeAcessoEnum;
import intraer.fablegis.domain.entities.usuario.PerfilEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// O que mudou no acesso de um usuário entre dois estados -- perfis, situação (ativo) e OM --, em eventos de auditoria prontos
// (um por tipo de mudança, com o detalhe legível). Regra pura: quem grava é a AuditoriaDeAcessoService, na transação da mudança.
public final class DiferencaDeAcesso {

    public record Evento(AcaoDeAcessoEnum acao, String detalhe) {}

    // O estado de acesso de um usuário num instante.
    public record Estado(Set<PerfilEnum> perfis, boolean ativo, String omSigla) {}

    private DiferencaDeAcesso() {}

    public static List<Evento> entre(Estado antes, Estado depois) {
        var eventos = new ArrayList<Evento>();

        Set<PerfilEnum> concedidos = EnumSet.noneOf(PerfilEnum.class);
        concedidos.addAll(depois.perfis());
        concedidos.removeAll(antes.perfis());
        Set<PerfilEnum> retirados = EnumSet.noneOf(PerfilEnum.class);
        retirados.addAll(antes.perfis());
        retirados.removeAll(depois.perfis());
        if (!concedidos.isEmpty() || !retirados.isEmpty()) {
            var partes = new ArrayList<String>();
            if (!concedidos.isEmpty()) partes.add("Concedido(s): " + lista(concedidos));
            if (!retirados.isEmpty()) partes.add("Retirado(s): " + lista(retirados));
            eventos.add(new Evento(AcaoDeAcessoEnum.ALTEROU_PERFIS,
                    String.join(". ", partes) + ". Perfis agora: " + lista(depois.perfis())));
        }

        if (antes.ativo() != depois.ativo()) {
            eventos.add(new Evento(depois.ativo() ? AcaoDeAcessoEnum.ATIVOU_USUARIO : AcaoDeAcessoEnum.DESATIVOU_USUARIO, null));
        }

        if (!Objects.equals(antes.omSigla(), depois.omSigla())) {
            eventos.add(new Evento(AcaoDeAcessoEnum.TROCOU_OM, antes.omSigla() + " → " + depois.omSigla()));
        }
        return eventos;
    }

    // Os perfis em ordem alfabética, ou "nenhum".
    public static String lista(Set<PerfilEnum> perfis) {
        if (perfis == null || perfis.isEmpty()) return "nenhum";
        return perfis.stream().sorted(Comparator.comparing(Enum::name)).map(Enum::name).collect(Collectors.joining(", "));
    }
}
