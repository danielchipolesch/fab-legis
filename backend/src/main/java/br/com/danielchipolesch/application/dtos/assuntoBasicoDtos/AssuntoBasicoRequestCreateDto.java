package br.com.danielchipolesch.application.dtos.assuntoBasicoDtos;

import jakarta.validation.constraints.NotBlank;

public record AssuntoBasicoRequestCreateDto(

        @NotBlank(message = "Número básico não pode estar vazio")
        String codigo,

        @NotBlank(message = "Classificação não pode estar vazia")
        String nome,

        @NotBlank(message = "Descrição não pode estar vazia")
        String descricao
) {
}
