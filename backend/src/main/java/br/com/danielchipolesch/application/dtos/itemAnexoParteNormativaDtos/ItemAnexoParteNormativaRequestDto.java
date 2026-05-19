package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import lombok.Data;

@Data
public class ItemAnexoParteNormativaRequestDto {

    private Long parentId;
    private ItemAnexoParteNormativaTipoEnum tipo;
    private String titulo;
    private String conteudo;
}
