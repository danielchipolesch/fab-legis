package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.services.NumeracaoService;
import lombok.Data;

// Numeração calculada de um elemento da parte normativa — mesmo cálculo usado no PDF
// oficial (NumeracaoService), exposto para qualquer consumidor da API (hoje: o gerador
// de PDF; no futuro, o frontend, eliminando a duplicação hoje mantida à mão em
// frontend/src/utils/numbering.js).
@Data
public class NumeracaoElementoResponseDto {

    private Long elementoId;
    private Integer numero;
    private String letra;
    private String label;

    public static NumeracaoElementoResponseDto from(Long elementoId, NumeracaoService.ElementoNumeracao en) {
        NumeracaoElementoResponseDto dto = new NumeracaoElementoResponseDto();
        dto.setElementoId(elementoId);
        dto.setNumero(en.numero());
        dto.setLetra(en.letra());
        dto.setLabel(en.label());
        return dto;
    }
}
