package intraer.fablegis.application.dtos.usuarioDtos;

import intraer.fablegis.domain.entities.usuario.PerfilEnum;
import intraer.fablegis.application.validation.EmailInstitucional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UsuarioUpdateRequestDto(

        @NotBlank
        String nome,

        String nomeGuerra,

        @Email(message = "E-mail inválido")
        @EmailInstitucional
        String email,

        Long postoGraduacaoId,

        @NotNull
        Long omId,

        @NotNull
        Boolean ativo,

        Set<PerfilEnum> perfis
) {
    public UsuarioUpdateRequestDto {
        if (perfis == null) perfis = Set.of();
    }
}
