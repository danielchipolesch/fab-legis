package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// O DOM (distintivo da OM) da NPA: PNG ou JPEG, até 1 MB (docs/dominio.md, "NPA").
class DistintivoDaOmTest {

    @Test
    void aceitaPngEJpegDeAte1Mb() {
        assertThatCode(() -> DistintivoDaOm.validar("image/png", 1000)).doesNotThrowAnyException();
        assertThatCode(() -> DistintivoDaOm.validar("image/jpeg", 1024 * 1024)).doesNotThrowAnyException();
        assertThatCode(() -> DistintivoDaOm.validar("IMAGE/PNG", 10)).doesNotThrowAnyException();
    }

    @Test
    void recusaOutrosFormatos() {
        assertThatThrownBy(() -> DistintivoDaOm.validar("image/gif", 10)).isInstanceOf(InvalidInputException.class).hasMessageContaining("PNG ou JPEG");
        assertThatThrownBy(() -> DistintivoDaOm.validar("application/pdf", 10)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> DistintivoDaOm.validar(null, 10)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void recusaArquivoVazioOuMaiorQue1Mb() {
        assertThatThrownBy(() -> DistintivoDaOm.validar("image/png", 0)).isInstanceOf(InvalidInputException.class).hasMessageContaining("vazio");
        assertThatThrownBy(() -> DistintivoDaOm.validar("image/png", 1024 * 1024 + 1)).isInstanceOf(InvalidInputException.class).hasMessageContaining("1 MB");
    }
}
