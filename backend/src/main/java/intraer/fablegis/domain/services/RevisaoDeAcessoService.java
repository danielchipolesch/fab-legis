package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.usuarioDtos.UsuarioResponseDto;
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
import java.util.EnumSet;
import java.util.Set;

// Revisão periódica de acessos (RevisaoDeAcesso). Quem revisa mantém os perfis elevados de quem lhe cabe ou retira alguns; nunca
// concede (conceder é a tela de usuários). Cada revisão vai para a auditoria de acessos, na mesma transação.
@Service
@RequiredArgsConstructor
public class RevisaoDeAcessoService {

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaDeAcessoService auditoriaDeAcesso;

    @Transactional
    public UsuarioResponseDto revisar(Long usuarioId, Set<PapelEnum> papeisMantidos) {
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
            throw new InvalidInputException("A revisão só mantém ou retira perfis; para conceder um papel use Manter Usuários.");
        }

        var depois = EnumSet.of(PapelEnum.ELB);
        depois.addAll(elevadosMantidos);
        var retirados = EnumSet.noneOf(PapelEnum.class);
        retirados.addAll(elevadosAntes);
        retirados.removeAll(elevadosMantidos);

        EscopoDeAcesso.exigirAoMenosUmAdminGeral(
                EscopoDeAcesso.ehAdminGeral(alvo) && alvo.isAtivo(),
                depois.contains(PapelEnum.ADM_GER) && alvo.isAtivo(),
                usuarioRepository.contarAtivosComPapel(PapelEnum.ADM_GER));
        alvo.setPapeis(depois);
        alvo.setDtRevisaoAcesso(Timestamp.from(Instant.now()));
        var salvo = usuarioRepository.save(alvo);

        var eventos = new ArrayList<DiferencaDeAcesso.Evento>();
        eventos.add(new DiferencaDeAcesso.Evento(AcaoDeAcessoEnum.REVISOU_ACESSO, detalhe(elevadosMantidos, retirados)));
        eventos.addAll(DiferencaDeAcesso.entre(
                new DiferencaDeAcesso.Estado(antes, salvo.isAtivo(), EscopoDeAcesso.siglaDaOm(salvo)),
                new DiferencaDeAcesso.Estado(depois, salvo.isAtivo(), EscopoDeAcesso.siglaDaOm(salvo))));
        auditoriaDeAcesso.registrarMudancas(salvo, eventos);
        return UsuarioResponseDto.from(salvo, revisor);
    }

    private static String detalhe(Set<PapelEnum> mantidos, Set<PapelEnum> retirados) {
        var partes = new ArrayList<String>();
        partes.add("Manteve: " + (mantidos.isEmpty() ? "nenhum papel elevado" : DiferencaDeAcesso.lista(mantidos)));
        if (!retirados.isEmpty()) partes.add("Retirou: " + DiferencaDeAcesso.lista(retirados));
        return String.join(". ", partes);
    }
}
