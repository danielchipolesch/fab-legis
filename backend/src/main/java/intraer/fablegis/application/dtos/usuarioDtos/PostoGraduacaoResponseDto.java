package intraer.fablegis.application.dtos.usuarioDtos;

import intraer.fablegis.domain.entities.usuario.PostoGraduacao;

public record PostoGraduacaoResponseDto(
        Long id,
        String nome,
        String bigrama
) {
    public static PostoGraduacaoResponseDto from(PostoGraduacao p) {
        return new PostoGraduacaoResponseDto(p.getId(), p.getNome(), p.getBigrama());
    }
}
