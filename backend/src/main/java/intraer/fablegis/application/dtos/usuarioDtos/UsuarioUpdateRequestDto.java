package intraer.fablegis.application.dtos.usuarioDtos;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UsuarioUpdateRequestDto(

        @NotBlank
        String nome,

        String nomeGuerra,

        @Email(message = "E-mail inválido")
        String email,

        Long postoGraduacaoId,

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
