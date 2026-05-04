package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import lombok.Data;


@Data
public class ItemAnexoParteNormativaRequestDto {

    private Long parentId;
    private ItemAnexoParteNormativaTipoEnum tipo;  // Ex: "CAPÍTULO", "SEÇÃO", "ARTIGO"
    private String titulo; // Nome do item normativo
    private String conteuto; // Texto do item
}
