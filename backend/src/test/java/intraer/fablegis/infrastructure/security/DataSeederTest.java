package intraer.fablegis.infrastructure.security;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.PostoGraduacao;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.PostoGraduacaoRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataSeederTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final OrganizacaoMilitarRepository oms = mock(OrganizacaoMilitarRepository.class);
    private final PostoGraduacaoRepository postos = mock(PostoGraduacaoRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final intraer.fablegis.domain.services.AuditoriaDeAcessoService auditoria = mock(intraer.fablegis.domain.services.AuditoriaDeAcessoService.class);
    private DataSeeder seeder;
    private OrganizacaoMilitar cendoc;

    @BeforeEach
    void preparar() {
        seeder = new DataSeeder(usuarios, oms, postos, encoder, auditoria);
        ReflectionTestUtils.setField(seeder, "adminCpf", "11144477735");
        ReflectionTestUtils.setField(seeder, "adminSenha", "Admin@123");
        ReflectionTestUtils.setField(seeder, "adminNome", "DANIEL ANGELO CHIPOLESCH DE ALMEIDA");
        ReflectionTestUtils.setField(seeder, "adminNomeGuerra", "CHIPOLESCH");
        ReflectionTestUtils.setField(seeder, "adminEmail", "chipoleschdaca@fab.mil.br");
        ReflectionTestUtils.setField(seeder, "adminPosto", "CP");
        cendoc = new OrganizacaoMilitar();
        cendoc.setSigla("CENDOC");
        PostoGraduacao cp = new PostoGraduacao();
        cp.setBigrama("CP");
        when(usuarios.findAll()).thenReturn(List.of());
        when(oms.findBySigla("CENDOC")).thenReturn(Optional.of(cendoc));
        when(postos.findAll()).thenReturn(List.of(cp));
        when(encoder.encode(any())).thenReturn("hash");
    }

    private Usuario executar() {
        seeder.run(null);
        ArgumentCaptor<Usuario> c = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(c.capture());
        return c.getValue();
    }

    @Test
    void usuarioPadraoNasceNoCendocComPostoNomeCompletoNomeDeGuerraEEmail() {
        Usuario u = executar();
        assertSame(cendoc, u.getOm());
        assertEquals("CP", u.getPostoGraduacao().getBigrama());
        assertEquals("DANIEL ANGELO CHIPOLESCH DE ALMEIDA", u.getNome());
        assertEquals("CHIPOLESCH", u.getNomeGuerra());
        assertEquals("chipoleschdaca@fab.mil.br", u.getEmail());
        assertEquals("11144477735", u.getCpf());
        assertTrue(u.getPapeis().contains(PapelEnum.ADM_GER));
        assertTrue(u.getPapeis().contains(PapelEnum.GES_GER));
        assertTrue(u.getPapeis().contains(PapelEnum.ELB));
    }

    @Test
    void emailForaDoDominioInstitucionalEhIgnorado() {
        ReflectionTestUtils.setField(seeder, "adminEmail", "alguem@gmail.com");
        assertNull(executar().getEmail());
    }

    @Test
    void naoFazNadaSeJaExisteUsuarioComLogin() {
        Usuario real = new Usuario();
        real.setSistema(false);
        when(usuarios.findAll()).thenReturn(List.of(real));
        seeder.run(null);
        verify(usuarios, never()).save(any());
    }

    @Test
    void semCendocNaoHaFallbackParaOutraOm() {
        when(oms.findBySigla("CENDOC")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> seeder.run(null));
        verify(oms, never()).findBySigla("SISTEMA");
    }
}
