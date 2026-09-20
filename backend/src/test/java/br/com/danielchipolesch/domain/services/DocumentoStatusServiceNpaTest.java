package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.regras.RegrasDasEspecies;
import br.com.danielchipolesch.domain.regras.TipoDeRegras;
import br.com.danielchipolesch.domain.regras.npa.CamposDeNpa;
import br.com.danielchipolesch.domain.regras.npa.CicloDeVidaDeNpa;
import br.com.danielchipolesch.domain.regras.npa.PublicacaoDeNpa;
import br.com.danielchipolesch.domain.regras.npa.RegrasDeNpa;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemParteFinalRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemPartePreliminarRepository;
import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum.*;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Ciclo de vida de uma NPA (docs/ciclo-de-vida.md): publicação e revogação no Boletim Interno -- sem portaria, BCA nem
// alteração. Mesma máquina de etapas do DocumentoStatusService, com as regras da NPA.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoStatusServiceNpaTest {

    private static final long DOC_ID = 7L;
    private static final long REVISOR_ID = 10L;
    private static final long PUBLICADOR_ID = 20L;

    @Mock DocumentoRepository documentoRepository;
    @Mock DocumentoHistoricoService documentoHistoricoService;
    @Mock DocumentoPdfService documentoPdfService;
    @Mock DocumentoHtmlService documentoHtmlService;
    @Mock ItemAnexoParteNormativaRepository normativaRepository;
    @Mock ItemPartePreliminarRepository preliminarRepository;
    @Mock ItemParteFinalRepository finalRepository;
    @Mock EmendaService emendaService;
    @Mock NotificacaoService notificacaoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PlatformTransactionManager transactionManager;

    @InjectMocks DocumentoStatusService service;

    private CamposDeNpa camposDeNpa;

    @BeforeEach
    void preparar() {
        camposDeNpa = mock(CamposDeNpa.class);
        var npa = new RegrasDeNpa(null, null, camposDeNpa, new PublicacaoDeNpa(camposDeNpa), null, null, null, null, null,
                new CicloDeVidaDeNpa());
        ReflectionTestUtils.setField(service, "regras", new RegrasDasEspecies(List.of(npa)));
        when(documentoPdfService.gerarEArmazenarPdf(any())).thenReturn("pdf-gerado");
        when(documentoHtmlService.gerarEArmazenarHtml(any())).thenReturn("html-gerado");
    }

    private static Usuario usuario(long id) {
        var u = new Usuario();
        u.setId(id);
        u.setNome("Usuário " + id);
        return u;
    }

    private Documento npaEm(SituacaoBcaEnum bca, SituacaoLocalEnum local) {
        var especie = new EspecieNormativa();
        especie.setSigla("NPA");
        especie.setTipoDeRegras(TipoDeRegras.NPA);
        var om = new OrganizacaoMilitar();
        om.setId(1L);
        om.setNome("OM de teste");
        var doc = new Documento();
        doc.setId(DOC_ID);
        doc.setEspecieNormativa(especie);
        doc.setIdentificacao("NPA-AGO-01");
        doc.setSituacaoBca(bca);
        doc.setSituacaoLocal(local);
        doc.setAutor(usuario(1L));
        doc.setOm(om);
        if (bca != NAO_PUBLICADO) {
            doc.setBcaReferencia("Boletim Interno Ostensivo nº 15, de 2 de abril de 2026");
            doc.setDtBcaReferencia(java.sql.Timestamp.valueOf("2026-04-02 00:00:00"));
        }
        when(documentoRepository.findById(DOC_ID)).thenReturn(Optional.of(doc));
        when(usuarioRepository.findById(REVISOR_ID)).thenReturn(Optional.of(usuario(REVISOR_ID)));
        when(usuarioRepository.findById(PUBLICADOR_ID)).thenReturn(Optional.of(usuario(PUBLICADOR_ID)));
        return doc;
    }

    private static DocumentoStatusRequestDto pedido(SituacaoLocalEnum destino, Integer numeroBoletim, LocalDate dataBoletim) {
        return new DocumentoStatusRequestDto(destino, REVISOR_ID, PUBLICADOR_ID, null, null, null, null, null, null,
                null, null, null, null, null, null, numeroBoletim, dataBoletim);
    }

    // ─── Publicação ─────────────────────────────────────────────────────────────

    @Test
    void publicarRegistraOBoletimInternoSemPortariaNemBca() {
        var doc = npaEm(NAO_PUBLICADO, EM_PUBLICACAO);

        service.changeStatus(DOC_ID, pedido(SEM_ETAPA, 15, LocalDate.of(2026, 4, 2)));

        assertThat(doc.getSituacaoBca()).isEqualTo(PUBLICADO);
        assertThat(doc.getSituacaoLocal()).isEqualTo(SEM_ETAPA);
        assertThat(doc.getBcaReferencia()).isEqualTo("Boletim Interno Ostensivo nº 15, de 2 de abril de 2026");
        assertThat(doc.getDtBcaReferencia().toLocalDateTime().toLocalDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(doc.getPortariaReferencia()).isNull();
        assertThat(doc.getDtPublicacao()).isNotNull();
    }

    @Test
    void publicarNaoExigeOsDadosDePortariaNemAParteDeUmAtoNormativo() {
        npaEm(NAO_PUBLICADO, EM_PUBLICACAO);

        // Sem portaria, sem BCA, sem PDF da portaria, sem epígrafe/ementa: só o Boletim Interno.
        service.changeStatus(DOC_ID, pedido(SEM_ETAPA, 1, LocalDate.of(2026, 1, 5)));

        verifyNoInteractions(emendaService);
    }

    @Test
    void publicarSemNumeroOuSemDataDoBoletimEhRecusado() {
        npaEm(NAO_PUBLICADO, EM_PUBLICACAO);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedido(SEM_ETAPA, null, LocalDate.of(2026, 4, 2))))
                .isInstanceOf(StatusCannotBeUpdatedException.class).hasMessageContaining("Boletim Interno");
        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedido(SEM_ETAPA, 15, null)))
                .isInstanceOf(StatusCannotBeUpdatedException.class);
    }

    @Test
    void oNumeroDoBoletimTemLimites() {
        npaEm(NAO_PUBLICADO, EM_PUBLICACAO);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedido(SEM_ETAPA, 0, LocalDate.of(2026, 4, 2))))
                .isInstanceOf(StatusCannotBeUpdatedException.class);
        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedido(SEM_ETAPA, 10_000, LocalDate.of(2026, 4, 2))))
                .isInstanceOf(StatusCannotBeUpdatedException.class);
    }

    // ─── Sem alteração ──────────────────────────────────────────────────────────

    @Test
    void umaNpaPublicadaNaoPodeSerAlterada() {
        npaEm(PUBLICADO, SEM_ETAPA);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedido(EM_ALTERACAO, null, null)))
                .isInstanceOf(StatusCannotBeUpdatedException.class).hasMessageContaining("Transição não permitida");
    }

    @Test
    void devolverDaRevisaoLevaAMinutaMesmoDeUmDocumentoPublicado() {
        var doc = npaEm(NAO_PUBLICADO, EM_REVISAO);

        service.changeStatus(DOC_ID, pedido(MINUTA, null, null));

        assertThat(doc.getSituacaoLocal()).isEqualTo(MINUTA);
        assertThat(doc.getRevisorAtribuido()).isNull();
    }

    // ─── Revogação ──────────────────────────────────────────────────────────────

    @Test
    void revogarRegistraOBoletimDaRevogacaoSemSobrescreverOdaPublicacao() {
        var doc = npaEm(PUBLICADO, EM_REVOGACAO);

        service.changeStatus(DOC_ID, pedido(SEM_ETAPA, 20, LocalDate.of(2026, 5, 3)));

        assertThat(doc.getSituacaoBca()).isEqualTo(REVOGADO);
        assertThat(doc.getBcaReferencia()).isEqualTo("Boletim Interno Ostensivo nº 15, de 2 de abril de 2026");
        verify(camposDeNpa).registrarRevogacao(eq(DOC_ID), eq("Boletim Interno Ostensivo nº 20, de 3 de maio de 2026"));
    }

    @Test
    void oBoletimDaRevogacaoNaoPodeSerAnteriorAoDaPublicacao() {
        npaEm(PUBLICADO, EM_REVOGACAO);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedido(SEM_ETAPA, 20, LocalDate.of(2026, 3, 1))))
                .isInstanceOf(StatusCannotBeUpdatedException.class).hasMessageContaining("anterior");
    }

    @Test
    void oPedidoDeRevogacaoSegueOMesmoCaminhoDeAprovacao() {
        var doc = npaEm(PUBLICADO, SEM_ETAPA);

        service.changeStatus(DOC_ID, pedido(ANALISE_REVOGACAO, null, null));
        assertThat(doc.getSituacaoLocal()).isEqualTo(ANALISE_REVOGACAO);
        assertThat(doc.getSituacaoBca()).isEqualTo(PUBLICADO);

        service.changeStatus(DOC_ID, pedido(EM_REVOGACAO, null, null));
        assertThat(doc.getSituacaoLocal()).isEqualTo(EM_REVOGACAO);
    }
}
