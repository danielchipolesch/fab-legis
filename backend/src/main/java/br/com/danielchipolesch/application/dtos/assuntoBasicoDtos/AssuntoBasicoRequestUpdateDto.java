package br.com.danielchipolesch.application.dtos.assuntoBasicoDtos;

public record AssuntoBasicoRequestUpdateDto(
        String codigo,
        String nome,
        String descricao
) {
}
