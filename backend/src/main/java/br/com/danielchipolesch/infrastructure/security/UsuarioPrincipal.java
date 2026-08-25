package br.com.danielchipolesch.infrastructure.security;

import br.com.danielchipolesch.domain.entities.usuario.PapelEnum;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Adapter entre Usuario (entidade JPA) e o contrato do Spring Security --
// mantém a entidade livre de detalhes de autenticação. REDATOR não é uma
// authority explícita: é o que todo Usuario autenticado já tem, então vira
// "ROLE_REDATOR" incondicionalmente aqui, nunca lido de t_usuario_papel.
public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.concat(
                Stream.of("ROLE_REDATOR"),
                usuario.getPapeis().stream().map(p -> "ROLE_" + p.name())
        ).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    public List<PapelEnum> getPapeis() {
        return usuario.getPapeis().stream().toList();
    }

    @Override
    public String getPassword() {
        return usuario.getSenhaHash();
    }

    @Override
    public String getUsername() {
        return usuario.getCpf();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isAtivo() && !usuario.isSistema();
    }
}
