package br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemPartePreliminar;

import java.time.LocalDateTime;

public record ItemPartePreliminarResponseDto(
        Long id,
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ItemPartePreliminarResponseDto from(ItemPartePreliminar item) {
        return new ItemPartePreliminarResponseDto(
                item.getId(),
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
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
