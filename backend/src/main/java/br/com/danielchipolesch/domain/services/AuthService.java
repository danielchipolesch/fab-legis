package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.authDtos.LoginRequestDto;
import br.com.danielchipolesch.application.dtos.authDtos.LoginResponseDto;
import br.com.danielchipolesch.application.dtos.authDtos.RefreshRequestDto;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.CredenciaisInvalidasException;
import br.com.danielchipolesch.domain.util.CpfValidator;
import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
import br.com.danielchipolesch.infrastructure.security.JwtService;
import br.com.danielchipolesch.infrastructure.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    // Mensagem sempre igual para CPF inexistente e senha errada -- não dar
    // pista de qual dos dois está incorreto.
    private static final String MSG_INVALIDA = "CPF ou senha inválidos.";

    public LoginResponseDto login(LoginRequestDto request) {
        String cpf = CpfValidator.onlyDigits(request.cpf());

        var usuario = usuarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new CredenciaisInvalidasException(MSG_INVALIDA));

        var principal = new UsuarioPrincipal(usuario);
        if (!principal.isEnabled() || !passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException(MSG_INVALIDA);
        }

        return montarResposta(usuario, principal);
    }

    // Troca um refresh token válido por um novo par access+refresh --
    // rotacionado a cada uso (ver RefreshTokenService.validarERotacionar),
    // então um valor vazado só é reaproveitável uma vez.
    public LoginResponseDto refresh(RefreshRequestDto request) {
        Usuario usuario = refreshTokenService.validarERotacionar(request.refreshToken());

        var principal = new UsuarioPrincipal(usuario);
        if (!principal.isEnabled()) {
            throw new CredenciaisInvalidasException("Sessão expirada. Entre novamente.");
        }

        return montarResposta(usuario, principal);
    }

    public void logout(RefreshRequestDto request) {
        refreshTokenService.revogar(request.refreshToken());
    }

    private LoginResponseDto montarResposta(Usuario usuario, UsuarioPrincipal principal) {
        String token = jwtService.gerarToken(principal);
        String refreshToken = refreshTokenService.gerar(usuario);
        var papeis = Stream.concat(Stream.of("REDATOR"), principal.getPapeis().stream().map(Enum::name)).toList();

        return new LoginResponseDto(
                token,
                jwtService.getExpirationMs(),
                refreshToken,
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getOm().getId(),
                usuario.getOm().getNome(),
                usuario.getNomeGuerra(),
                usuario.getPostoGraduacao() != null ? usuario.getPostoGraduacao().getBigrama() : null,
                papeis
        );
    }
}
