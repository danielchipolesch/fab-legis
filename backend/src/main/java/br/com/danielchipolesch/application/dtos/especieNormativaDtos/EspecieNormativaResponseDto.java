package br.com.danielchipolesch.application.dtos.especieNormativaDtos;

public record EspecieNormativaResponseDto(
        Long id,
        String sigla,
        String nome,
        String descricao
) {
}
