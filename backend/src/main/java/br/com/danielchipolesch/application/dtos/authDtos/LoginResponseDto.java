package br.com.danielchipolesch.application.dtos.authDtos;

import java.util.List;

public record LoginResponseDto(
        String token,
        long expiresIn,
        String refreshToken,
        Long usuarioId,
        String nome,
        String cpf,
        Long omId,
        String omNome,
        List<String> papeis
) {
}
