package br.com.danielchipolesch.domain.mappers;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;

public class ItemAnexoParteNormativaMapper {

    public static ItemAnexoParteNormativaResponseDto toDto(ItemAnexoParteNormativa item) {
        return ItemAnexoParteNormativaResponseDto.from(item);
    }
}
