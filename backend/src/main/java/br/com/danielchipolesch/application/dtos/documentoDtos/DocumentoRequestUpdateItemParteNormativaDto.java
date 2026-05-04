package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoRequestUpdateItemParteNormativaDto {

    private Long ParentId;

    @NotBlank
    private ItemAnexoParteNormativaTipoEnum tipo;  // Ex: "CAPÍTULO", "SEÇÃO", "ARTIGO"

    private String titulo; // Nome do item normativo

    @NotBlank(message = "O campo conteúdo não pode ser nulo")
    private String conteuto; // Texto do item
}
