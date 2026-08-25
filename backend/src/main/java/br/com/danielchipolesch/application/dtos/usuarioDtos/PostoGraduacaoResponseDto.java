package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.domain.entities.usuario.PostoGraduacao;

public record PostoGraduacaoResponseDto(
        Long id,
        String nome,
        String bigrama
) {
    public static PostoGraduacaoResponseDto from(PostoGraduacao p) {
        return new PostoGraduacaoResponseDto(p.getId(), p.getNome(), p.getBigrama());
    }
}
