package br.com.danielchipolesch.application.dtos.documentoDtos;

import jakarta.validation.constraints.NotBlank;

public record DocumentoRequestUpdateDto(

        @NotBlank
        String tituloDocumento,

        Integer numeroSecundario
) {
}
