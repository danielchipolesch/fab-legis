package intraer.fablegis.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.regex.Pattern;

public class EmailInstitucionalValidator implements ConstraintValidator<EmailInstitucional, String> {

    public static final String DOMINIO = "fab.mil.br";

    // Parte local simples (sem espaços, sem @) e exatamente o domínio da FAB: "fulano@fab.mil.br". Outro domínio, e também um
    // subdomínio ou um sufixo enganoso ("fulano@fab.mil.br.exemplo.com", "fulano@xfab.mil.br"), é recusado.
    private static final Pattern PADRAO = Pattern.compile("^[^@\\s]+@" + Pattern.quote(DOMINIO) + "$");

    public static boolean isValid(String email) {
        return email != null && PADRAO.matcher(email.trim().toLowerCase(Locale.ROOT)).matches();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // O e-mail é opcional: vazio é válido; só se valida o domínio quando algo foi enviado.
        if (value == null || value.isBlank()) return true;
        return isValid(value);
    }
}
