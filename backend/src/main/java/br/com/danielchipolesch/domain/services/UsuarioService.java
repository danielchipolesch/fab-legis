package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.usuarioDtos.RedefinirSenhaRequestDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.UsuarioCreateRequestDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.UsuarioResponseDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.UsuarioUpdateRequestDto;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.entities.usuario.PapelEnum;
import br.com.danielchipolesch.domain.entities.usuario.PostoGraduacao;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceAlreadyExistsException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.util.CpfValidator;
import br.com.danielchipolesch.infrastructure.repositories.OrganizacaoMilitarRepository;
import br.com.danielchipolesch.infrastructure.repositories.PostoGraduacaoRepository;
import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
import br.com.danielchipolesch.infrastructure.security.AutenticacaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

// Tela "Manter Usuários" (admin). Não há exclusão definitiva -- usuários são
// autores de documento (FK sem ON DELETE), então desativar (fl_ativo=false)
// é o único caminho, e já era a intenção original de ter esse campo (ver
// comentário em Usuario.java / migração V9).
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private OrganizacaoMilitarRepository organizacaoMilitarRepository;

    @Autowired
    private PostoGraduacaoRepository postoGraduacaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UsuarioResponseDto> listar() {
        return usuarioRepository.findAll().stream()
                .filter(u -> !u.isSistema())
                .map(UsuarioResponseDto::from)
                .toList();
    }

    public UsuarioResponseDto obter(Long id) {
        return UsuarioResponseDto.from(buscarUsuarioReal(id));
    }

    @Transactional
    public UsuarioResponseDto criar(UsuarioCreateRequestDto request) {
        String cpf = CpfValidator.onlyDigits(request.cpf());
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new ResourceAlreadyExistsException("Já existe um usuário cadastrado com esse CPF.");
        }

        OrganizacaoMilitar om = buscarOm(request.omId());

        var usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setNomeGuerra(vazioComoNulo(request.nomeGuerra()));
        usuario.setCpf(cpf);
        usuario.setEmail(vazioComoNulo(request.email()));
        usuario.setPostoGraduacao(buscarPostoGraduacaoOpcional(request.postoGraduacaoId()));
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setOm(om);
        usuario.setAtivo(true);
        usuario.setPapeis(papeisComo(request.papeis()));

        return UsuarioResponseDto.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDto atualizar(Long id, UsuarioUpdateRequestDto request) {
        Usuario usuario = buscarUsuarioReal(id);
        Usuario usuarioLogado = AutenticacaoUtil.usuarioAtual();

        boolean ehVoceMesmo = usuario.getId().equals(usuarioLogado.getId());
        if (ehVoceMesmo && !request.ativo()) {
            throw new ResourceCannotBeUpdatedException("Você não pode desativar sua própria conta.");
        }
        if (ehVoceMesmo && !request.papeis().contains(PapelEnum.ADMIN)) {
            throw new ResourceCannotBeUpdatedException("Você não pode remover seu próprio papel de administrador.");
        }

        OrganizacaoMilitar om = buscarOm(request.omId());

        usuario.setNome(request.nome());
        usuario.setNomeGuerra(vazioComoNulo(request.nomeGuerra()));
        usuario.setEmail(vazioComoNulo(request.email()));
        usuario.setPostoGraduacao(buscarPostoGraduacaoOpcional(request.postoGraduacaoId()));
        usuario.setOm(om);
        usuario.setAtivo(request.ativo());
        usuario.setPapeis(papeisComo(request.papeis()));

        return UsuarioResponseDto.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public void redefinirSenha(Long id, RedefinirSenhaRequestDto request) {
        Usuario usuario = buscarUsuarioReal(id);
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
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

    private EnumSet<PapelEnum> papeisComo(java.util.Set<PapelEnum> papeis) {
        var resultado = EnumSet.noneOf(PapelEnum.class);
        if (papeis != null) resultado.addAll(papeis);
        return resultado;
    }
}
