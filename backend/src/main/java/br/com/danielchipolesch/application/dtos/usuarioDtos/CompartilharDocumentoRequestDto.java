package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.application.validation.CpfValido;
import jakarta.validation.constraints.NotBlank;

public record CompartilharDocumentoRequestDto(

        @NotBlank
        @CpfValido
        String cpf
) {
}
