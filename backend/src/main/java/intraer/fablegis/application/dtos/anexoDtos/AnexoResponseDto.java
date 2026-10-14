package intraer.fablegis.application.dtos.anexoDtos;

import intraer.fablegis.domain.entities.estruturaDocumento.Anexo;
import intraer.fablegis.domain.entities.estruturaDocumento.OrientacaoDoAnexo;

public record AnexoResponseDto(
        Long id,
        String titulo,
        String urlImagem,
        Integer ordem,
        OrientacaoDoAnexo orientacao
) {
    // Anexo em retrato, a orientação padrão.
    public AnexoResponseDto(Long id, String titulo, String urlImagem, Integer ordem) {
        this(id, titulo, urlImagem, ordem, OrientacaoDoAnexo.RETRATO);
    }

    public static AnexoResponseDto from(Anexo a) {
        return new AnexoResponseDto(a.getId(), a.getTitulo(), a.getUrlImagem(), a.getOrdem(), a.getOrientacao());
    }
}
