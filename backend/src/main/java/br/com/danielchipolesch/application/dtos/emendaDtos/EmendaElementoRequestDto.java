package br.com.danielchipolesch.application.dtos.emendaDtos;

import jakarta.validation.constraints.NotNull;

public record EmendaElementoRequestDto(

        @NotNull
        EmendaAcaoEnum acao,

        String novoConteudo,

        String novoTitulo,

        // Obrigatória para ALTERAR e REVOGAR; ignorada em DESFAZER
        String justificativa,

        // Ver SecoesSaveRequestDto.versaoEsperada / DocumentoConcorrenciaService.
        Integer versaoEsperada
) {
}
