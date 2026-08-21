package br.com.danielchipolesch.application.dtos.emendaDtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmendaElementoRequestDto {

    @NotNull
    private EmendaAcaoEnum acao;

    private String novoConteudo;

    private String novoTitulo;

    // Obrigatória para ALTERAR e REVOGAR; ignorada em DESFAZER
    private String justificativa;

    // Ver SecoesSaveRequestDto.versaoEsperada / DocumentoConcorrenciaService.
    private Integer versaoEsperada;
}
