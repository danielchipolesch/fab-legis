package intraer.fablegis.application.dtos.auditoriaDtos;

import intraer.fablegis.domain.entities.auditoria.AcaoDeAcessoEnum;
import intraer.fablegis.domain.entities.auditoria.LogDeAcesso;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;

public record LogDeAcessoResponseDto(

        Long id,
        Long atorId,
        String atorNome,
        String atorCpf,
        Long alvoId,
        String alvoNome,
        String alvoCpf,
        String omSigla,
        AcaoDeAcessoEnum acao,
        String detalhe,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtOcorrencia
) {
    public static LogDeAcessoResponseDto from(LogDeAcesso log) {
        var ator = log.getAtor();
        return new LogDeAcessoResponseDto(
                log.getId(),
                ator != null ? ator.getId() : null,
                ator != null ? ator.getNome() : null,
                ator != null ? ator.getCpf() : null,
                log.getAlvoId(), log.getAlvoNome(), log.getAlvoCpf(), log.getOmSigla(),
                log.getAcao(), log.getDetalhe(), log.getDtOcorrencia());
    }
}
