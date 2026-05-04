package br.com.danielchipolesch.domain.mappers;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;


public class ItemAnexoParteNormativaMapper {

    public static ItemAnexoParteNormativaResponseDto itemAnexoParteNormativaToItemAnexoParteNormativaResponseDto(ItemAnexoParteNormativa itemAnexoParteNormativa){
        return new ItemAnexoParteNormativaResponseDto(
                itemAnexoParteNormativa.getId(),
                itemAnexoParteNormativa.getTipo(),
                itemAnexoParteNormativa.getTitulo(),
                itemAnexoParteNormativa.getConteuto(),
                ItemAnexoParteNormativaMapper.itemAnexoParteNormativaToItemAnexoParteNormativaResponseDto(itemAnexoParteNormativa.getParent()),
                itemAnexoParteNormativa.getChildren()
//                itemAnexoParteNormativa.getChildren().stream().map(ItemAnexoParteNormativaMapper::itemAnexoParteNormativaToItemAnexoParteNormativaResponseDto).toList()

        );
    }
}
