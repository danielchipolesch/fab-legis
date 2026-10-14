package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Adapter entre Usuario (entidade JPA) e o contrato do Spring Security --
// mantém a entidade livre de detalhes de autenticação. Nenhuma authority é
// concedida incondicionalmente: os papéis de PapelEnum (ELB, APR_OM, PUB_BCA, AUD_OM, AUD_GER, ADM_OM, ADM_GER)
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
        // Um authority por papel, pelo código (ROLE_ELB, ROLE_ADM_GER...): o escopo geral/local já está no próprio papel.
        return usuario.getPapeis().stream()
                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + p.codigo()))
                .toList();
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
