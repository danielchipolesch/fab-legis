package br.com.danielchipolesch.application.dtos.emendaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import jakarta.validation.constraints.NotNull;

public record EmendaIncluirRequestDto(

        @NotNull
        ItemAnexoParteNormativaTipoEnum tipo,

        String titulo,

        String conteudo,

        // Apenas para PARTE_NORMATIVA: id do elemento pai (null = raiz)
        Long parentId,

        Integer elementOrder,

        @NotNull
        String justificativa,

        // Ver SecoesSaveRequestDto.versaoEsperada / DocumentoConcorrenciaService.
        Integer versaoEsperada
) {
}
