package br.com.danielchipolesch.application.dtos.assuntoBasicoDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public abstract class AssuntoBasicoMainDto {

    @NotBlank(message = "Número básico não pode estar vazio")
    private String codigo;

    @NotBlank(message = "Classificação não pode estar vazia")
    private String nome;

    @NotBlank(message = "Descrição não pode estar vazia")
    private String descricao;
}
