package br.com.danielchipolesch.application.dtos.anexoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Anexo;

public record AnexoResponseDto(
        Long id,
        String titulo,
        String urlImagem,
        Integer ordem
) {
    public static AnexoResponseDto from(Anexo a) {
        return new AnexoResponseDto(a.getId(), a.getTitulo(), a.getUrlImagem(), a.getOrdem());
    }
}
