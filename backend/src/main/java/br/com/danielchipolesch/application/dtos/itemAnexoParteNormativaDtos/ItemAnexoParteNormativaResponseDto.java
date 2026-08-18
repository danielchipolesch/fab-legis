package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class ItemAnexoParteNormativaResponseDto {

    private Long id;
    private Long parentId;
    private ItemAnexoParteNormativaTipoEnum elementType;
    private Integer elementOrder;
    private String elementTitle;
    private String elementContent;
    private String fullTextContent;
    private ElementoEmendaStatusEnum emendaStatus;
    private String conteudoEmenda;
    private String tituloEmenda;
    private String justificativaEmenda;
    private String clausulaEmenda;
    private boolean incluidoPorEmenda;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItemAnexoParteNormativaResponseDto> children;

    public static ItemAnexoParteNormativaResponseDto from(ItemAnexoParteNormativa item) {
        ItemAnexoParteNormativaResponseDto dto = new ItemAnexoParteNormativaResponseDto();
        dto.setId(item.getId());
        dto.setParentId(item.getParent() != null ? item.getParent().getId() : null);
        dto.setElementType(item.getTipo());
        dto.setElementOrder(item.getElementOrder());
        dto.setElementTitle(item.getTitulo());
        dto.setElementContent(item.getConteudo());
        dto.setFullTextContent(item.getFullTextContent());
        dto.setEmendaStatus(item.getEmendaStatus());
        dto.setConteudoEmenda(item.getConteudoEmenda());
        dto.setTituloEmenda(item.getTituloEmenda());
        dto.setJustificativaEmenda(item.getJustificativaEmenda());
        dto.setClausulaEmenda(item.getClausulaEmenda());
        dto.setIncluidoPorEmenda(item.isIncluidoPorEmenda());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        List<ItemAnexoParteNormativa> childrenEntities = item.getChildren();
        if (childrenEntities != null && !childrenEntities.isEmpty()) {
            dto.setChildren(childrenEntities.stream()
                    .map(ItemAnexoParteNormativaResponseDto::from)
                    .toList());
        } else {
            dto.setChildren(Collections.emptyList());
        }
        return dto;
    }
}
