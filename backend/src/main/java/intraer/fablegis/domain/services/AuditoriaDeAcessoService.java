package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.auditoriaDtos.LogDeAcessoResponseDto;
import intraer.fablegis.domain.entities.auditoria.AcaoDeAcessoEnum;
import intraer.fablegis.domain.entities.auditoria.LogDeAcesso;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.regras.DiferencaDeAcesso;
import intraer.fablegis.infrastructure.repositories.LogDeAcessoRepository;
import intraer.fablegis.infrastructure.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

// Auditoria dos acessos (docs/autenticacao.md, "Auditoria de acessos"). Diferente da auditoria de documentos -- que engole a
// falha para não derrubar a ação --, esta grava NA TRANSAÇÃO da mudança e deixa a falha subir: conceder um papel sem deixar
// rastro é pior do que não conceder. Quem fez a mudança é o usuário autenticado; sem autenticação (primeiro boot), é o sistema.
@Service
@RequiredArgsConstructor
public class AuditoriaDeAcessoService {

    private final LogDeAcessoRepository repositorio;

    @Transactional
    public void registrarCriacao(Usuario alvo) {
        gravar(alvo, AcaoDeAcessoEnum.CRIOU_USUARIO,
                "Papéis: " + DiferencaDeAcesso.lista(alvo.getPapeis()) + (alvo.isAtivo() ? "" : ". Criado inativo"));
    }

    @Transactional
    public void registrarMudancas(Usuario alvo, List<DiferencaDeAcesso.Evento> eventos) {
        eventos.forEach(e -> gravar(alvo, e.acao(), e.detalhe()));
    }

    @Transactional
    public void registrarRedefinicaoDeSenha(Usuario alvo) {
        gravar(alvo, AcaoDeAcessoEnum.REDEFINIU_SENHA, null);
    }

    private void gravar(Usuario alvo, AcaoDeAcessoEnum acao, String detalhe) {
        var registro = new LogDeAcesso();
        registro.setAtor(atorAtual());
        registro.setAlvoId(alvo.getId());
        registro.setAlvoCpf(alvo.getCpf());
        registro.setAlvoNome(alvo.getNome());
        registro.setOmSigla(alvo.getOm() != null ? alvo.getOm().getSigla() : null);
        registro.setAcao(acao);
        registro.setDetalhe(detalhe);
        repositorio.save(registro);
    }

    private static Usuario atorAtual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UsuarioPrincipal p ? p.getUsuario() : null;
    }

    @Transactional(readOnly = true)
    public Page<LogDeAcessoResponseDto> filtrar(String cpfDoAlvo, AcaoDeAcessoEnum acao, Timestamp dataInicio, Timestamp dataFim,
                                                Pageable pageable) {
        Specification<LogDeAcesso> spec = (root, query, cb) -> null;
        if (cpfDoAlvo != null && !cpfDoAlvo.isBlank()) {
            String digitos = cpfDoAlvo.replaceAll("\\D", "");
            spec = spec.and((root, query, cb) -> cb.equal(root.get("alvoCpf"), digitos));
        }
        if (acao != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("acao"), acao));
        if (dataInicio != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dtOcorrencia"), dataInicio));
        if (dataFim != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dtOcorrencia"), dataFim));
        return repositorio.findAll(spec, pageable).map(LogDeAcessoResponseDto::from);
    }
}
