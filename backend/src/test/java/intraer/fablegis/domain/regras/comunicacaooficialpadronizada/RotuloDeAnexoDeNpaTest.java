package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

// Numa NPA os anexos ficam ao final, rotulados A, B, C...; o primeiro anexo (ordem 1) é o ANEXO A -- não há "ANEXO I"
// reservado ao corpo normativo como nos atos normativos.
class RotuloDeAnexoDeNpaTest {

    private final RotuloDeAnexoDeNpa rotulo = new RotuloDeAnexoDeNpa();

    @ParameterizedTest(name = "ordem {0} é {1}")
    @CsvSource({"1, ANEXO A", "2, ANEXO B", "3, ANEXO C", "26, ANEXO Z", "27, ANEXO AA"})
    void osAnexosSaoLetrados(int ordem, String esperado) {
        assertThat(rotulo.rotulo(ordem)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "ordem {0} tem a letra {1}")
    @CsvSource({"1, A", "2, B", "26, Z", "28, AB"})
    void aLetraSozinhaServeParaAListaDoCabecalho(int ordem, String esperada) {
        assertThat(RotuloDeAnexoDeNpa.letra(ordem)).isEqualTo(esperada);
    }
}
