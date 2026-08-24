package br.com.danielchipolesch.application.dtos.especieNormativaDtos;

public record DocumentationTypeResponseDto(
        Long id,
        String acronym,
        String name,
        String description
) {
}
