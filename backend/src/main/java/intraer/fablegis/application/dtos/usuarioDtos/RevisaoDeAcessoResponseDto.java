package intraer.fablegis.application.dtos.usuarioDtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.regras.EscopoDeAcesso;
import intraer.fablegis.domain.regras.RevisaoDeAcesso;

import java.sql.Timestamp;
import java.util.List;

// Um usuário com papel elevado que quem revisa pode manter ou reduzir, com o motivo da pendência (NENHUM = em dia).
public record RevisaoDeAcessoResponseDto(
        Long usuarioId,
        String nome,
        String nomeGuerra,
        String postoGraduacaoBigrama,
        String cpf,
        String omSigla,
        List<String> papeisElevados,
        RevisaoDeAcesso.Motivo motivo,
        boolean pendente,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtRevisaoAcesso,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtUltimoAcesso
) {
    public static RevisaoDeAcessoResponseDto from(Usuario u, Timestamp agora) {
        var motivo = RevisaoDeAcesso.motivo(u, agora);
        return new RevisaoDeAcessoResponseDto(
                u.getId(), u.getNome(), u.getNomeGuerra(),
                u.getPostoGraduacao() != null ? u.getPostoGraduacao().getBigrama() : null,
                u.getCpf(), EscopoDeAcesso.siglaDaOm(u),
                RevisaoDeAcesso.papeisElevados(u.getPapeis()).stream().map(Enum::name).sorted().toList(),
                motivo, motivo != RevisaoDeAcesso.Motivo.NENHUM,
                u.getDtRevisaoAcesso(), u.getDtUltimoAcesso());
    }
}
