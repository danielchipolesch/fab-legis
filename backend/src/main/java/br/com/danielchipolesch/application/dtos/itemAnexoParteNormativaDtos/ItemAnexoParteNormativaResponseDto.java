package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.mappers.ItemAnexoParteNormativaMapper;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class ItemAnexoParteNormativaResponseDto {

    private Long id;
    private ItemAnexoParteNormativaTipoEnum tipo;  // Ex: "CAPÍTULO", "SEÇÃO", "ARTIGO"
    private String titulo; // Nome do item normativo
    private String conteuto; // Texto do item
    private ItemAnexoParteNormativaResponseDto parent;
    private List<ItemAnexoParteNormativa> children;
//    private List<ItemAnexoParteNormativaResponseDto> children; // Lista de elementos filhos

//    public ItemAnexoParteNormativaResponseDto(Long id, ItemAnexoParteNormativaTipoEnum tipo, String titulo, String conteuto, ItemAnexoParteNormativaResponseDto parent, List<ItemAnexoParteNormativa> children) {
//        this.id = id;
//        this.tipo = tipo;
//        this.titulo = titulo;
//        this.conteuto = conteuto;
//        this.parent = parent;
//        this.children = children.stream().map(ItemAnexoParteNormativaMapper::itemAnexoParteNormativaToItemAnexoParteNormativaResponseDto).toList();
//    }
}
