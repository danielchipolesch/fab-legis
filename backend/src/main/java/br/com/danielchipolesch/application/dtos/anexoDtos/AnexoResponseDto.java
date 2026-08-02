package br.com.danielchipolesch.application.dtos.anexoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Anexo;
import lombok.Data;

@Data
public class AnexoResponseDto {

    private Long id;
    private String titulo;
    private String urlImagem;
    private Integer ordem;

    public static AnexoResponseDto from(Anexo a) {
        AnexoResponseDto dto = new AnexoResponseDto();
        dto.setId(a.getId());
        dto.setTitulo(a.getTitulo());
        dto.setUrlImagem(a.getUrlImagem());
        dto.setOrdem(a.getOrdem());
        return dto;
    }
}
