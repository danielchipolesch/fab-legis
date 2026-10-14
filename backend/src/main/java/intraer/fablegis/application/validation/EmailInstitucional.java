package intraer.fablegis.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// O e-mail de um usuário do sistema precisa ser institucional, do domínio @fab.mil.br (vazio é permitido: o e-mail é opcional).
@Documented
@Constraint(validatedBy = EmailInstitucionalValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailInstitucional {
    String message() default "Informe um e-mail institucional, do domínio @fab.mil.br";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
