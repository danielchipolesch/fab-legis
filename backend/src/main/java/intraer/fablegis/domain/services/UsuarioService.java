package intraer.fablegis.domain.services;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.dtos.usuarioDtos.RedefinirSenhaRequestDto;
import intraer.fablegis.application.dtos.usuarioDtos.UsuarioCreateRequestDto;
import intraer.fablegis.application.dtos.usuarioDtos.UsuarioResponseDto;
import intraer.fablegis.application.dtos.usuarioDtos.UsuarioUpdateRequestDto;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.regras.DiferencaDeAcesso;
import intraer.fablegis.domain.regras.EscopoDeAcesso;
import intraer.fablegis.domain.regras.RevisaoDeAcesso;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.PostoGraduacao;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.ResourceAlreadyExistsException;
import intraer.fablegis.domain.handlers.exceptions.ResourceCannotBeUpdatedException;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.util.CpfValidator;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.PostoGraduacaoRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import intraer.fablegis.infrastructure.security.AutenticacaoUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

// Tela "Manter Usuários" (admin). Não há exclusão definitiva -- usuários são
// autores de documento (FK sem ON DELETE), então desativar (fl_ativo=false)
// é o único caminho, e já era a intenção original de ter esse campo (ver
// comentário em Usuario.java).
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;

    private final PostoGraduacaoRepository postoGraduacaoRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuditoriaDeAcessoService auditoriaDeAcesso;

    public List<UsuarioResponseDto> listar() {
        var ator = AutenticacaoUtil.usuarioAtual();
        return usuarioRepository.findAll().stream()
                .filter(u -> !u.isSistema())
                .filter(u -> EscopoDeAcesso.enxerga(ator, u))
                .map(u -> UsuarioResponseDto.from(u, ator))
                .toList();
    }

    public UsuarioResponseDto obter(Long id) {
        var ator = AutenticacaoUtil.usuarioAtual();
        return UsuarioResponseDto.from(buscarUsuarioDoEscopo(id, ator), ator);
    }

    @Transactional
    public UsuarioResponseDto criar(UsuarioCreateRequestDto request) {
        String cpf = CpfValidator.onlyDigits(request.cpf());
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new ResourceAlreadyExistsException("Já existe um usuário cadastrado com esse CPF.");
        }

        OrganizacaoMilitar om = buscarOm(request.omId());
        var papeis = papeisComo(request.papeis());
        EscopoDeAcesso.exigirPodeGerenciar(AutenticacaoUtil.usuarioAtual(), null, om.getSigla(), papeis);

        var usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setNomeGuerra(vazioComoNulo(request.nomeGuerra()));
        usuario.setCpf(cpf);
        usuario.setEmail(vazioComoNulo(request.email()));
        usuario.setPostoGraduacao(buscarPostoGraduacaoOpcional(request.postoGraduacaoId()));
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setOm(om);
        usuario.setAtivo(true);
        usuario.setPapeis(papeis);
        // Quem nasce com papel elevado conta como revisado agora: o prazo de 6 meses corre a partir da concessão.
        usuario.setDtRevisaoAcesso(java.sql.Timestamp.from(java.time.Instant.now()));

        var salvo = usuarioRepository.save(usuario);
        auditoriaDeAcesso.registrarCriacao(salvo);
        return UsuarioResponseDto.from(salvo, AutenticacaoUtil.usuarioAtual());
    }

    @Transactional
    public UsuarioResponseDto atualizar(Long id, UsuarioUpdateRequestDto request) {
        Usuario usuarioLogado = AutenticacaoUtil.usuarioAtual();
        Usuario usuario = buscarUsuarioDoEscopo(id, usuarioLogado);

        boolean ehVoceMesmo = usuario.getId().equals(usuarioLogado.getId());
        if (ehVoceMesmo && !request.ativo()) {
            throw new ResourceCannotBeUpdatedException("Você não pode desativar sua própria conta.");
        }
        if (ehVoceMesmo && !request.papeis().contains(PapelEnum.ADMIN)) {
            throw new ResourceCannotBeUpdatedException("Você não pode remover seu próprio papel de administrador.");
        }

        OrganizacaoMilitar om = buscarOm(request.omId());
        var papeisDepois = papeisComo(request.papeis());
        EscopoDeAcesso.exigirPodeGerenciar(usuarioLogado, usuario, om.getSigla(), papeisDepois);
        EscopoDeAcesso.exigirAoMenosUmAdminGeral(
                EscopoDeAcesso.ehAdminGeral(usuario) && usuario.isAtivo(),
                papeisDepois.contains(PapelEnum.ADMIN) && EscopoDeAcesso.ehOmGeral(om.getSigla()) && request.ativo(),
                usuarioRepository.contarAtivosComPapelNaOm(EscopoDeAcesso.OM_GERAL, PapelEnum.ADMIN));

        // O estado de acesso ANTES da mudança, para a auditoria registrar só o que de fato mudou (papéis, situação, OM).
        var antes = new DiferencaDeAcesso.Estado(EnumSet.copyOf(papeisOuVazio(usuario.getPapeis())), usuario.isAtivo(), siglaDa(usuario.getOm()));

        usuario.setNome(request.nome());
        usuario.setNomeGuerra(vazioComoNulo(request.nomeGuerra()));
        usuario.setEmail(vazioComoNulo(request.email()));
        usuario.setPostoGraduacao(buscarPostoGraduacaoOpcional(request.postoGraduacaoId()));
        usuario.setOm(om);
        usuario.setAtivo(request.ativo());
        usuario.setPapeis(papeisDepois);
        atualizarRevisaoDeAcesso(usuario, antes);

        var salvo = usuarioRepository.save(usuario);
        var depois = new DiferencaDeAcesso.Estado(EnumSet.copyOf(papeisOuVazio(salvo.getPapeis())), salvo.isAtivo(), siglaDa(salvo.getOm()));
        auditoriaDeAcesso.registrarMudancas(salvo, DiferencaDeAcesso.entre(antes, depois));
        return UsuarioResponseDto.from(salvo, usuarioLogado);
    }

    @Transactional
    public void redefinirSenha(Long id, RedefinirSenhaRequestDto request) {
        Usuario usuario = buscarUsuarioDoEscopo(id, AutenticacaoUtil.usuarioAtual());
        EscopoDeAcesso.exigirPodeAdministrar(AutenticacaoUtil.usuarioAtual(), usuario);
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
        auditoriaDeAcesso.registrarRedefinicaoDeSenha(usuario);
    }

    // Troca de OM de quem tem papel elevado deixa a revisão PENDENTE (data nula); conceder um papel elevado novo conta como revisão
    // (o prazo corre dali). Ver RevisaoDeAcesso.
    private static void atualizarRevisaoDeAcesso(Usuario usuario, DiferencaDeAcesso.Estado antes) {
        var elevadosDepois = RevisaoDeAcesso.papeisElevados(usuario.getPapeis());
        boolean trocouDeOm = !java.util.Objects.equals(antes.omSigla(), siglaDa(usuario.getOm()));
        if (trocouDeOm && !elevadosDepois.isEmpty()) {
            usuario.setDtRevisaoAcesso(null);
        } else if (!RevisaoDeAcesso.papeisElevados(antes.papeis()).containsAll(elevadosDepois)) {
            usuario.setDtRevisaoAcesso(java.sql.Timestamp.from(java.time.Instant.now()));
        }
    }

    private static java.util.Set<PapelEnum> papeisOuVazio(java.util.Set<PapelEnum> papeis) {
        return papeis == null || papeis.isEmpty() ? EnumSet.noneOf(PapelEnum.class) : papeis;
    }

    private static String siglaDa(OrganizacaoMilitar om) {
        return om != null ? om.getSigla() : null;
    }

    // O usuário real, se quem age o enxerga: o admin local nem sabe que existe um usuário de outra OM (404, como se não houvesse).
    private Usuario buscarUsuarioDoEscopo(Long id, Usuario ator) {
        Usuario usuario = buscarUsuarioReal(id);
        if (!EscopoDeAcesso.enxerga(ator, usuario)) {
            throw new ResourceNotFoundException("Usuário não encontrado.");
        }
        return usuario;
    }

    private Usuario buscarUsuarioReal(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (usuario.isSistema()) {
            throw new ResourceNotFoundException("Usuário não encontrado.");
        }
        return usuario;
    }

    private OrganizacaoMilitar buscarOm(Long omId) {
        return organizacaoMilitarRepository.findById(omId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização militar não encontrada."));
    }

    // Nulo é válido (usuário não-militar, ex.: servidor civil) -- só busca
    // quando um id foi de fato informado.
    private PostoGraduacao buscarPostoGraduacaoOpcional(Long postoGraduacaoId) {
        if (postoGraduacaoId == null) return null;
        return postoGraduacaoRepository.findById(postoGraduacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Posto/graduação não encontrado."));
    }

    private String vazioComoNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    // Editor é papel padrão e permanente de todo usuário (exceto o usuário
    // "sistema", que nem passa por aqui) -- a regra de posse sobre documento
    // continua a mesma (DocumentoAcessoService: só edita quem é autor/coautor),
    // isso só garante que ninguém cria/edita um usuário sem a capacidade básica
    // de editar. Adicionado aqui, não só na tela (UsuariosPage.vue trava o
    // checkbox), pra valer também numa chamada direta à API.
    private EnumSet<PapelEnum> papeisComo(java.util.Set<PapelEnum> papeis) {
        var resultado = EnumSet.noneOf(PapelEnum.class);
        if (papeis != null) resultado.addAll(papeis);
        resultado.add(PapelEnum.EDIT);
        return resultado;
    }
}
