package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.usuarioDtos.UsuarioCreateRequestDto;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.ResourceAlreadyExistsException;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.PostoGraduacaoRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// O CPF é o login e o identificador do usuário: é único (docs/autenticacao.md), com ou sem máscara na digitação.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock OrganizacaoMilitarRepository organizacaoMilitarRepository;
    @Mock PostoGraduacaoRepository postoGraduacaoRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditoriaDeAcessoService auditoriaDeAcesso;
    @InjectMocks UsuarioService service;

    private static UsuarioCreateRequestDto pedido(String cpf) {
        return new UsuarioCreateRequestDto("Fulano", null, cpf, null, null, "senha-forte-1", 1L, null);
    }

    @Test
    void cpfJaCadastradoEhRecusadoMesmoDigitadoComMascara() {
        when(usuarioRepository.existsByCpf("11144477735")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(pedido("111.444.777-35")))
                .isInstanceOf(ResourceAlreadyExistsException.class).hasMessageContaining("CPF");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cpfNovoEhGravadoSoComOsDigitos() {
        when(usuarioRepository.existsByCpf("11144477735")).thenReturn(false);
        when(organizacaoMilitarRepository.findById(1L)).thenReturn(Optional.of(new OrganizacaoMilitar()));
        when(passwordEncoder.encode("senha-forte-1")).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.criar(pedido("111.444.777-35"));

        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getCpf()).isEqualTo("11144477735");
    }

    // ─── Auditoria dos acessos ───────────────────────────────────────────────

    private Usuario existente(long id, String sigla, PapelEnum... papeis) {
        var om = new OrganizacaoMilitar();
        om.setId(1L);
        om.setSigla(sigla);
        var u = new Usuario();
        u.setId(id);
        u.setNome("Fulano");
        u.setCpf("11144477735");
        u.setOm(om);
        u.setAtivo(true);
        u.setPapeis(java.util.EnumSet.copyOf(java.util.List.of(papeis)));
        return u;
    }

    private void autenticarComo(Usuario ator) {
        var principal = new intraer.fablegis.infrastructure.security.UsuarioPrincipal(ator);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @org.junit.jupiter.api.AfterEach
    void limparContexto() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void criarUsuarioRegistraACriacaoNaAuditoria() {
        when(usuarioRepository.existsByCpf("11144477735")).thenReturn(false);
        when(organizacaoMilitarRepository.findById(1L)).thenReturn(Optional.of(new OrganizacaoMilitar()));
        when(passwordEncoder.encode("senha-forte-1")).thenReturn("hash");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.criar(pedido("111.444.777-35"));

        verify(auditoriaDeAcesso).registrarCriacao(any(Usuario.class));
    }

    @Test
    void concederPapelPeloCadastroVaiParaAAuditoriaComOQueMudou() {
        var admin = existente(1L, "CENDOC", PapelEnum.ADMIN, PapelEnum.EDIT);
        autenticarComo(admin);
        var alvo = existente(2L, "BAGL", PapelEnum.EDIT);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alvo));
        var om = new OrganizacaoMilitar();
        om.setSigla("BAGL");
        when(organizacaoMilitarRepository.findById(1L)).thenReturn(Optional.of(om));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(2L, new intraer.fablegis.application.dtos.usuarioDtos.UsuarioUpdateRequestDto(
                "Fulano", null, null, null, 1L, true, java.util.Set.of(PapelEnum.EDIT, PapelEnum.APROV)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<intraer.fablegis.domain.regras.DiferencaDeAcesso.Evento>> eventos = ArgumentCaptor.forClass(java.util.List.class);
        verify(auditoriaDeAcesso).registrarMudancas(any(Usuario.class), eventos.capture());
        assertThat(eventos.getValue()).hasSize(1);
        assertThat(eventos.getValue().get(0).acao()).isEqualTo(intraer.fablegis.domain.entities.auditoria.AcaoDeAcessoEnum.ALTEROU_PAPEIS);
        assertThat(eventos.getValue().get(0).detalhe()).contains("Concedido(s): APROV");
    }

    @Test
    void editarSemMudarOAcessoNaoGeraRegistro() {
        autenticarComo(existente(1L, "CENDOC", PapelEnum.ADMIN, PapelEnum.EDIT));
        var alvo = existente(2L, "BAGL", PapelEnum.EDIT);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alvo));
        var om = new OrganizacaoMilitar();
        om.setSigla("BAGL");
        when(organizacaoMilitarRepository.findById(1L)).thenReturn(Optional.of(om));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(2L, new intraer.fablegis.application.dtos.usuarioDtos.UsuarioUpdateRequestDto(
                "Fulano da Silva", null, null, null, 1L, true, java.util.Set.of(PapelEnum.EDIT)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<intraer.fablegis.domain.regras.DiferencaDeAcesso.Evento>> eventos = ArgumentCaptor.forClass(java.util.List.class);
        verify(auditoriaDeAcesso).registrarMudancas(any(Usuario.class), eventos.capture());
        assertThat(eventos.getValue()).isEmpty();
    }

    @Test
    void redefinirSenhaVaiParaAAuditoria() {
        var alvo = existente(2L, "BAGL", PapelEnum.EDIT);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alvo));
        when(passwordEncoder.encode("nova-senha-1")).thenReturn("hash");

        service.redefinirSenha(2L, new intraer.fablegis.application.dtos.usuarioDtos.RedefinirSenhaRequestDto("nova-senha-1"));

        verify(auditoriaDeAcesso).registrarRedefinicaoDeSenha(alvo);
    }
}
