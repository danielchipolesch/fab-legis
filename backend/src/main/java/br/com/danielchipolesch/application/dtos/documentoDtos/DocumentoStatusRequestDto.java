package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoStatusRequestDto {

    @NotNull
    private DocumentoStatusEnum status;

    // Obrigatórios apenas ao publicar (transição para PUBLICADO)
    private String orgaoPortaria;
    private String setorPortaria;
    private String numeroPortaria;
    private LocalDate dataPortaria;
    private Integer numeroBca;
    private LocalDate dataBca;
}
