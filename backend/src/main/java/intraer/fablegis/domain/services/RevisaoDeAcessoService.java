package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.usuarioDtos.RevisaoDeAcessoResponseDto;
import intraer.fablegis.domain.entities.auditoria.AcaoDeAcessoEnum;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.regras.DiferencaDeAcesso;
import intraer.fablegis.domain.regras.EscopoDeAcesso;
import intraer.fablegis.domain.regras.RevisaoDeAcesso;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import intraer.fablegis.infrastructure.security.AutenticacaoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// Revisão periódica de acessos (RevisaoDeAcesso). Quem revisa mantém os papéis elevados de quem lhe cabe ou retira alguns; nunca
// concede (conceder é a tela de usuários). Cada revisão vai para a auditoria de acessos, na mesma transação.
@Service
@RequiredArgsConstructor
public class RevisaoDeAcessoService {

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaDeAcessoService auditoriaDeAcesso;

    // Os usuários ativos, com papel elevado, que quem pergunta revisa -- pendentes primeiro.
    @Transactional(readOnly = true)
    public List<RevisaoDeAcessoResponseDto> listar() {
        var revisor = AutenticacaoUtil.usuarioAtual();
        var agora = Timestamp.from(Instant.now());
        return usuarioRepository.findAll().stream()
                .filter(u -> !u.isSistema() && u.isAtivo() && RevisaoDeAcesso.temPapelElevado(u) && RevisaoDeAcesso.podeRevisar(revisor, u))
                .map(u -> RevisaoDeAcessoResponseDto.from(u, agora))
                .sorted(Comparator.comparing(RevisaoDeAcessoResponseDto::pendente).reversed()
                        .thenComparing(RevisaoDeAcessoResponseDto::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public RevisaoDeAcessoResponseDto revisar(Long usuarioId, Set<PapelEnum> papeisMantidos) {
        var revisor = AutenticacaoUtil.usuarioAtual();
        Usuario alvo = usuarioRepository.findById(usuarioId)
                .filter(u -> !u.isSistema())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (!RevisaoDeAcesso.podeRevisar(revisor, alvo)) {
            throw new OperacaoNaoAutorizadaException("Você não revisa o acesso desta pessoa.");
        }

        var antes = EnumSet.noneOf(PapelEnum.class);
        antes.addAll(alvo.getPapeis());
        var elevadosAntes = RevisaoDeAcesso.papeisElevados(antes);
        var elevadosMantidos = RevisaoDeAcesso.papeisElevados(papeisMantidos);
        if (!elevadosAntes.containsAll(elevadosMantidos)) {
            throw new InvalidInputException("A revisão só mantém ou retira papéis; para conceder um papel use Manter Usuários.");
        }

        var depois = EnumSet.of(PapelEnum.EDIT);
        depois.addAll(elevadosMantidos);
        var retirados = EnumSet.noneOf(PapelEnum.class);
        retirados.addAll(elevadosAntes);
        retirados.removeAll(elevadosMantidos);

        alvo.setPapeis(depois);
        alvo.setDtRevisaoAcesso(Timestamp.from(Instant.now()));
        var salvo = usuarioRepository.save(alvo);

        var eventos = new ArrayList<DiferencaDeAcesso.Evento>();
        eventos.add(new DiferencaDeAcesso.Evento(AcaoDeAcessoEnum.REVISOU_ACESSO, detalhe(elevadosMantidos, retirados)));
        eventos.addAll(DiferencaDeAcesso.entre(
                new DiferencaDeAcesso.Estado(antes, salvo.isAtivo(), EscopoDeAcesso.siglaDaOm(salvo)),
                new DiferencaDeAcesso.Estado(depois, salvo.isAtivo(), EscopoDeAcesso.siglaDaOm(salvo))));
        auditoriaDeAcesso.registrarMudancas(salvo, eventos);
        return RevisaoDeAcessoResponseDto.from(salvo, Timestamp.from(Instant.now()));
    }

    private static String detalhe(Set<PapelEnum> mantidos, Set<PapelEnum> retirados) {
        var partes = new ArrayList<String>();
        partes.add("Manteve: " + (mantidos.isEmpty() ? "nenhum papel elevado" : DiferencaDeAcesso.lista(mantidos)));
        if (!retirados.isEmpty()) partes.add("Retirou: " + DiferencaDeAcesso.lista(retirados));
        return String.join(". ", partes);
    }
}
