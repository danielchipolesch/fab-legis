package br.com.danielchipolesch.application.dtos.especieNormativaDtos;

import lombok.Data;

@Data
public class DocumentationTypeRequestUpdateDto {

    private String acronym;
    private String name;
    private String description;
}
