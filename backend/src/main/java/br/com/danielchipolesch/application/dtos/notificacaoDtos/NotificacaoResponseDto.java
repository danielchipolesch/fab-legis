package br.com.danielchipolesch.application.dtos.notificacaoDtos;

import br.com.danielchipolesch.domain.entities.notificacao.Notificacao;
import br.com.danielchipolesch.domain.entities.notificacao.TipoNotificacaoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;

public record NotificacaoResponseDto(

        Long id,
        TipoNotificacaoEnum tipo,
        String mensagem,
        Long documentoId,
        String documentoDescricao,
        boolean lida,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtCriacao
) {
    public static NotificacaoResponseDto from(Notificacao n) {
        return new NotificacaoResponseDto(
                n.getId(), n.getTipo(), n.getMensagem(),
                n.getDocumentoId(), n.getDocumentoDescricao(),
                n.isLida(), n.getDtCriacao()
        );
    }
}
