package br.com.danielchipolesch.application.dtos.especieNormativaDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EspecieNormativaRequestCreateDto{

    @NotBlank(message = "Sigla não pode estar vazia")
    private String sigla;

    @NotBlank(message = "Nome não pode estar vazio")
    private String nome;

    @NotBlank(message = "Descrição não pode estar vazia")
    private String descricao;
}
