package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.regras.EscopoDeAcesso;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Adapter entre Usuario (entidade JPA) e o contrato do Spring Security --
// mantém a entidade livre de detalhes de autenticação. Nenhuma authority é
// concedida incondicionalmente: EDIT/APROV/PUBLIC/ADMIN/AUDITOR (ver PapelEnum)
// são todos explícitos, lidos de t_usuario_papel -- sem papel nenhum, a pessoa
// só visualiza o acervo (DocumentoAcessoService não exige authority pra isso).
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
        var authorities = new java.util.ArrayList<GrantedAuthority>();
        usuario.getPapeis().forEach(p -> authorities.add(new SimpleGrantedAuthority("ROLE_" + p.name())));
        // Escopo GERAL (só o CENDOC, ver EscopoDeAcesso): ADMIN_GERAL e AUDITOR_GERAL, para o @PreAuthorize distinguir do local.
        if (EscopoDeAcesso.temEscopoGeral(usuario)) {
            if (usuario.getPapeis().contains(PapelEnum.ADMIN)) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN_GERAL"));
            if (usuario.getPapeis().contains(PapelEnum.AUDITOR)) authorities.add(new SimpleGrantedAuthority("ROLE_AUDITOR_GERAL"));
        }
        return authorities;
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
