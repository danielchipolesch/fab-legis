package br.com.danielchipolesch.application.dtos.documentoDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record DocumentoHistoricoResponseDto(
        Long id,
        TipoAlteracaoEnum tipoAlteracao,
        String descricao,
        DocumentoStatusEnum statusAnterior,
        DocumentoStatusEnum statusNovo,
        String usuario,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        LocalDateTime dtRegistro
) {}
