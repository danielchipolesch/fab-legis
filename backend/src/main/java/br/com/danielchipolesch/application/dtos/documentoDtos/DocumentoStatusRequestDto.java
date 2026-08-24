package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DocumentoStatusRequestDto(

        @NotNull
        DocumentoStatusEnum status,

        // Obrigatórios apenas ao publicar (transição para PUBLICADO)
        String orgaoPortaria,
        String setorPortaria,
        String numeroPortaria,
        LocalDate dataPortaria,
        Integer numeroBca,
        LocalDate dataBca
) {
}
