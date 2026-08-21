package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.application.validation.CpfValido;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompartilharDocumentoRequestDto {

    @NotBlank
    @CpfValido
    private String cpf;
}
