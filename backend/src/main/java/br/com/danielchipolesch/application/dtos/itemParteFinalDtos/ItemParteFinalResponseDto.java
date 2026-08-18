package br.com.danielchipolesch.application.dtos.itemParteFinalDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
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
    private ElementoEmendaStatusEnum emendaStatus;
    private String conteudoEmenda;
    private String tituloEmenda;
    private String justificativaEmenda;
    private String clausulaEmenda;
    private String clausulaEmendaAnterior;
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
        dto.setEmendaStatus(item.getEmendaStatus());
        dto.setConteudoEmenda(item.getConteudoEmenda());
        dto.setTituloEmenda(item.getTituloEmenda());
        dto.setJustificativaEmenda(item.getJustificativaEmenda());
        dto.setClausulaEmenda(item.getClausulaEmenda());
        dto.setClausulaEmendaAnterior(item.getClausulaEmendaAnterior());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }
}
