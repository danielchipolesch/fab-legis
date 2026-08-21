package br.com.danielchipolesch.application.dtos.authDtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private long expiresIn;
    private Long usuarioId;
    private String nome;
    private String cpf;
    private Long omId;
    private String omNome;
    private List<String> papeis;
}
