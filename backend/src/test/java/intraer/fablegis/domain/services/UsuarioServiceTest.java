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

    // Por padrão, quem age é o administrador geral (ADMIN no CENDOC); os testes do escopo local trocam o ator.
    @org.junit.jupiter.api.BeforeEach
    void autenticarOAdministradorGeral() {
        autenticarComo(existente(1L, "CENDOC", PapelEnum.ADM_GER, PapelEnum.ELB));
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
        var admin = existente(1L, "CENDOC", PapelEnum.ADM_GER, PapelEnum.ELB);
        autenticarComo(admin);
        var alvo = existente(2L, "BAGL", PapelEnum.ELB);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alvo));
        var om = new OrganizacaoMilitar();
        om.setSigla("BAGL");
        when(organizacaoMilitarRepository.findById(1L)).thenReturn(Optional.of(om));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(2L, new intraer.fablegis.application.dtos.usuarioDtos.UsuarioUpdateRequestDto(
                "Fulano", null, null, null, 1L, true, java.util.Set.of(PapelEnum.ELB, PapelEnum.APR_OM)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<intraer.fablegis.domain.regras.DiferencaDeAcesso.Evento>> eventos = ArgumentCaptor.forClass(java.util.List.class);
        verify(auditoriaDeAcesso).registrarMudancas(any(Usuario.class), eventos.capture());
        assertThat(eventos.getValue()).hasSize(1);
        assertThat(eventos.getValue().get(0).acao()).isEqualTo(intraer.fablegis.domain.entities.auditoria.AcaoDeAcessoEnum.ALTEROU_PAPEIS);
        assertThat(eventos.getValue().get(0).detalhe()).contains("Concedido(s): APR_OM");
    }

    @Test
    void editarSemMudarOAcessoNaoGeraRegistro() {
        autenticarComo(existente(1L, "CENDOC", PapelEnum.ADM_GER, PapelEnum.ELB));
        var alvo = existente(2L, "BAGL", PapelEnum.ELB);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alvo));
        var om = new OrganizacaoMilitar();
        om.setSigla("BAGL");
        when(organizacaoMilitarRepository.findById(1L)).thenReturn(Optional.of(om));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(2L, new intraer.fablegis.application.dtos.usuarioDtos.UsuarioUpdateRequestDto(
                "Fulano da Silva", null, null, null, 1L, true, java.util.Set.of(PapelEnum.ELB)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<intraer.fablegis.domain.regras.DiferencaDeAcesso.Evento>> eventos = ArgumentCaptor.forClass(java.util.List.class);
        verify(auditoriaDeAcesso).registrarMudancas(any(Usuario.class), eventos.capture());
        assertThat(eventos.getValue()).isEmpty();
    }

    @Test
    void redefinirSenhaVaiParaAAuditoria() {
        var alvo = existente(2L, "BAGL", PapelEnum.ELB);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(alvo));
        when(passwordEncoder.encode("nova-senha-1")).thenReturn("hash");

        service.redefinirSenha(2L, new intraer.fablegis.application.dtos.usuarioDtos.RedefinirSenhaRequestDto("nova-senha-1"));

        verify(auditoriaDeAcesso).registrarRedefinicaoDeSenha(alvo);
    }

    // ─── Escopo do administrador local ───────────────────────────────────────

    @Test
    void oAdminLocalSoVeOsUsuariosDaPropriaOm() {
        autenticarComo(existente(1L, "BAGL", PapelEnum.ADM_OM, PapelEnum.ELB));
        when(usuarioRepository.findAll()).thenReturn(java.util.List.of(
                existente(2L, "BAGL", PapelEnum.ELB), existente(3L, "BABE", PapelEnum.ELB), existente(4L, "CENDOC", PapelEnum.ELB)));

        assertThat(service.listar()).extracting(r -> r.id()).containsExactly(2L);
    }

    @Test
    void oAdminGeralVeTodosOsUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(java.util.List.of(
                existente(2L, "BAGL", PapelEnum.ELB), existente(3L, "BABE", PapelEnum.ELB), existente(4L, "CENDOC", PapelEnum.ELB)));

        assertThat(service.listar()).hasSize(3);
    }

    @Test
    void oAdminLocalNaoConsultaUsuarioDeOutraOm() {
        autenticarComo(existente(1L, "BAGL", PapelEnum.ADM_OM, PapelEnum.ELB));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(existente(3L, "BABE", PapelEnum.ELB)));

        assertThatThrownBy(() -> service.obter(3L)).isInstanceOf(intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException.class);
    }

    @Test
    void oAdminLocalNaoCriaUsuarioEmOutraOmNemComPapelDeAdmin() {
        autenticarComo(existente(1L, "BAGL", PapelEnum.ADM_OM, PapelEnum.ELB));
        when(usuarioRepository.existsByCpf("11144477735")).thenReturn(false);
        var outra = new OrganizacaoMilitar();
        outra.setSigla("BABE");
        when(organizacaoMilitarRepository.findById(2L)).thenReturn(Optional.of(outra));

        assertThatThrownBy(() -> service.criar(new UsuarioCreateRequestDto("Fulano", null, "11144477735", null, null, "senha-forte-1", 2L, null)))
                .isInstanceOf(intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException.class);
        verify(usuarioRepository, never()).save(any());

        var propria = new OrganizacaoMilitar();
        propria.setSigla("BAGL");
        when(organizacaoMilitarRepository.findById(1L)).thenReturn(Optional.of(propria));
        assertThatThrownBy(() -> service.criar(new UsuarioCreateRequestDto("Fulano", null, "11144477735", null, null, "senha-forte-1", 1L,
                java.util.Set.of(PapelEnum.ADM_OM))))
                .isInstanceOf(intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void oAdminLocalNaoRedefineASenhaDeOutroAdmin() {
        autenticarComo(existente(1L, "BAGL", PapelEnum.ADM_OM, PapelEnum.ELB));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(existente(2L, "BAGL", PapelEnum.ADM_OM, PapelEnum.ELB)));

        assertThatThrownBy(() -> service.redefinirSenha(2L, new intraer.fablegis.application.dtos.usuarioDtos.RedefinirSenhaRequestDto("nova-senha-1")))
                .isInstanceOf(intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException.class);
        verify(passwordEncoder, never()).encode(any());
    }

    // ─── Revisão periódica: o que atualiza a data da revisão ─────────────────

    private void prepararEdicao(Usuario alvo, String siglaDeDestino) {
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        var om = new OrganizacaoMilitar();
        om.setSigla(siglaDeDestino);
        when(organizacaoMilitarRepository.findById(1L)).thenReturn(Optional.of(om));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static intraer.fablegis.application.dtos.usuarioDtos.UsuarioUpdateRequestDto edicao(PapelEnum... papeis) {
        return new intraer.fablegis.application.dtos.usuarioDtos.UsuarioUpdateRequestDto(
                "Fulano", null, null, null, 1L, true, java.util.Set.of(papeis));
    }

    @Test
    void trocarDeOmDeQuemTemPapelElevadoDeixaARevisaoPendente() {
        var alvo = existente(2L, "BAGL", PapelEnum.ELB, PapelEnum.APR_OM);
        alvo.setDtRevisaoAcesso(java.sql.Timestamp.from(java.time.Instant.now()));
        prepararEdicao(alvo, "BABE");

        service.atualizar(2L, edicao(PapelEnum.ELB, PapelEnum.APR_OM));

        assertThat(alvo.getDtRevisaoAcesso()).isNull();
    }

    @Test
    void trocarDeOmDeQuemSoEEditorNaoGeraPendencia() {
        var alvo = existente(2L, "BAGL", PapelEnum.ELB);
        var revisao = java.sql.Timestamp.valueOf("2026-01-01 00:00:00");
        alvo.setDtRevisaoAcesso(revisao);
        prepararEdicao(alvo, "BABE");

        service.atualizar(2L, edicao(PapelEnum.ELB));

        assertThat(alvo.getDtRevisaoAcesso()).isEqualTo(revisao);
    }

    @Test
    void concederUmPapelElevadoNovoReiniciaOPrazoDaRevisao() {
        var alvo = existente(2L, "BAGL", PapelEnum.ELB);
        alvo.setDtRevisaoAcesso(java.sql.Timestamp.valueOf("2020-01-01 00:00:00"));
        prepararEdicao(alvo, "BAGL");

        service.atualizar(2L, edicao(PapelEnum.ELB, PapelEnum.APR_OM));

        assertThat(alvo.getDtRevisaoAcesso()).isAfter(java.sql.Timestamp.valueOf("2026-01-01 00:00:00"));
    }

    @Test
    void editarSemMudarOAcessoNaoMexeNaDataDaRevisao() {
        var alvo = existente(2L, "BAGL", PapelEnum.ELB, PapelEnum.APR_OM);
        var revisao = java.sql.Timestamp.valueOf("2026-03-01 00:00:00");
        alvo.setDtRevisaoAcesso(revisao);
        prepararEdicao(alvo, "BAGL");

        service.atualizar(2L, edicao(PapelEnum.ELB, PapelEnum.APR_OM));

        assertThat(alvo.getDtRevisaoAcesso()).isEqualTo(revisao);
    }

    // ─── O CENDOC nunca fica sem administrador geral ─────────────────────────

    @Test
    void naoSeRetiraOPapelDeAdminDoUltimoAdminGeral() {
        var alvo = existente(2L, "CENDOC", PapelEnum.ELB, PapelEnum.ADM_GER);
        prepararEdicao(alvo, "CENDOC");
        when(usuarioRepository.contarAtivosComPapel(PapelEnum.ADM_GER)).thenReturn(1L);

        assertThatThrownBy(() -> service.atualizar(2L, edicao(PapelEnum.ELB)))
                .isInstanceOf(intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException.class)
                .hasMessageContaining("Administrador Geral");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoSeMudaDeOmOUltimoAdminGeral() {
        var alvo = existente(2L, "CENDOC", PapelEnum.ELB, PapelEnum.ADM_GER);
        prepararEdicao(alvo, "BAGL");
        when(usuarioRepository.contarAtivosComPapel(PapelEnum.ADM_GER)).thenReturn(1L);

        assertThatThrownBy(() -> service.atualizar(2L, edicao(PapelEnum.ELB, PapelEnum.ADM_OM)))
                .isInstanceOf(intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException.class);
    }

    @Test
    void comOutroAdminGeralAtivoPodeSerRetirado() {
        var alvo = existente(2L, "CENDOC", PapelEnum.ELB, PapelEnum.ADM_GER);
        prepararEdicao(alvo, "CENDOC");
        when(usuarioRepository.contarAtivosComPapel(PapelEnum.ADM_GER)).thenReturn(2L);

        service.atualizar(2L, edicao(PapelEnum.ELB));

        assertThat(alvo.getPapeis()).doesNotContain(PapelEnum.ADM_OM);
    }
}
