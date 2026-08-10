package br.com.danielchipolesch.application.dtos.emendaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmendaIncluirRequestDto {

    @NotNull
    private ItemAnexoParteNormativaTipoEnum tipo;

    private String titulo;

    private String conteudo;

    // Apenas para PARTE_NORMATIVA: id do elemento pai (null = raiz)
    private Long parentId;

    private Integer elementOrder;

    @NotNull
    private String justificativa;
}
