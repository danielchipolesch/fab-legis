package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import lombok.Data;

import java.util.List;

@Data
public class SecoesSaveRequestDto {
    private List<SecaoItemRequestDto> itens;
}
