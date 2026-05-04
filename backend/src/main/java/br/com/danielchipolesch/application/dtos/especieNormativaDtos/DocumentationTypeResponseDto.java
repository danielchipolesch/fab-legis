package br.com.danielchipolesch.application.dtos.especieNormativaDtos;

import lombok.Data;

@Data
public class DocumentationTypeResponseDto {

    private Long id;
    private String acronym;
    private String name;
    private String description;
}
