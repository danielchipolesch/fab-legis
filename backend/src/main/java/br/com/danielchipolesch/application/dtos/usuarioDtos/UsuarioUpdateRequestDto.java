package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.domain.entities.usuario.PapelEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UsuarioUpdateRequestDto(

        @NotBlank
        String nome,

        @NotNull
        Long omId,

        @NotNull
        Boolean ativo,

        Set<PapelEnum> papeis
) {
    public UsuarioUpdateRequestDto {
        if (papeis == null) papeis = Set.of();
    }
}
