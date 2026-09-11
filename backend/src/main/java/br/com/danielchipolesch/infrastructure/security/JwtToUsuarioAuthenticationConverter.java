package br.com.danielchipolesch.infrastructure.security;

import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

// Faz a ponte entre o Jwt validado pelo resource server (spring-boot-starter-
// oauth2-resource-server) e o restante do sistema, que sempre girou em torno
// de UsuarioPrincipal (DocumentoAcessoService, AutenticacaoUtil, os
// controllers que fazem (UsuarioPrincipal) auth.getPrincipal()). Em vez de
// reescrever cada um desses pontos para entender Jwt, este converter recarrega
// o Usuario do banco pelo claim "sub" (mesmo id que JwtAuthenticationFilter
// usava) e devolve um UsuarioPrincipal -- todo o resto do código continua
// igual. Authorities vêm sempre do estado ATUAL do usuário no banco (não das
// claims do token), igual o filtro antigo já fazia -- uma mudança de papel
// no meio da vida do token já vale na próxima requisição, sem esperar expirar.
@Component
public class JwtToUsuarioAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long usuarioId;
        try {
            usuarioId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("Token com subject inválido.");
        }

        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new InvalidBearerTokenException("Usuário do token não encontrado."));

        var principal = new UsuarioPrincipal(usuario);
        if (!principal.isEnabled()) {
            throw new InvalidBearerTokenException("Usuário inativo.");
        }

        var auth = new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
        auth.setDetails(jwt);
        return auth;
    }
}
