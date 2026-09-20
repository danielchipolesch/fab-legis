package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record DocumentoHistoricoResponseDto(
        Long id,
        TipoAlteracaoEnum tipoAlteracao,
        String descricao,
        SituacaoLocalEnum statusAnterior,
        SituacaoLocalEnum statusNovo,
        String usuario,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        LocalDateTime dtRegistro
) {}
