package br.com.danielchipolesch.application.dtos.comentarioDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ComentarioElemento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;

import java.sql.Timestamp;

public record ComentarioElementoResponseDto(
        Long id,
        Long elementoId,
        SecaoDocumentoEnum secao,
        Long autorId,
        String autorNome,
        String texto,
        Long parentId,
        boolean resolvido,
        Timestamp dtCriacao,
        Timestamp dtResolucao
) {
    public static ComentarioElementoResponseDto from(ComentarioElemento c) {
        return new ComentarioElementoResponseDto(
                c.getId(),
                c.getElementoId(),
                c.getSecao(),
                c.getAutor().getId(),
                c.getAutor().getNome(),
                c.getTexto(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.isResolvido(),
                c.getDtCriacao(),
                c.getDtResolucao()
        );
    }
}
