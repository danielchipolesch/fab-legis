package intraer.fablegis.domain.entities.estruturaDocumento;

import org.junit.jupiter.api.Test;

import static intraer.fablegis.domain.entities.estruturaDocumento.OrientacaoDoAnexo.PAISAGEM;
import static intraer.fablegis.domain.entities.estruturaDocumento.OrientacaoDoAnexo.RETRATO;
import static org.assertj.core.api.Assertions.assertThat;

// Regra de sugestão da orientação do anexo de imagem (docs/dominio.md): paisagem só se a imagem é mais larga que alta.
// O frontend espelha a regra em utils/orientacaoDoAnexo.js (mesmos cenários em orientacaoDoAnexo.test.js).
class OrientacaoDoAnexoTest {

    @Test
    void imagemMaisLargaQueAltaSugerePaisagem() {
        assertThat(OrientacaoDoAnexo.sugeridaPara(1600, 900)).isEqualTo(PAISAGEM);
        assertThat(OrientacaoDoAnexo.sugeridaPara(101, 100)).isEqualTo(PAISAGEM);
    }

    @Test
    void imagemMaisAltaQueLargaSugereRetrato() {
        assertThat(OrientacaoDoAnexo.sugeridaPara(900, 1600)).isEqualTo(RETRATO);
        assertThat(OrientacaoDoAnexo.sugeridaPara(100, 101)).isEqualTo(RETRATO);
    }

    @Test
    void imagemQuadradaFicaEmRetrato() {
        assertThat(OrientacaoDoAnexo.sugeridaPara(500, 500)).isEqualTo(RETRATO);
    }

    @Test
    void dimensoesInvalidasFicamEmRetrato() {
        assertThat(OrientacaoDoAnexo.sugeridaPara(0, 0)).isEqualTo(RETRATO);
        assertThat(OrientacaoDoAnexo.sugeridaPara(-10, -20)).isEqualTo(RETRATO);
        assertThat(OrientacaoDoAnexo.sugeridaPara(800, 0)).isEqualTo(RETRATO);
        assertThat(OrientacaoDoAnexo.sugeridaPara(0, 600)).isEqualTo(RETRATO);
    }
}
