package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoStatusRequestDto {

    @NotNull
    private DocumentoStatusEnum status;

    // Obrigatórios apenas ao transicionar de EM_ALTERACAO para PUBLICADO
    private String portariaReferencia;
    private String bcaReferencia;
}
