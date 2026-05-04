package br.com.danielchipolesch.application.dtos.documentoDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentoRequestCreateDto {

    @NotNull
    private Long IdEspecieNormativa;

    @NotNull
    private Long IdAssuntoBasico;

    @NotBlank
    private String tituloDocumento;
}
