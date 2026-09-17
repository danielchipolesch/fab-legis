package br.com.danielchipolesch.application.dtos.comentarioDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComentarioElementoCreateRequestDto(

        @NotNull
        Long elementoId,

        @NotNull
        SecaoDocumentoEnum secao,

        @NotBlank
        String texto,

        // Preenchido só quando é resposta a um comentário existente -- ver
        // ComentarioElementoService.criar.
        Long parentId
) {
}
