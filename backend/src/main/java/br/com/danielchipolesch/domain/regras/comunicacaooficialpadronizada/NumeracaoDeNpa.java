package br.com.danielchipolesch.domain.regras.comunicacaooficialpadronizada;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.regras.CalculadoraDeNumeracaoDosElementos;
import br.com.danielchipolesch.domain.regras.ElementoNumeracao;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Numeração da NPA: algarismo arábico, pelo CAMINHO do elemento na estrutura.
//   capítulo   -> 1, 2, 3
//   seção      -> 1.1, 1.2       (número do capítulo . posição)
//   subseção   -> 1.1.1          (número da seção . posição)
//   parágrafo  -> 1.1.1.1        (número do pai . posição), direto sob capítulo (3.1), seção (2.1.1) ou subseção
//   alínea     -> a), b), c)     (letra pela posição entre as alíneas do parágrafo)
// Seção, subseção e parágrafo do MESMO pai dividem uma única sequência: num capítulo com a seção 1.1, um parágrafo
// que venha depois dela recebe 1.2. Todo elemento é numerado, e a numeração é sempre recalculada -- não há emenda,
// então nada é congelado nem recebe sufixo de letra (a NPA só é publicada ou revogada, nunca alterada).
@Component
public class NumeracaoDeNpa implements CalculadoraDeNumeracaoDosElementos {

    @Override
    public Map<Long, ElementoNumeracao> calcular(List<ItemAnexoParteNormativaResponseDto> normativos) {
        Map<Long, ElementoNumeracao> resultado = new HashMap<>();
        int capitulo = 0;
        for (var item : normativos) {
            if (item.elementType() != ItemAnexoParteNormativaTipoEnum.CAPITULO) continue;
            capitulo++;
            resultado.put(item.id(), new ElementoNumeracao(capitulo, null, String.valueOf(capitulo)));
            numerarFilhos(item, String.valueOf(capitulo), resultado);
        }
        return resultado;
    }

    private void numerarFilhos(ItemAnexoParteNormativaResponseDto pai, String caminhoDoPai,
                               Map<Long, ElementoNumeracao> resultado) {
        int posicao = 0;
        int alinea = 0;
        for (var filho : pai.children()) {
            switch (filho.elementType()) {
                case SECAO_NORMATIVA, SUBSECAO_NORMATIVA, PARAGRAFO -> {
                    posicao++;
                    String caminho = caminhoDoPai + "." + posicao;
                    resultado.put(filho.id(), new ElementoNumeracao(posicao, null, caminho));
                    numerarFilhos(filho, caminho, resultado);
                }
                case ALINEA -> {
                    alinea++;
                    resultado.put(filho.id(), new ElementoNumeracao(alinea, null, letra(alinea) + ")"));
                }
                default -> {
                    // Tipo fora da gramática da NPA (a hierarquia o recusa no salvamento): não recebe número.
                }
            }
        }
    }

    // a..z, depois aa, ab... (uma NPA raramente passa de 26 alíneas, mas a numeração nunca deve falhar).
    static String letra(int posicao) {
        var sb = new StringBuilder();
        for (int n = posicao; n > 0; n = (n - 1) / 26) {
            sb.insert(0, (char) ('a' + (n - 1) % 26));
        }
        return sb.toString();
    }
}
