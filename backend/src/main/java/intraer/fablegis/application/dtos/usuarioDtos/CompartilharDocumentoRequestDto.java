package intraer.fablegis.application.dtos.usuarioDtos;

import intraer.fablegis.application.validation.CpfValido;
import jakarta.validation.constraints.NotBlank;

public record CompartilharDocumentoRequestDto(

        @NotBlank
        @CpfValido
        String cpf
) {
}
