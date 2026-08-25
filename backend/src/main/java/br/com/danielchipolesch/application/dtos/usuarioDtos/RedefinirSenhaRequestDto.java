package br.com.danielchipolesch.application.dtos.usuarioDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequestDto(

        @NotBlank
        @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres.")
        String novaSenha
) {
}
