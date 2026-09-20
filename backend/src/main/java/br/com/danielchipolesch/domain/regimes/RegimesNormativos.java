package br.com.danielchipolesch.domain.regimes;

import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Onde o sistema descobre as regras de uma espécie: cada EspecieNormativa aponta para um RegimeNormativo, e
// este registro entrega o RegimeDoDocumento correspondente. Toda implementação de RegimeDoDocumento registrada
// como bean entra sozinha -- acrescentar um regime não exige mexer aqui.
@Component
public class RegimesNormativos {

    private final Map<RegimeNormativo, RegimeDoDocumento> porRegime = new EnumMap<>(RegimeNormativo.class);

    public RegimesNormativos(List<RegimeDoDocumento> regimes) {
        for (var regime : regimes) {
            porRegime.put(regime.regime(), regime);
        }
    }

    public RegimeDoDocumento para(EspecieNormativa especie) {
        return para(especie.getRegime());
    }

    public RegimeDoDocumento para(RegimeNormativo regime) {
        var encontrado = porRegime.get(regime != null ? regime : RegimeNormativo.ATO_NORMATIVO);
        if (encontrado == null) {
            throw new IllegalStateException("Nenhum regime normativo registrado para " + regime + ".");
        }
        return encontrado;
    }
}
