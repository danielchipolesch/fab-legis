package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.entities.usuario.PapelEnum;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.EnumSet;
import java.util.Optional;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Quem pode pedir cada mudança de etapa (docs/autenticacao.md). Se a transição em si é válida é
// coisa do DocumentoStatusService; aqui é só posse/atribuição.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoAcessoServiceTest {

    private static final long DOC_ID = 5L;
    private static final long OM_ID = 1L;

    @Mock DocumentoRepository documentoRepository;
    @Mock DocumentoCompartilhamentoRepository compartilhamentoRepository;
    @InjectMocks DocumentoAcessoService service;

    private Usuario autor;
    private Usuario aprovadorDaOm;
    private Usuario aprovadorDeOutraOm;
    private Usuario revisor;
    private Usuario publicador;
    private Usuario coautor;
    private Usuario estranho;
    private Documento doc;

    private static Usuario usuario(long id, long omId, PapelEnum papel) {
        var om = new OrganizacaoMilitar();
        om.setId(omId);
        var u = new Usuario();
        u.setId(id);
        u.setOm(om);
        u.setPapeis(EnumSet.of(papel));
        return u;
    }

    private static Authentication como(Usuario u) {
        return new UsernamePasswordAuthenticationToken(new UsuarioPrincipal(u), null);
    }

    @BeforeEach
    void cenario() {
        autor = usuario(1, OM_ID, PapelEnum.EDIT);
        coautor = usuario(2, OM_ID, PapelEnum.EDIT);
        aprovadorDaOm = usuario(3, OM_ID, PapelEnum.APROV);
        aprovadorDeOutraOm = usuario(4, 99, PapelEnum.APROV);
        revisor = usuario(5, OM_ID, PapelEnum.APROV);
        publicador = usuario(6, OM_ID, PapelEnum.PUBLIC);
        estranho = usuario(7, OM_ID, PapelEnum.EDIT);

        var om = new OrganizacaoMilitar();
        om.setId(OM_ID);
        doc = new Documento();
        doc.setId(DOC_ID);
        doc.setOm(om);
        doc.setAutor(autor);
        doc.setSituacaoBca(SituacaoBcaEnum.NAO_PUBLICADO);
        when(documentoRepository.findById(DOC_ID)).thenReturn(Optional.of(doc));
        when(compartilhamentoRepository.existsByDocumentoIdAndUsuarioId(DOC_ID, coautor.getId())).thenReturn(true);
    }

    private boolean pode(Usuario u, SituacaoLocalEnum destino) {
        return service.podeMudarStatus(DOC_ID, destino, como(u));
    }

    @Test
    void documentoInexistenteNaoPodeMudar() {
        when(documentoRepository.findById(DOC_ID)).thenReturn(Optional.empty());
        assertThat(pode(autor, MINUTA)).isFalse();
    }

    // ─── Enviar para revisão / pedir análise de revogação: quem edita ────────────

    @ParameterizedTest
    @EnumSource(value = SituacaoLocalEnum.class, names = {"EM_REVISAO", "ANALISE_REVOGACAO"})
    void autorECoautorComPapelEditPedemRevisaoOuAnaliseDeRevogacao(SituacaoLocalEnum destino) {
        doc.setSituacaoLocal(destino == EM_REVISAO ? MINUTA : SEM_ETAPA);
        assertThat(pode(autor, destino)).isTrue();
        assertThat(pode(coautor, destino)).isTrue();
        assertThat(pode(estranho, destino)).isFalse();
    }

    @Test
    void semOPapelEditNemOAutorEnviaParaRevisao() {
        doc.setSituacaoLocal(MINUTA);
        autor.setPapeis(EnumSet.of(PapelEnum.APROV));
        assertThat(pode(autor, EM_REVISAO)).isFalse();
    }

    // ─── Iniciar e cancelar uma alteração ────────────────────────────────────────

    @Test
    void soAprovadorDaMesmaOmIniciaUmaAlteracao() {
        doc.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        doc.setSituacaoLocal(SEM_ETAPA);

        assertThat(pode(aprovadorDaOm, EM_ALTERACAO)).isTrue();
        assertThat(pode(aprovadorDeOutraOm, EM_ALTERACAO)).isFalse();
        // Nem o autor (papel EDIT): quem reabre um ato publicado é um Aprovador.
        assertThat(pode(autor, EM_ALTERACAO)).isFalse();
        assertThat(pode(publicador, EM_ALTERACAO)).isFalse();
    }

    @Test
    void aprovadorDaOmOuQuemEditaPodeCancelarAAlteracao() {
        doc.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        doc.setSituacaoLocal(EM_ALTERACAO);

        assertThat(pode(aprovadorDaOm, SEM_ETAPA)).isTrue();
        assertThat(pode(autor, SEM_ETAPA)).isTrue();
        assertThat(pode(coautor, SEM_ETAPA)).isTrue();
        assertThat(pode(aprovadorDeOutraOm, SEM_ETAPA)).isFalse();
        assertThat(pode(estranho, SEM_ETAPA)).isFalse();
    }

    // ─── Revisão: só o revisor atribuído decide ──────────────────────────────────

    @ParameterizedTest
    @EnumSource(value = SituacaoLocalEnum.class, names = {"EM_PUBLICACAO", "MINUTA"})
    void soORevisorAtribuidoAprovaOuDevolveDeEmRevisao(SituacaoLocalEnum destino) {
        doc.setSituacaoLocal(EM_REVISAO);
        doc.setRevisorAtribuido(revisor);

        assertThat(pode(revisor, destino)).isTrue();
        assertThat(pode(aprovadorDaOm, destino)).isFalse();
        assertThat(pode(publicador, destino)).isFalse();
    }

    @Test
    void semRevisorAtribuidoNinguemDecideARevisao() {
        doc.setSituacaoLocal(EM_REVISAO);
        doc.setRevisorAtribuido(null);
        assertThat(pode(aprovadorDaOm, EM_PUBLICACAO)).isFalse();
    }

    @Test
    void revisorNaoCancelaODocumento() {
        doc.setSituacaoLocal(EM_REVISAO);
        doc.setRevisorAtribuido(revisor);
        assertThat(pode(revisor, CANCELADO)).isFalse();
    }

    @Test
    void soORevisorAtribuidoAprovaOuDevolveAAnaliseDeRevogacao() {
        doc.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        doc.setSituacaoLocal(ANALISE_REVOGACAO);
        doc.setRevisorAtribuido(revisor);

        assertThat(pode(revisor, EM_REVOGACAO)).isTrue();
        assertThat(pode(revisor, SEM_ETAPA)).isTrue();
        assertThat(pode(aprovadorDaOm, EM_REVOGACAO)).isFalse();
        assertThat(pode(aprovadorDaOm, SEM_ETAPA)).isFalse();
    }

    // ─── Publicação e revogação: só o publicador atribuído decide ────────────────

    @ParameterizedTest
    @EnumSource(value = SituacaoLocalEnum.class, names = {"SEM_ETAPA", "MINUTA", "EM_ALTERACAO"})
    void soOPublicadorAtribuidoPublicaOuDevolveDeEmPublicacao(SituacaoLocalEnum destino) {
        doc.setSituacaoLocal(EM_PUBLICACAO);
        doc.setPublicadorAtribuido(publicador);

        assertThat(pode(publicador, destino)).isTrue();
        assertThat(pode(revisor, destino)).isFalse();
        assertThat(pode(autor, destino)).isFalse();
    }

    @Test
    void soOPublicadorAtribuidoFormalizaARevogacao() {
        doc.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        doc.setSituacaoLocal(EM_REVOGACAO);
        doc.setPublicadorAtribuido(publicador);

        assertThat(pode(publicador, SEM_ETAPA)).isTrue();
        assertThat(pode(revisor, SEM_ETAPA)).isFalse();
    }

    @Test
    void publicadorNaoAtribuidoNaoPublica() {
        doc.setSituacaoLocal(EM_PUBLICACAO);
        doc.setPublicadorAtribuido(usuario(60, OM_ID, PapelEnum.PUBLIC));
        assertThat(pode(publicador, SEM_ETAPA)).isFalse();
    }

    // ─── Demais transições: posse de editar ──────────────────────────────────────

    @Test
    void cancelarUmRascunhoOuMinutaEDeQuemEdita() {
        doc.setSituacaoLocal(MINUTA);
        assertThat(pode(autor, CANCELADO)).isTrue();
        assertThat(pode(coautor, CANCELADO)).isTrue();
        assertThat(pode(estranho, CANCELADO)).isFalse();
    }

    @Test
    void minutarEDeQuemEdita() {
        doc.setSituacaoLocal(RASCUNHO);
        assertThat(pode(autor, MINUTA)).isTrue();
        assertThat(pode(estranho, MINUTA)).isFalse();
    }
}
