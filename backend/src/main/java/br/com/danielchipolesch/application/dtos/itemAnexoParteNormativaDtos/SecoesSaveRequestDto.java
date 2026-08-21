package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import lombok.Data;

import java.util.List;

@Data
public class SecoesSaveRequestDto {
    private List<SecaoItemRequestDto> itens;

    // Versão do documento (Documento.versao) que o cliente tinha quando
    // carregou os dados que está salvando agora -- ver
    // DocumentoConcorrenciaService. Null desativa a checagem (compatibilidade
    // com chamadas antigas), mas o editor sempre a envia.
    private Integer versaoEsperada;
}
