package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record ItemAnexoParteNormativaResponseDto(
        Long id,
        Long parentId,
        ItemAnexoParteNormativaTipoEnum elementType,
        Integer elementOrder,
        String elementTitle,
        String elementContent,
        String fullTextContent,
        ElementoEmendaStatusEnum emendaStatus,
        String conteudoEmenda,
        String tituloEmenda,
        String justificativaEmenda,
        String clausulaEmenda,
        String clausulaEmendaAnterior,
        boolean incluidoPorEmenda,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ItemAnexoParteNormativaResponseDto> children
) {
    public static ItemAnexoParteNormativaResponseDto from(ItemAnexoParteNormativa item) {
        List<ItemAnexoParteNormativa> childrenEntities = item.getChildren();
        List<ItemAnexoParteNormativaResponseDto> children = (childrenEntities != null && !childrenEntities.isEmpty())
                ? childrenEntities.stream().map(ItemAnexoParteNormativaResponseDto::from).toList()
                : Collections.emptyList();

        return new ItemAnexoParteNormativaResponseDto(
                item.getId(),
                item.getParent() != null ? item.getParent().getId() : null,
                item.getTipo(),
                item.getElementOrder(),
                item.getTitulo(),
                item.getConteudo(),
                item.getFullTextContent(),
                item.getEmendaStatus(),
                item.getConteudoEmenda(),
                item.getTituloEmenda(),
                item.getJustificativaEmenda(),
                item.getClausulaEmenda(),
                item.getClausulaEmendaAnterior(),
                item.isIncluidoPorEmenda(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                children
        );
    }
}
