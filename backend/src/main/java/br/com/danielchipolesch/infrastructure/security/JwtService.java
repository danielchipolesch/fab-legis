package br.com.danielchipolesch.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

// Claims nomeadas como o Keycloak já nomeia as suas (sub, preferred_username,
// realm_access.roles) de propósito -- ver o design doc de autenticação. Só
// om_id é específica deste sistema; no Keycloak ela vira um protocol mapper
// configurado no realm, não código. Trocar o emissor do token (fase de
// migração para o Keycloak) não deve exigir tocar nesta classe nem em
// DocumentoAcessoService, só o filtro que valida a assinatura.
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String gerarToken(UsuarioPrincipal principal) {
        var usuario = principal.getUsuario();
        List<String> roles = principal.getPapeis().stream().map(Enum::name).toList();

        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("preferred_username", usuario.getCpf())
                .claim("nome", usuario.getNome())
                .claim("om_id", String.valueOf(usuario.getOm().getId()))
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(key)
                .compact();
    }

    public Claims validarEExtrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
