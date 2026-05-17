package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import lombok.Data;

import java.util.List;

@Data
public class SecaoItemRequestDto {
    private SecaoDocumentoEnum secao;
    private ItemAnexoParteNormativaTipoEnum tipo;
    private String titulo;
    private String conteudo;
    private List<SecaoItemRequestDto> filhos;
}
