package br.com.danielchipolesch.application.validation;

import br.com.danielchipolesch.domain.util.CpfValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidoValidator implements ConstraintValidator<CpfValido, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // @NotBlank cuida do caso nulo/vazio separadamente; aqui só validamos o
        // formato quando algo foi de fato enviado.
        if (value == null || value.isBlank()) return true;
        return CpfValidator.isValid(value);
    }
}
