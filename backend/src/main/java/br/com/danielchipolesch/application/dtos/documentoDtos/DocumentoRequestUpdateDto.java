package br.com.danielchipolesch.application.dtos.documentoDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoRequestUpdateDto {

    @NotBlank
    private String tituloDocumento;
}
