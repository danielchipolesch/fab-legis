package br.com.danielchipolesch.application.dtos.authDtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(

        @NotBlank
        String refreshToken
) {
}
