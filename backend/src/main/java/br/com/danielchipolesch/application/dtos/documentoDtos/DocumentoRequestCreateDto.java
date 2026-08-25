package br.com.danielchipolesch.application.dtos.documentoDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentoRequestCreateDto(

        @NotNull
        Long idEspecieNormativa,

        @NotNull
        Long idAssuntoBasico,

        @NotBlank
        String tituloDocumento
) {
}
