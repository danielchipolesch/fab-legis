package br.com.danielchipolesch.application.dtos.auditoriaDtos;

import br.com.danielchipolesch.domain.entities.auditoria.AcaoAuditoriaEnum;
import br.com.danielchipolesch.domain.entities.auditoria.LogAuditoria;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;

public record LogAuditoriaResponseDto(

        Long id,
        Long usuarioId,
        String usuarioNome,
        String usuarioCpf,
        Long documentoId,
        String documentoDescricao,
        AcaoAuditoriaEnum acao,
        String detalhe,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtOcorrencia
) {
    public static LogAuditoriaResponseDto from(LogAuditoria log) {
        return new LogAuditoriaResponseDto(
                log.getId(),
                log.getUsuario().getId(),
                log.getUsuario().getNome(),
                log.getUsuario().getCpf(),
                log.getDocumentoId(),
                log.getDocumentoDescricao(),
                log.getAcao(),
                log.getDetalhe(),
                log.getDtOcorrencia()
        );
    }
}
