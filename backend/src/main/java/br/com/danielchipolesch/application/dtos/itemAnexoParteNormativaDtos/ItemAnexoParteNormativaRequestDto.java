package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;

public record ItemAnexoParteNormativaRequestDto(
        Long parentId,
        ItemAnexoParteNormativaTipoEnum tipo,
        String titulo,
        String conteudo
) {
}
