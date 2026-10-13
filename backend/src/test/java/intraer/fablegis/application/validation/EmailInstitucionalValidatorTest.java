package intraer.fablegis.application.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailInstitucionalValidatorTest {

    private final EmailInstitucionalValidator validator = new EmailInstitucionalValidator();

    private boolean valido(String email) {
        return validator.isValid(email, null);
    }

    @Test
    void aceitaEmailDoDominioFab() {
        assertTrue(valido("fulano@fab.mil.br"));
        assertTrue(valido("  Fulano.Silva@FAB.MIL.BR "));
    }

    @Test
    void recusaOutrosDominios() {
        assertFalse(valido("fulano@gmail.com"));
        assertFalse(valido("fulano@mail.fab.mil.br"));
        assertFalse(valido("fulano@fab.mil.br.exemplo.com"));
        assertFalse(valido("fulano@xfab.mil.br"));
        assertFalse(valido("@fab.mil.br"));
    }

    @Test
    void vazioEhValidoPoisEmailEhOpcional() {
        assertTrue(valido(null));
        assertTrue(valido(""));
        assertTrue(valido("  "));
    }
}
