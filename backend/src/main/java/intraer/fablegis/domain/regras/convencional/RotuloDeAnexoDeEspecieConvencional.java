package intraer.fablegis.domain.regras.convencional;

import intraer.fablegis.domain.regras.RotuloDosAnexos;
import intraer.fablegis.domain.services.NumeracaoService;
import org.springframework.stereotype.Component;

// Anexos de um ato normativo: o ANEXO I é o sumário + corpo normativo, então o primeiro anexo de imagem
// (ordem 1) é o ANEXO II, e assim por diante, em romano.
@Component
public class RotuloDeAnexoDeEspecieConvencional implements RotuloDosAnexos {

    @Override
    public String rotulo(int ordem) {
        return "ANEXO " + NumeracaoService.toRoman(ordem + 1);
    }
}
