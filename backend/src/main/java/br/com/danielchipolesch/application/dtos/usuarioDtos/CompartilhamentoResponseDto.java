package br.com.danielchipolesch.application.dtos.usuarioDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoCompartilhamento;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class CompartilhamentoResponseDto {

    private Long usuarioId;
    private String nome;
    private String cpf;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
    private Timestamp dtCompartilhamento;

    public static CompartilhamentoResponseDto from(DocumentoCompartilhamento c) {
        return new CompartilhamentoResponseDto(
                c.getUsuario().getId(),
                c.getUsuario().getNome(),
                c.getUsuario().getCpf(),
                c.getDtCompartilhamento()
        );
    }
}
