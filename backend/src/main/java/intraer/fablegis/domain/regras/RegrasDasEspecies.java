package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Onde o sistema descobre as regras de uma espécie: cada EspecieNormativa aponta para um TipoDeEspecie, e este
// registro entrega a RegrasDaEspecieNormativa correspondente. Toda implementação registrada como bean entra
// sozinha -- acrescentar uma espécie com regras próprias não exige mexer aqui.
@Component
public class RegrasDasEspecies {

    private final Map<TipoDeEspecie, RegrasDaEspecieNormativa> porTipo = new EnumMap<>(TipoDeEspecie.class);

    public RegrasDasEspecies(List<RegrasDaEspecieNormativa> regras) {
        for (var r : regras) {
            porTipo.put(r.tipo(), r);
        }
    }

    public RegrasDaEspecieNormativa para(EspecieNormativa especie) {
        return para(especie.getTipoDeEspecie());
    }

    // Os tipos de espécie que são (ou não) de uso interno da OM -- para as consultas que separam as NPAs do resto.
    public List<TipoDeEspecie> tipos(boolean deUsoInternoDaOm) {
        return porTipo.values().stream().filter(r -> r.deUsoInternoDaOm() == deUsoInternoDaOm).map(RegrasDaEspecieNormativa::tipo).toList();
    }

    public RegrasDaEspecieNormativa para(TipoDeEspecie tipo) {
        var encontrada = porTipo.get(tipo != null ? tipo : TipoDeEspecie.CONVENCIONAL);
        if (encontrada == null) {
            throw new IllegalStateException("Nenhuma regra registrada para o tipo " + tipo + ".");
        }
        return encontrada;
    }
}
