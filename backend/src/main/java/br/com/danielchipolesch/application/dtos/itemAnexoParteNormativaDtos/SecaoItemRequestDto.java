package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;

import java.util.List;

public record SecaoItemRequestDto(
        SecaoDocumentoEnum secao,
        ItemAnexoParteNormativaTipoEnum tipo,
        Integer elementOrder,
        String titulo,
        String conteudo,
        String fullTextContent,
        List<SecaoItemRequestDto> filhos
) {
}
