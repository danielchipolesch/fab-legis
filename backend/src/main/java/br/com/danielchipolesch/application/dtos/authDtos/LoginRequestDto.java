package br.com.danielchipolesch.application.dtos.authDtos;

import br.com.danielchipolesch.application.validation.CpfValido;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank
    @CpfValido
    private String cpf;

    @NotBlank
    private String senha;
}
