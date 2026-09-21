package intraer.fablegis.application.dtos.assuntoBasicoDtos;

public record AssuntoBasicoRequestUpdateDto(
        String codigo,
        String nome,
        String descricao
) {
}
