package br.com.danielchipolesch.domain.regras.atonormativo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

// Num ato normativo o ANEXO I é o sumário + corpo normativo: o primeiro anexo de imagem (ordem 1) é o ANEXO II.
class RotuloDeAnexoDeAtoNormativoTest {

    private final RotuloDeAnexoDeAtoNormativo rotulo = new RotuloDeAnexoDeAtoNormativo();

    @ParameterizedTest(name = "ordem {0} é {1}")
    @CsvSource({"1, ANEXO II", "2, ANEXO III", "3, ANEXO IV", "8, ANEXO IX", "9, ANEXO X", "13, ANEXO XIV"})
    void osAnexosDeImagemComecamNoAnexoII(int ordem, String esperado) {
        assertThat(rotulo.rotulo(ordem)).isEqualTo(esperado);
    }
}
