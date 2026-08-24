package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.application.validation.CpfValido;
import br.com.danielchipolesch.domain.entities.usuario.PapelEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioCreateRequestDto(

        @NotBlank
        String nome,

        String nomeGuerra,

        @NotBlank
        @CpfValido
        String cpf,

        @Email(message = "E-mail inválido")
        String email,

        Long postoGraduacaoId,

        @NotBlank
        @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres.")
        String senha,

        @NotNull
        Long omId,

        Set<PapelEnum> papeis
) {
    public UsuarioCreateRequestDto {
        if (papeis == null) papeis = Set.of();
    }
}
