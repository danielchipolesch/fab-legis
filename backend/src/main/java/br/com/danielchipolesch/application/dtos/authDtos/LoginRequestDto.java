package br.com.danielchipolesch.application.dtos.authDtos;

import br.com.danielchipolesch.application.validation.CpfValido;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(

        @NotBlank
        @CpfValido
        String cpf,

        @NotBlank
        String senha
) {
}
