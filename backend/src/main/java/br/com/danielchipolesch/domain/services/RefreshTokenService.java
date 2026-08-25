package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.usuario.RefreshToken;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.CredenciaisInvalidasException;
import br.com.danielchipolesch.infrastructure.repositories.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HexFormat;

// Token opaco de vida longa, trocável por um novo par access+refresh via
// POST /v1/auth/refresh (ver AuthService). Guardamos só o hash SHA-256 --
// nunca o valor em si -- e cada uso ROTACIONA o token: o antigo é marcado
// revogado e um novo é emitido, então um valor vazado só serve uma vez
// antes de ficar inválido dos dois lados.
@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private static final String MSG_INVALIDO = "Sessão expirada. Entre novamente.";

    @Transactional
    public String gerar(Usuario usuario) {
        String valor = gerarValorAleatorio();

        var entity = new RefreshToken();
        entity.setUsuario(usuario);
        entity.setTokenHash(hash(valor));
        entity.setDtExpiracao(new Timestamp(System.currentTimeMillis() + refreshExpirationMs));
        refreshTokenRepository.save(entity);

        return valor;
    }

    @Transactional
    public Usuario validarERotacionar(String valor) {
        var entity = refreshTokenRepository.findByTokenHash(hash(valor))
                .orElseThrow(() -> new CredenciaisInvalidasException(MSG_INVALIDO));

        if (!entity.isValido()) {
            throw new CredenciaisInvalidasException(MSG_INVALIDO);
        }

        entity.setDtRevogacao(new Timestamp(System.currentTimeMillis()));
        refreshTokenRepository.save(entity);

        return entity.getUsuario();
    }

    @Transactional
    public void revogar(String valor) {
        refreshTokenRepository.findByTokenHash(hash(valor))
                .filter(RefreshToken::isValido)
                .ifPresent(entity -> {
                    entity.setDtRevogacao(new Timestamp(System.currentTimeMillis()));
                    refreshTokenRepository.save(entity);
                });
    }

    private String gerarValorAleatorio() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }
}
