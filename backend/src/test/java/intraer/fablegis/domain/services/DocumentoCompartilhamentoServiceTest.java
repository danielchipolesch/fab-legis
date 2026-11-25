package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.usuarioDtos.CompartilharDocumentoRequestDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.DocumentoCompartilhamento;
import intraer.fablegis.domain.entities.notificacao.TipoNotificacaoEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.RegraDeNegocioException;
import intraer.fablegis.domain.handlers.exceptions.ResourceAlreadyExistsException;
import intraer.fablegis.infrastructure.notificacao.AvisosAoVivo;
import intraer.fablegis.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Autoria do documento (docs/papeis-e-permissoes.md): autores iguais; o documento nunca fica sem autor; atribuir avisa a pessoa.
// Quem PODE atribuir ou desvincular é de DocumentoAcessoService (DocumentoAcessoServiceTest).
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoCompartilhamentoServiceTest {

    private static final long DOC_ID = 5L;
    private static final String CPF = "11144477735";

    @Mock DocumentoRepository documentoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock DocumentoCompartilhamentoRepository compartilhamentoRepository;
    @Mock NotificacaoService notificacaoService;
    @Mock AvisosAoVivo avisosAoVivo;
    @InjectMocks DocumentoCompartilhamentoService service;

    private Documento doc;
    private Usuario novo;
    private Usuario ator;

    private static Usuario usuario(long id, String nome) {
        var u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setAtivo(true);
        return u;
    }

    @BeforeEach
    void cenario() {
        doc = new Documento();
        doc.setId(DOC_ID);
        novo = usuario(2, "Novo Autor");
        ator = usuario(9, "Administrador Local");
        when(documentoRepository.findById(DOC_ID)).thenReturn(Optional.of(doc));
        when(usuarioRepository.findByCpf(CPF)).thenReturn(Optional.of(novo));
        when(compartilhamentoRepository.save(any(DocumentoCompartilhamento.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void atribuirAutoriaAcrescentaAPessoaEAvisaElaEAsTelas() {
        var resultado = service.compartilhar(DOC_ID, new CompartilharDocumentoRequestDto(CPF), ator);

        assertThat(resultado.usuarioId()).isEqualTo(2L);
        verify(notificacaoService).criar(eq(novo), eq(TipoNotificacaoEnum.DOCUMENTO_COMPARTILHADO), any(), eq(DOC_ID), any());
        verify(avisosAoVivo).documentoMudou(doc);
    }

    // O Administrador da OM pode atribuir a autoria a si mesmo: nesse caso não há por que avisar a própria pessoa.
    @Test
    void quemAtribuiAutoriaASiMesmoNaoRecebeNotificacao() {
        when(usuarioRepository.findByCpf(CPF)).thenReturn(Optional.of(ator));

        service.compartilhar(DOC_ID, new CompartilharDocumentoRequestDto(CPF), ator);

        verify(compartilhamentoRepository).save(any(DocumentoCompartilhamento.class));
        verify(notificacaoService, never()).criar(any(), any(), any(), any(), any());
    }

    @Test
    void quemJaEAutorNaoEAcrescentadoDeNovo() {
        when(compartilhamentoRepository.existsByDocumentoIdAndUsuarioId(DOC_ID, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.compartilhar(DOC_ID, new CompartilharDocumentoRequestDto(CPF), ator))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void contaDesativadaNaoPodeSerAutora() {
        novo.setAtivo(false);

        assertThatThrownBy(() -> service.compartilhar(DOC_ID, new CompartilharDocumentoRequestDto(CPF), ator))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(compartilhamentoRepository, never()).save(any(DocumentoCompartilhamento.class));
    }

    @Test
    void desvincularUmDosAutoresFunciona() {
        var autoria = new DocumentoCompartilhamento();
        autoria.setUsuario(novo);
        when(compartilhamentoRepository.findByDocumentoIdAndUsuarioId(DOC_ID, 2L)).thenReturn(Optional.of(autoria));
        when(compartilhamentoRepository.countByDocumentoId(DOC_ID)).thenReturn(2L);

        assertThat(service.desvincular(DOC_ID, 2L)).isEqualTo("Novo Autor");

        verify(compartilhamentoRepository).delete(autoria);
        verify(avisosAoVivo).documentoMudou(doc);
    }

    // O sistema avisa que não dá para se desvincular por ser o único elaborador atribuído -- vale para quem se desvincula e para o administrador.
    @Test
    void oDocumentoNuncaFicaSemAutor() {
        var autoria = new DocumentoCompartilhamento();
        autoria.setUsuario(novo);
        when(compartilhamentoRepository.findByDocumentoIdAndUsuarioId(DOC_ID, 2L)).thenReturn(Optional.of(autoria));
        when(compartilhamentoRepository.countByDocumentoId(DOC_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.desvincular(DOC_ID, 2L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("única pessoa atribuída como elaboradora");
        verify(compartilhamentoRepository, never()).delete(any(DocumentoCompartilhamento.class));
    }
}
