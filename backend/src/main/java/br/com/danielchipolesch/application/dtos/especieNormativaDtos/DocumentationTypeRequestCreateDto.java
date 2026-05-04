package br.com.danielchipolesch.application.dtos.especieNormativaDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentationTypeRequestCreateDto{

    @NotBlank(message = "Sigla não pode estar vazia")
    private String acronym;

    @NotBlank(message = "Nome não pode estar vazio")
    private String name;

    @NotBlank(message = "Descrição não pode estar vazia")
    private String description;
}
