package intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos;

import intraer.fablegis.domain.regras.ElementoNumeracao;
import intraer.fablegis.domain.services.NumeracaoService;

// Numeração calculada de um elemento da parte normativa — mesmo cálculo usado no PDF
// oficial (NumeracaoService), exposto para qualquer consumidor da API (hoje: o gerador
// de PDF; no futuro, o frontend, eliminando a duplicação hoje mantida à mão em
// frontend/src/utils/numbering.js).
public record NumeracaoElementoResponseDto(
        Long elementoId,
        Integer numero,
        String letra,
        String label
) {
    public static NumeracaoElementoResponseDto from(Long elementoId, ElementoNumeracao en) {
        return new NumeracaoElementoResponseDto(elementoId, en.numero(), en.letra(), en.label());
    }
}
