package intraer.fablegis.application.dtos.usuarioDtos;

import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.regras.RevisaoDeAcesso;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

// motivoDaRevisao: por que o acesso precisa de revisão (NENHUM = em dia ou sem perfil elevado) -- calculado na hora, nada é gravado.
// revisavel: quem pergunta é quem revisa o acesso deste usuário (RevisaoDeAcesso.podeRevisar) e há perfil elevado a revisar.
public record UsuarioResponseDto(

        Long id,
        String nome,
        String nomeGuerra,
        String cpf,
        String email,
        Long postoGraduacaoId,
        String postoGraduacaoNome,
        String postoGraduacaoBigrama,
        Long omId,
        String omNome,
        String omSigla,
        boolean ativo,
        List<String> perfis,
        RevisaoDeAcesso.Motivo motivoDaRevisao,
        boolean revisavel,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtRevisaoAcesso,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtUltimoAcesso,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/Sao_Paulo")
        Timestamp dtCriacao
) {
    // Sem saber quem pergunta: nada é "revisável".
    public static UsuarioResponseDto from(Usuario usuario) {
        return from(usuario, null);
    }

    public static UsuarioResponseDto from(Usuario usuario, Usuario quemPergunta) {
        var posto = usuario.getPostoGraduacao();
        boolean revisavel = quemPergunta != null && usuario.isAtivo()
                && RevisaoDeAcesso.temPerfilElevado(usuario) && RevisaoDeAcesso.podeRevisar(quemPergunta, usuario);
        return new UsuarioResponseDto(
                usuario.getId(),
                usuario.getNome(),
                usuario.getNomeGuerra(),
                usuario.getCpf(),
                usuario.getEmail(),
                posto != null ? posto.getId() : null,
                posto != null ? posto.getNome() : null,
                posto != null ? posto.getBigrama() : null,
                usuario.getOm().getId(),
                usuario.getOm().getNome(),
                usuario.getOm().getSigla(),
                usuario.isAtivo(),
                usuario.getPerfis().stream().map(Enum::name).toList(),
                usuario.isAtivo() ? RevisaoDeAcesso.motivo(usuario, Timestamp.from(Instant.now())) : RevisaoDeAcesso.Motivo.NENHUM,
                revisavel,
                usuario.getDtRevisaoAcesso(),
                usuario.getDtUltimoAcesso(),
                usuario.getDtCriacao()
        );
    }
}
