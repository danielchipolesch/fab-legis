package br.com.danielchipolesch.application.dtos.itemParteFinalDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemParteFinal;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItemParteFinalResponseDto {

    private Long id;
    private ItemAnexoParteNormativaTipoEnum elementType;
    private Integer elementOrder;
    private String elementTitle;
    private String elementContent;
    private String fullTextContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ItemParteFinalResponseDto from(ItemParteFinal item) {
        ItemParteFinalResponseDto dto = new ItemParteFinalResponseDto();
        dto.setId(item.getId());
        dto.setElementType(item.getTipo());
        dto.setElementOrder(item.getElementOrder());
        dto.setElementTitle(item.getTitulo());
        dto.setElementContent(item.getConteudo());
        dto.setFullTextContent(item.getFullTextContent());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }
}
