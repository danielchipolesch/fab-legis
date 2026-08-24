package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;
import java.util.List;

public record UsuarioResponseDto(

        Long id,
        String nome,
        String cpf,
        Long omId,
        String omNome,
        boolean ativo,
        List<String> papeis,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtCriacao
) {
    public static UsuarioResponseDto from(Usuario usuario) {
        return new UsuarioResponseDto(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getOm().getId(),
                usuario.getOm().getNome(),
                usuario.isAtivo(),
                usuario.getPapeis().stream().map(Enum::name).toList(),
                usuario.getDtCriacao()
        );
    }
}
