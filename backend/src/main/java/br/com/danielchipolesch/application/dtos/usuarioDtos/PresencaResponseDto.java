package br.com.danielchipolesch.application.dtos.usuarioDtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresencaResponseDto {
    private Long usuarioId;
    private String nome;
}
