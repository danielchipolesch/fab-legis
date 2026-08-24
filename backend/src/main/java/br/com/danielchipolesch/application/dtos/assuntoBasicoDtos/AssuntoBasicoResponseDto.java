package br.com.danielchipolesch.application.dtos.assuntoBasicoDtos;

public record AssuntoBasicoResponseDto(
        Long idAssuntoBasico,
        String codigo,
        String nome,
        String descricao
) {
}
