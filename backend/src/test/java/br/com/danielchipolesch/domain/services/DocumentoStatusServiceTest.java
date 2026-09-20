package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoPortariaPublicacaoEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemParteFinalRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemPartePreliminarRepository;
import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.danielchipolesch.domain.regras.RegrasDasEspecies;
import br.com.danielchipolesch.domain.regras.atonormativo.RegrasDeAtoNormativo;
import br.com.danielchipolesch.domain.regras.atonormativo.CicloDeVidaDeAtoNormativo;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.mockito.quality.Strictness;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum.NAO_PUBLICADO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum.PUBLICADO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum.REVOGADO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Máquina de etapas do documento (docs/ciclo-de-vida.md), sem Spring nem banco: repositórios e
// serviços colaboradores são mocks. As tabelas de transição esperadas aqui vêm do ciclo de vida
// documentado, não do código -- se as duas divergirem, o teste falha.
//
// Modelo: a Situação BCA é a real (espelha o repositório oficial) e só muda ao registrar portaria +
// BCA; a Situação Local é a etapa interna e nunca tira um documento publicado de PUBLICADO.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoStatusServiceTest {

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
    @Mock DocumentoParteNormativaService documentoParteNormativaService;
    @Mock PortariaPublicacaoService portariaPublicacaoService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PlatformTransactionManager transactionManager;

    @InjectMocks DocumentoStatusService service;

    @BeforeEach
    void arquivosGerados() {
        // O ciclo de vida é regra da espécie: aqui, o dos atos normativos (as espécies de teste não
        // declaram tipo de regras, e o padrão é ATO_NORMATIVO).
        ReflectionTestUtils.setField(service, "regras", new RegrasDasEspecies(List.of(
                new RegrasDeAtoNormativo(null, null, null, null, null, null, null, null, new CicloDeVidaDeAtoNormativo()))));
        when(documentoPdfService.gerarEArmazenarPdf(any())).thenReturn("pdf-gerado");
        when(documentoHtmlService.gerarEArmazenarHtml(any())).thenReturn("html-gerado");
    }

    // ─── Fixtures ────────────────────────────────────────────────────────────────

    private static Usuario usuario(long id, String nome) {
        var u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        return u;
    }

    private Documento documentoEm(SituacaoBcaEnum bca, SituacaoLocalEnum local) {
        var especie = new EspecieNormativa();
        especie.setSigla("ICA");
        var assunto = new AssuntoBasico();
        assunto.setCodigo("5");
        assunto.setNome("Publicações");
        var om = new OrganizacaoMilitar();
        om.setId(1L);
        om.setNome("OM de teste");

        var doc = new Documento();
        doc.setId(DOC_ID);
        doc.setEspecieNormativa(especie);
        doc.setAssuntoBasico(assunto);
        doc.setNumeroSecundario(3);
        doc.setIdentificacao("ICA 5-3");
        doc.setSituacaoBca(bca);
        doc.setSituacaoLocal(local);
        doc.setAutor(usuario(1L, "Autor"));
        doc.setOm(om);
        if (bca != NAO_PUBLICADO) doc.setDtPublicacao(Timestamp.valueOf("2026-01-10 00:00:00"));
        // Versão vigente já gravada de uma publicação anterior.
        if (bca != NAO_PUBLICADO) {
            doc.setUrlPdf("pdf-vigente-anterior");
            doc.setUrlHtml("html-vigente-anterior");
        }

        when(documentoRepository.findById(DOC_ID)).thenReturn(Optional.of(doc));
        when(usuarioRepository.findById(REVISOR_ID)).thenReturn(Optional.of(usuario(REVISOR_ID, "Revisor")));
        when(usuarioRepository.findById(PUBLICADOR_ID)).thenReturn(Optional.of(usuario(PUBLICADOR_ID, "Publicador")));
        return doc;
    }

    // Pedido com TODOS os campos que qualquer transição possa exigir preenchidos -- assim uma
    // transição válida nunca falha por dado faltando, e uma inválida só pode falhar pela regra
    // de transição.
    private static DocumentoStatusRequestDto pedidoCompleto(SituacaoLocalEnum destino) {
        return new DocumentoStatusRequestDto(destino, REVISOR_ID, PUBLICADOR_ID,
                "COMAER", "DIRAP", "123", LocalDate.of(2026, 3, 5), 12, LocalDate.of(2026, 3, 6),
                "{}", "{}", "{}", "{}", "{}", "https://minio/portaria.pdf");
    }

    private static DocumentoStatusRequestDto apenas(SituacaoLocalEnum destino) {
        return new DocumentoStatusRequestDto(destino, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    // Portaria + BCA + PDF, sem a parte preliminar -- é tudo que uma ALTERAÇÃO ou uma REVOGAÇÃO
    // exigem (a parte preliminar é só da primeira publicação).
    private static DocumentoStatusRequestDto somentePortariaEBca(SituacaoLocalEnum destino) {
        return new DocumentoStatusRequestDto(destino, null, null,
                "COMAER", "DIRAP", "123", LocalDate.of(2026, 3, 5), 12, LocalDate.of(2026, 3, 6),
                null, null, null, null, null, "https://minio/portaria.pdf");
    }

    private static DocumentoStatusRequestDto publicacaoCom(String orgao, String setor, String numero, LocalDate dataPortaria,
                                                            Integer numeroBca, LocalDate dataBca, String epigrafe, String pdf) {
        return new DocumentoStatusRequestDto(SEM_ETAPA, null, null, orgao, setor, numero, dataPortaria, numeroBca, dataBca,
                epigrafe, "{}", "{}", "{}", "{}", pdf);
    }

    private static DocumentoStatusRequestDto publicacaoValida() {
        return publicacaoCom("COMAER", "DIRAP", "123", LocalDate.of(2026, 3, 5), 12, LocalDate.of(2026, 3, 6), "{}",
                "https://minio/portaria.pdf");
    }

    // ─── Transições permitidas (docs/ciclo-de-vida.md) ────────────────────────────

    private record Par(SituacaoBcaEnum bca, SituacaoLocalEnum local) {
        @Override public String toString() { return bca + "+" + local; }
    }

    // Combinações que existem de fato (as demais são impossíveis: ex. um PUBLICADO em RASCUNHO).
    private static final Par[] COMBINACOES_ALCANCAVEIS = {
            new Par(NAO_PUBLICADO, RASCUNHO), new Par(NAO_PUBLICADO, MINUTA), new Par(NAO_PUBLICADO, EM_REVISAO),
            new Par(NAO_PUBLICADO, EM_PUBLICACAO), new Par(NAO_PUBLICADO, CANCELADO),
            new Par(PUBLICADO, SEM_ETAPA), new Par(PUBLICADO, EM_ALTERACAO), new Par(PUBLICADO, EM_REVISAO),
            new Par(PUBLICADO, EM_PUBLICACAO), new Par(PUBLICADO, ANALISE_REVOGACAO), new Par(PUBLICADO, EM_REVOGACAO),
            new Par(REVOGADO, SEM_ETAPA),
    };

    private static boolean transicaoPermitida(Par de, SituacaoLocalEnum para) {
        boolean publicado = de.bca() == PUBLICADO;
        return switch (de.local()) {
            case RASCUNHO          -> para == MINUTA || para == CANCELADO;
            case MINUTA            -> para == EM_REVISAO || para == CANCELADO;
            // Aprovar (EM_PUBLICACAO) ou devolver (publicado volta à alteração; não publicado, à minuta).
            case EM_REVISAO, EM_PUBLICACAO -> (de.local() == EM_REVISAO ? para == EM_PUBLICACAO : para == SEM_ETAPA)
                    || para == (publicado ? EM_ALTERACAO : MINUTA);
            // Enviar para revisão ou cancelar a alteração.
            case EM_ALTERACAO      -> para == EM_REVISAO || para == SEM_ETAPA;
            // Aprovar a revogação ou devolver a análise (o documento segue PUBLICADO).
            case ANALISE_REVOGACAO -> para == EM_REVOGACAO || para == SEM_ETAPA;
            // A única saída é formalizar a revogação: nunca volta a PUBLICADO.
            case EM_REVOGACAO      -> para == SEM_ETAPA;
            case SEM_ETAPA         -> publicado && (para == EM_ALTERACAO || para == ANALISE_REVOGACAO);
            case CANCELADO         -> false;
        };
    }

    static Stream<Arguments> todasAsCombinacoes() {
        var lista = Stream.<Arguments>builder();
        for (var de : COMBINACOES_ALCANCAVEIS) {
            for (var para : SituacaoLocalEnum.values()) {
                lista.add(Arguments.of(de, para));
            }
        }
        return lista.build();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("todasAsCombinacoes")
    void transicoesSeguemOCicloDeVida(Par de, SituacaoLocalEnum para) {
        var doc = documentoEm(de.bca(), de.local());

        if (transicaoPermitida(de, para)) {
            assertThatCode(() -> service.changeStatus(DOC_ID, pedidoCompleto(para))).doesNotThrowAnyException();
            assertThat(doc.getSituacaoLocal()).isEqualTo(para);
        } else {
            assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedidoCompleto(para)))
                    .isInstanceOf(StatusCannotBeUpdatedException.class)
                    .hasMessageContaining("Transição não permitida");
            assertThat(doc.getSituacaoLocal()).isEqualTo(de.local());
            assertThat(doc.getSituacaoBca()).isEqualTo(de.bca());
        }
    }

    // A regra central: a situação BCA só muda ao registrar portaria + BCA. Nenhuma etapa
    // interna -- nem devolver, nem cancelar, nem aprovar -- tira um documento publicado de PUBLICADO.
    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("todasAsCombinacoes")
    void situacaoBcaSoMudaAoRegistrarPortaria(Par de, SituacaoLocalEnum para) {
        if (!transicaoPermitida(de, para)) return;
        var doc = documentoEm(de.bca(), de.local());

        service.changeStatus(DOC_ID, pedidoCompleto(para));

        boolean publicando = de.local() == EM_PUBLICACAO && para == SEM_ETAPA;
        boolean revogando = de.local() == EM_REVOGACAO && para == SEM_ETAPA;
        SituacaoBcaEnum esperada = publicando ? PUBLICADO : revogando ? REVOGADO : de.bca();
        assertThat(doc.getSituacaoBca()).isEqualTo(esperada);
    }

    @Test
    void documentoInexistenteNaoTemSituacaoAlterada() {
        when(documentoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(999L, pedidoCompleto(MINUTA)))
                .isInstanceOf(br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException.class);
    }

    // ─── O documento publicado continua publicado durante as etapas internas ─────

    @Test
    void devolverUmaAnaliseDeRevogacaoMantemODocumentoPublicadoESemExigirNada() {
        var doc = documentoEm(PUBLICADO, ANALISE_REVOGACAO);
        var dataPublicacao = doc.getDtPublicacao();
        doc.setRevisorAtribuido(usuario(REVISOR_ID, "Revisor"));

        assertThatCode(() -> service.changeStatus(DOC_ID, apenas(SEM_ETAPA))).doesNotThrowAnyException();

        assertThat(doc.getSituacaoBca()).isEqualTo(PUBLICADO);
        assertThat(doc.getSituacaoLocal()).isEqualTo(SEM_ETAPA);
        assertThat(doc.getRevisorAtribuido()).isNull();
        assertThat(doc.getDtPublicacao()).isEqualTo(dataPublicacao);
        verify(portariaPublicacaoService, never()).registrar(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(documentoParteNormativaService, never()).salvarItensPreliminares(any(), any());
        verify(documentoPdfService, never()).gerarEArmazenarPdf(any());
    }

    @Test
    void iniciarUmaAlteracaoNaoTiraODocumentoDePublicado() {
        var doc = documentoEm(PUBLICADO, SEM_ETAPA);

        service.changeStatus(DOC_ID, apenas(EM_ALTERACAO));

        assertThat(doc.getSituacaoBca()).isEqualTo(PUBLICADO);
        assertThat(doc.getSituacaoLocal()).isEqualTo(EM_ALTERACAO);
        assertThat(doc.getDtEmAlteracao()).isNotNull();
    }

    @Test
    void naoSeIniciaRevogacaoComUmaAlteracaoEmCurso() {
        documentoEm(PUBLICADO, EM_ALTERACAO);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedidoCompleto(ANALISE_REVOGACAO)))
                .isInstanceOf(StatusCannotBeUpdatedException.class)
                .hasMessageContaining("Transição não permitida");
    }

    @Test
    void emRevogacaoNuncaVoltaAPublicadoSoSaiParaRevogado() {
        var doc = documentoEm(PUBLICADO, EM_REVOGACAO);

        // Nenhum destino além de SEM_ETAPA (a revogação sendo formalizada) é aceito.
        for (var destino : SituacaoLocalEnum.values()) {
            if (destino == SEM_ETAPA) continue;
            assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedidoCompleto(destino)))
                    .isInstanceOf(StatusCannotBeUpdatedException.class);
        }
        assertThat(doc.getSituacaoLocal()).isEqualTo(EM_REVOGACAO);
        assertThat(doc.getSituacaoBca()).isEqualTo(PUBLICADO);
    }

    @Test
    void formalizarARevogacaoTornaODocumentoRevogado() {
        var doc = documentoEm(PUBLICADO, EM_REVOGACAO);
        doc.setPublicadorAtribuido(usuario(PUBLICADOR_ID, "Publicador"));

        service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA));

        assertThat(doc.getSituacaoBca()).isEqualTo(REVOGADO);
        assertThat(doc.getSituacaoLocal()).isEqualTo(SEM_ETAPA);
        assertThat(doc.getDtRevogacao()).isNotNull();
        assertThat(doc.getPortariaReferencia()).isEqualTo("Portaria COMAER/DIRAP n° 123, de 5 de março de 2026");
        verifyTipoPortaria(TipoPortariaPublicacaoEnum.REVOGACAO);
    }

    // ─── Cancelar a alteração ────────────────────────────────────────────────────
    @Test
    void cancelarAAlteracaoSemAlteracoesPendentesMantemODocumentoPublicado() {
        var doc = documentoEm(PUBLICADO, EM_ALTERACAO);
        doc.setUrlPdfTramitacao("pdf-em-tramitacao");
        when(emendaService.temAlteracoesPendentes(DOC_ID)).thenReturn(false);

        service.changeStatus(DOC_ID, apenas(SEM_ETAPA));

        assertThat(doc.getSituacaoBca()).isEqualTo(PUBLICADO);
        assertThat(doc.getSituacaoLocal()).isEqualTo(SEM_ETAPA);
        assertThat(doc.getUrlPdf()).isEqualTo("pdf-vigente-anterior");
        assertThat(doc.getUrlPdfTramitacao()).isNull();
    }

    // Nada é descartado em massa: cada alteração pendente se desfaz elemento a elemento (sidebar).
    @Test
    void naoSeCancelaUmaAlteracaoComAlteracoesPendentes() {
        var doc = documentoEm(PUBLICADO, EM_ALTERACAO);
        when(emendaService.temAlteracoesPendentes(DOC_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, apenas(SEM_ETAPA)))
                .isInstanceOf(StatusCannotBeUpdatedException.class)
                .hasMessageContaining("alterações pendentes");

        assertThat(doc.getSituacaoLocal()).isEqualTo(EM_ALTERACAO);
        verify(documentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void outrasTransicoesNaoConsultamAsAlteracoesPendentes() {
        documentoEm(PUBLICADO, EM_ALTERACAO);
        service.changeStatus(DOC_ID, pedidoCompleto(EM_REVISAO));

        verify(emendaService, never()).temAlteracoesPendentes(anyLong());
    }

    // ─── Datas ───────────────────────────────────────────────────────────────────

    @Test
    void cadaEtapaGravaSuaPropriaData() {
        var doc = documentoEm(NAO_PUBLICADO, RASCUNHO);
        service.changeStatus(DOC_ID, apenas(MINUTA));
        assertThat(doc.getDtMinuta()).isNotNull();

        service.changeStatus(DOC_ID, pedidoCompleto(EM_REVISAO));
        assertThat(doc.getDtEmRevisao()).isNotNull();

        service.changeStatus(DOC_ID, pedidoCompleto(EM_PUBLICACAO));
        assertThat(doc.getDtAprovacao()).isNotNull();
        assertThat(doc.getDtEmPublicacao()).isNotNull();
        assertThat(doc.getDtAlterado()).isNull();

        service.changeStatus(DOC_ID, publicacaoValida());
        assertThat(doc.getDtPublicacao()).isNotNull();
    }

    @Test
    void aAprovacaoDeUmaAlteracaoUsaDtAlteradoSemSobrescreverAAprovacaoOriginal() {
        var doc = documentoEm(PUBLICADO, EM_REVISAO);

        service.changeStatus(DOC_ID, pedidoCompleto(EM_PUBLICACAO));

        assertThat(doc.getDtAlterado()).isNotNull();
        assertThat(doc.getDtAprovacao()).isNull();
    }

    @Test
    void dtPublicacaoEADaPrimeiraPublicacaoENaoMudaComAsAlteracoes() {
        var doc = documentoEm(PUBLICADO, EM_PUBLICACAO);
        var primeira = doc.getDtPublicacao();

        service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA));

        assertThat(doc.getDtPublicacao()).isEqualTo(primeira);
    }

    @Test
    void cancelarODocumentoGravaADataDeCancelamento() {
        var doc = documentoEm(NAO_PUBLICADO, MINUTA);

        service.changeStatus(DOC_ID, apenas(CANCELADO));

        assertThat(doc.getDtCancelamento()).isNotNull();
        assertThat(doc.getSituacaoBca()).isEqualTo(NAO_PUBLICADO);
    }

    // ─── Atribuição pessoal (revisor / publicador) ───────────────────────────────

    @Test
    void enviarParaRevisaoExigeEscolherORevisor() {
        documentoEm(NAO_PUBLICADO, MINUTA);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, apenas(EM_REVISAO)))
                .isInstanceOf(StatusCannotBeUpdatedException.class)
                .hasMessage("É obrigatório escolher quem vai revisar.");
    }

    @Test
    void enviarParaRevisaoAtribuiONotificaSoAPessoaEscolhida() {
        var doc = documentoEm(NAO_PUBLICADO, MINUTA);

        service.changeStatus(DOC_ID, pedidoCompleto(EM_REVISAO));

        assertThat(doc.getRevisorAtribuido().getId()).isEqualTo(REVISOR_ID);
        verify(notificacaoService).notificarAtribuicao(eq(REVISOR_ID), eq(DOC_ID), eq("ICA 5-3"), contains("revisão"));
        verify(notificacaoService, times(1)).notificarAtribuicao(anyLong(), anyLong(), any(), any());
    }

    @Test
    void pedirAnaliseDeRevogacaoExigeEAtribuiORevisor() {
        var doc = documentoEm(PUBLICADO, SEM_ETAPA);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, apenas(ANALISE_REVOGACAO)))
                .hasMessage("É obrigatório escolher quem vai revisar.");

        service.changeStatus(DOC_ID, pedidoCompleto(ANALISE_REVOGACAO));
        assertThat(doc.getRevisorAtribuido().getId()).isEqualTo(REVISOR_ID);
        assertThat(doc.getSituacaoBca()).isEqualTo(PUBLICADO);
    }

    @Test
    void revisorInexistenteEhRejeitado() {
        documentoEm(NAO_PUBLICADO, MINUTA);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        var pedido = new DocumentoStatusRequestDto(EM_REVISAO, 99L, null, null, null, null, null, null, null,
                null, null, null, null, null, null);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedido))
                .isInstanceOf(StatusCannotBeUpdatedException.class)
                .hasMessage("Usuário escolhido não encontrado.");
    }

    @Test
    void aprovarExigeEscolherOPublicador() {
        documentoEm(NAO_PUBLICADO, EM_REVISAO);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, apenas(EM_PUBLICACAO)))
                .hasMessage("É obrigatório escolher quem vai publicar.");
    }

    @Test
    void aprovarVaiDiretoParaEmPublicacaoENotificaOPublicador() {
        var doc = documentoEm(NAO_PUBLICADO, EM_REVISAO);

        service.changeStatus(DOC_ID, pedidoCompleto(EM_PUBLICACAO));

        assertThat(doc.getSituacaoLocal()).isEqualTo(EM_PUBLICACAO);
        assertThat(doc.getPublicadorAtribuido().getId()).isEqualTo(PUBLICADOR_ID);
        verify(documentoHistoricoService).registrar(doc, TipoAlteracaoEnum.ALTERACAO_STATUS,
                "EM_REVISAO → EM_PUBLICACAO", EM_REVISAO, EM_PUBLICACAO);
        verify(notificacaoService).notificarAtribuicao(eq(PUBLICADOR_ID), eq(DOC_ID), eq("ICA 5-3"), contains("publicação"));
    }

    @Test
    void aprovarARevogacaoExigeEscolherOPublicador() {
        var doc = documentoEm(PUBLICADO, ANALISE_REVOGACAO);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, apenas(EM_REVOGACAO)))
                .hasMessage("É obrigatório escolher quem vai publicar a revogação.");

        service.changeStatus(DOC_ID, pedidoCompleto(EM_REVOGACAO));
        assertThat(doc.getPublicadorAtribuido().getId()).isEqualTo(PUBLICADOR_ID);
        assertThat(doc.getDtEmRevogacao()).isNotNull();
        verify(notificacaoService).notificarAtribuicao(eq(PUBLICADOR_ID), eq(DOC_ID), eq("ICA 5-3"), contains("revogação"));
    }

    static Stream<Arguments> devolucoes() {
        return Stream.of(
                Arguments.of(NAO_PUBLICADO, EM_REVISAO, MINUTA),
                Arguments.of(NAO_PUBLICADO, EM_PUBLICACAO, MINUTA),
                Arguments.of(PUBLICADO, EM_REVISAO, EM_ALTERACAO),
                Arguments.of(PUBLICADO, EM_PUBLICACAO, EM_ALTERACAO),
                Arguments.of(PUBLICADO, ANALISE_REVOGACAO, SEM_ETAPA));
    }

    @ParameterizedTest(name = "devolver {0}+{1} → {2} limpa a atribuição")
    @MethodSource("devolucoes")
    void devolverLimpaRevisorEPublicador(SituacaoBcaEnum bca, SituacaoLocalEnum de, SituacaoLocalEnum para) {
        var doc = documentoEm(bca, de);
        doc.setRevisorAtribuido(usuario(REVISOR_ID, "Revisor"));
        doc.setPublicadorAtribuido(usuario(PUBLICADOR_ID, "Publicador"));

        service.changeStatus(DOC_ID, apenas(para));

        assertThat(doc.getSituacaoLocal()).isEqualTo(para);
        assertThat(doc.getRevisorAtribuido()).isNull();
        assertThat(doc.getPublicadorAtribuido()).isNull();
    }

    // ─── Publicar: primeira vez x alteração, e a parte preliminar ────────────────

    @Test
    void publicarPelaPrimeiraVezTornaODocumentoPublicado() {
        var doc = documentoEm(NAO_PUBLICADO, EM_PUBLICACAO);

        service.changeStatus(DOC_ID, publicacaoValida());

        assertThat(doc.getSituacaoBca()).isEqualTo(PUBLICADO);
        assertThat(doc.getSituacaoLocal()).isEqualTo(SEM_ETAPA);
        assertThat(doc.getPortariaReferencia()).isEqualTo("Portaria COMAER/DIRAP n° 123, de 5 de março de 2026");
        assertThat(doc.getBcaReferencia()).isEqualTo("BCA n° 12, de 6 de março de 2026");
        verify(documentoHistoricoService).registrar(doc, TipoAlteracaoEnum.ALTERACAO_STATUS,
                "EM_PUBLICACAO → SEM_ETAPA (situação BCA: NAO_PUBLICADO → PUBLICADO)", EM_PUBLICACAO, SEM_ETAPA);
    }

    static Stream<DocumentoStatusRequestDto> publicacoesSemDadosDaPortariaOuDoBca() {
        var d = LocalDate.of(2026, 3, 5);
        return Stream.of(
                publicacaoCom(null, "DIRAP", "123", d, 12, d, "{}", "u"),
                publicacaoCom("COMAER", " ", "123", d, 12, d, "{}", "u"),
                publicacaoCom("COMAER", "DIRAP", null, d, 12, d, "{}", "u"),
                publicacaoCom("COMAER", "DIRAP", "123", null, 12, d, "{}", "u"),
                publicacaoCom("COMAER", "DIRAP", "123", d, null, d, "{}", "u"),
                publicacaoCom("COMAER", "DIRAP", "123", d, 12, null, "{}", "u"),
                publicacaoCom("COMAER", "DIRAP", "123", d, 12, d, "{}", null),
                publicacaoCom("COMAER", "DIRAP", "123", d, 12, d, "{}", ""));
    }

    @ParameterizedTest
    @MethodSource("publicacoesSemDadosDaPortariaOuDoBca")
    void publicarExigePortariaBcaEPdfDaPortaria(DocumentoStatusRequestDto pedido) {
        documentoEm(NAO_PUBLICADO, EM_PUBLICACAO);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedido))
                .isInstanceOf(StatusCannotBeUpdatedException.class)
                .hasMessage("É obrigatório informar a portaria, o BCA de referência e o PDF da portaria.");
    }

    @ParameterizedTest(name = "alteração e revogação também exigem portaria, BCA e PDF (destino de {0})")
    @ValueSource(strings = {"EM_PUBLICACAO", "EM_REVOGACAO"})
    void alteracaoERevogacaoExigemPortariaBcaEPdf(String de) {
        documentoEm(PUBLICADO, SituacaoLocalEnum.valueOf(de));

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, apenas(SEM_ETAPA)))
                .isInstanceOf(StatusCannotBeUpdatedException.class)
                .hasMessage("É obrigatório informar a portaria, o BCA de referência e o PDF da portaria.");
    }

    static Stream<DocumentoStatusRequestDto> pedidosSemUmCampoDaParteNormativaPreliminar() {
        var d = LocalDate.of(2026, 3, 5);
        var lista = Stream.<DocumentoStatusRequestDto>builder();
        for (int faltante = 0; faltante < 5; faltante++) {
            for (var motivo : new String[]{null, "  "}) {
                var c = new String[]{"{}", "{}", "{}", "{}", "{}"};
                c[faltante] = motivo;
                lista.add(new DocumentoStatusRequestDto(SEM_ETAPA, null, null, "COMAER", "DIRAP", "123", d, 12, d,
                        c[0], c[1], c[2], c[3], c[4], "u"));
            }
        }
        return lista.build();
    }

    // Só a PRIMEIRA publicação exige a parte preliminar inteira.
    @ParameterizedTest(name = "primeira publicação sem um dos 5 campos da parte preliminar")
    @MethodSource("pedidosSemUmCampoDaParteNormativaPreliminar")
    void aPrimeiraPublicacaoExigeATodaAParteNormativaPreliminar(DocumentoStatusRequestDto pedido) {
        var doc = documentoEm(NAO_PUBLICADO, EM_PUBLICACAO);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID, pedido))
                .isInstanceOf(StatusCannotBeUpdatedException.class)
                .hasMessageContaining("epígrafe, ementa, preâmbulo, fecho e assinatura");
        assertThat(doc.getSituacaoLocal()).isEqualTo(EM_PUBLICACAO);
        assertThat(doc.getSituacaoBca()).isEqualTo(NAO_PUBLICADO);
    }

    // A portaria de publicação é a única que aparece na parte preliminar e é perene: alteração e
    // revogação não pedem esses campos e NUNCA a sobrescrevem.
    @Test
    void alteracaoNaoExigeENaoSobrescreveAParteNormativaPreliminar() {
        var doc = documentoEm(PUBLICADO, EM_PUBLICACAO);

        assertThatCode(() -> service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA))).doesNotThrowAnyException();

        assertThat(doc.getSituacaoBca()).isEqualTo(PUBLICADO);
        verify(documentoParteNormativaService, never()).salvarItensPreliminares(any(), any());
    }

    @Test
    void revogacaoNaoExigeENaoSobrescreveAParteNormativaPreliminar() {
        documentoEm(PUBLICADO, EM_REVOGACAO);

        assertThatCode(() -> service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA))).doesNotThrowAnyException();

        verify(documentoParteNormativaService, never()).salvarItensPreliminares(any(), any());
    }

    @Test
    void aPrimeiraPublicacaoGravaAParteNormativaPreliminarInformada() {
        documentoEm(NAO_PUBLICADO, EM_PUBLICACAO);

        service.changeStatus(DOC_ID, publicacaoValida());

        verify(documentoParteNormativaService, times(1)).salvarItensPreliminares(any(), any());
    }

    @ParameterizedTest(name = "BCA n° {0} é inválido")
    @ValueSource(ints = {0, -1, 367})
    void numeroDoBcaFicaEntre1E366(int numeroBca) {
        var d = LocalDate.of(2026, 3, 5);
        documentoEm(NAO_PUBLICADO, EM_PUBLICACAO);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID,
                publicacaoCom("COMAER", "DIRAP", "123", d, numeroBca, d, "{}", "u")))
                .hasMessage("O número do BCA deve estar entre 1 e 366.");
    }

    @ParameterizedTest(name = "BCA n° {0} é válido")
    @ValueSource(ints = {1, 366})
    void limitesDoNumeroDoBcaSaoAceitos(int numeroBca) {
        var d = LocalDate.of(2026, 3, 5);
        documentoEm(NAO_PUBLICADO, EM_PUBLICACAO);

        assertThatCode(() -> service.changeStatus(DOC_ID,
                publicacaoCom("COMAER", "DIRAP", "123", d, numeroBca, d, "{}", "u"))).doesNotThrowAnyException();
    }

    @Test
    void dataDaPortariaNaoPodeSerAnteriorADaAlteracaoAnterior() {
        var doc = documentoEm(PUBLICADO, EM_PUBLICACAO);
        doc.setDtPortariaReferencia(Timestamp.valueOf("2026-03-10 00:00:00"));
        var d = LocalDate.of(2026, 3, 9);

        assertThatThrownBy(() -> service.changeStatus(DOC_ID,
                publicacaoCom("COMAER", "DIRAP", "123", d, 12, LocalDate.of(2026, 3, 20), null, "u")))
                .hasMessage("A data da portaria não pode ser anterior à da alteração anterior.");
    }

    @Test
    void dataDoBcaNaoPodeSerAnteriorADaAlteracaoAnterior() {
        var doc = documentoEm(PUBLICADO, EM_PUBLICACAO);
        doc.setDtBcaReferencia(Timestamp.valueOf("2026-03-10 00:00:00"));

        assertThatThrownBy(() -> service.changeStatus(DOC_ID,
                publicacaoCom("COMAER", "DIRAP", "123", LocalDate.of(2026, 3, 20), 12, LocalDate.of(2026, 3, 9), null, "u")))
                .hasMessage("A data do BCA não pode ser anterior à da alteração anterior.");
    }

    @Test
    void mesmaDataDaAlteracaoAnteriorEPermitida() {
        var doc = documentoEm(PUBLICADO, EM_PUBLICACAO);
        doc.setDtPortariaReferencia(Timestamp.valueOf("2026-03-10 00:00:00"));
        doc.setDtBcaReferencia(Timestamp.valueOf("2026-03-10 00:00:00"));
        var d = LocalDate.of(2026, 3, 10);

        assertThatCode(() -> service.changeStatus(DOC_ID,
                publicacaoCom("COMAER", "DIRAP", "123", d, 12, d, null, "u"))).doesNotThrowAnyException();
    }

    @Test
    void oTipoDaPortariaDependeDaSituacaoBcaEDaEtapa() {
        documentoEm(NAO_PUBLICADO, EM_PUBLICACAO);
        service.changeStatus(DOC_ID, publicacaoValida());
        verifyTipoPortaria(TipoPortariaPublicacaoEnum.EDICAO);

        Mockito.clearInvocations(portariaPublicacaoService);
        documentoEm(PUBLICADO, EM_PUBLICACAO);
        service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA));
        verifyTipoPortaria(TipoPortariaPublicacaoEnum.ALTERACAO);

        Mockito.clearInvocations(portariaPublicacaoService);
        documentoEm(PUBLICADO, EM_REVOGACAO);
        service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA));
        verifyTipoPortaria(TipoPortariaPublicacaoEnum.REVOGACAO);
    }

    private void verifyTipoPortaria(TipoPortariaPublicacaoEnum esperado) {
        var tipo = ArgumentCaptor.forClass(TipoPortariaPublicacaoEnum.class);
        verify(portariaPublicacaoService).registrar(any(), tipo.capture(), any(), any(), any(), any(), any(), any(), any());
        assertThat(tipo.getValue()).isEqualTo(esperado);
    }

    @Test
    void soUmaAlteracaoConsolidaAsEmendasPendentes() {
        documentoEm(NAO_PUBLICADO, EM_PUBLICACAO);
        service.changeStatus(DOC_ID, publicacaoValida());
        verify(emendaService, never()).consolidarPublicacao(anyLong(), any(), any());

        documentoEm(PUBLICADO, EM_PUBLICACAO);
        service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA));
        verify(emendaService, times(1)).consolidarPublicacao(eq(DOC_ID), any(), any());
    }

    @Test
    void revogarNuncaConsolidaEmendas() {
        documentoEm(PUBLICADO, EM_REVOGACAO);

        service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA));

        verify(emendaService, never()).consolidarPublicacao(anyLong(), any(), any());
    }

    // ─── Efeitos colaterais ──────────────────────────────────────────────────────

    @Test
    void entrarEmAlteracaoRespacaOsElementOrdersDasTresPartes() {
        documentoEm(PUBLICADO, SEM_ETAPA);

        service.changeStatus(DOC_ID, apenas(EM_ALTERACAO));

        verify(normativaRepository).respacarElementOrders(DOC_ID);
        verify(preliminarRepository).respacarElementOrders(DOC_ID);
        verify(finalRepository).respacarElementOrders(DOC_ID);
    }

    @Test
    void outrasTransicoesNaoRespacam() {
        documentoEm(NAO_PUBLICADO, RASCUNHO);

        service.changeStatus(DOC_ID, apenas(MINUTA));

        verify(normativaRepository, never()).respacarElementOrders(anyLong());
    }

    // ─── Versão vigente x versão em tramitação (arquivos) ────────────────────────

    @Test
    void aprovarCongelaAVersaoEmTramitacaoSemTocarNaVigente() {
        var doc = documentoEm(PUBLICADO, EM_REVISAO);

        service.changeStatus(DOC_ID, pedidoCompleto(EM_PUBLICACAO));

        assertThat(doc.getUrlPdfTramitacao()).isEqualTo("pdf-gerado");
        assertThat(doc.getUrlHtmlTramitacao()).isEqualTo("html-gerado");
        // A vigente é a que a situação BCA descreve: só muda quando a portaria é registrada.
        assertThat(doc.getUrlPdf()).isEqualTo("pdf-vigente-anterior");
        assertThat(doc.getUrlHtml()).isEqualTo("html-vigente-anterior");
    }

    @Test
    void aprovarARevogacaoTambemCongelaAVersaoEmTramitacao() {
        var doc = documentoEm(PUBLICADO, ANALISE_REVOGACAO);

        service.changeStatus(DOC_ID, pedidoCompleto(EM_REVOGACAO));

        assertThat(doc.getUrlPdfTramitacao()).isEqualTo("pdf-gerado");
        assertThat(doc.getUrlPdf()).isEqualTo("pdf-vigente-anterior");
    }

    @ParameterizedTest(name = "publicar/revogar ({0}+{1}) grava a vigente e descarta a em tramitação")
    @MethodSource("publicacaoOuRevogacao")
    void registrarPortariaGravaAVersaoVigenteEDescartaAEmTramitacao(SituacaoBcaEnum bca, SituacaoLocalEnum de) {
        var doc = documentoEm(bca, de);
        doc.setUrlPdfTramitacao("pdf-congelado");
        doc.setUrlHtmlTramitacao("html-congelado");

        service.changeStatus(DOC_ID, pedidoCompleto(SEM_ETAPA));

        assertThat(doc.getUrlPdf()).isEqualTo("pdf-gerado");
        assertThat(doc.getUrlHtml()).isEqualTo("html-gerado");
        assertThat(doc.getUrlPdfTramitacao()).isNull();
        assertThat(doc.getUrlHtmlTramitacao()).isNull();
    }

    static Stream<Arguments> publicacaoOuRevogacao() {
        return Stream.of(
                Arguments.of(NAO_PUBLICADO, EM_PUBLICACAO),
                Arguments.of(PUBLICADO, EM_PUBLICACAO),
                Arguments.of(PUBLICADO, EM_REVOGACAO));
    }

    @ParameterizedTest(name = "{0}+{1} → {2} descarta a versão em tramitação e preserva a vigente")
    @MethodSource("descartesDeTramitacao")
    void devolverECancelarDescartamAVersaoEmTramitacao(SituacaoBcaEnum bca, SituacaoLocalEnum de, SituacaoLocalEnum para) {
        var doc = documentoEm(bca, de);
        doc.setUrlPdfTramitacao("pdf-congelado");
        doc.setUrlHtmlTramitacao("html-congelado");

        service.changeStatus(DOC_ID, apenas(para));

        assertThat(doc.getUrlPdfTramitacao()).isNull();
        assertThat(doc.getUrlHtmlTramitacao()).isNull();
        if (bca != NAO_PUBLICADO) assertThat(doc.getUrlPdf()).isEqualTo("pdf-vigente-anterior");
        verify(documentoPdfService, never()).gerarEArmazenarPdf(any());
    }

    static Stream<Arguments> descartesDeTramitacao() {
        return Stream.of(
                Arguments.of(NAO_PUBLICADO, EM_PUBLICACAO, MINUTA),
                Arguments.of(PUBLICADO, EM_PUBLICACAO, EM_ALTERACAO),
                Arguments.of(PUBLICADO, ANALISE_REVOGACAO, SEM_ETAPA),
                Arguments.of(PUBLICADO, EM_ALTERACAO, SEM_ETAPA),
                Arguments.of(NAO_PUBLICADO, MINUTA, CANCELADO));
    }

    @ParameterizedTest(name = "{0}+{1} → {2} não gera arquivo nenhum")
    @MethodSource("etapasQueNaoGeramArquivo")
    void etapasEmQueOTextoAindaMudaNaoArmazenamArquivos(SituacaoBcaEnum bca, SituacaoLocalEnum de, SituacaoLocalEnum para) {
        documentoEm(bca, de);

        service.changeStatus(DOC_ID, pedidoCompleto(para));

        verify(documentoPdfService, never()).gerarEArmazenarPdf(any());
        verify(documentoHtmlService, never()).gerarEArmazenarHtml(any());
    }

    static Stream<Arguments> etapasQueNaoGeramArquivo() {
        return Stream.of(
                Arguments.of(NAO_PUBLICADO, RASCUNHO, MINUTA),
                Arguments.of(NAO_PUBLICADO, MINUTA, EM_REVISAO),
                Arguments.of(PUBLICADO, SEM_ETAPA, EM_ALTERACAO),
                Arguments.of(PUBLICADO, EM_ALTERACAO, EM_REVISAO),
                Arguments.of(PUBLICADO, SEM_ETAPA, ANALISE_REVOGACAO));
    }

    @Test
    void falhaAoGerarOsArquivosNaoDesfazAMudancaDeEtapa() {
        var doc = documentoEm(NAO_PUBLICADO, EM_REVISAO);
        when(documentoPdfService.gerarEArmazenarPdf(any())).thenThrow(new RuntimeException("MinIO fora do ar"));
        when(documentoHtmlService.gerarEArmazenarHtml(any())).thenThrow(new RuntimeException("MinIO fora do ar"));

        assertThatCode(() -> service.changeStatus(DOC_ID, pedidoCompleto(EM_PUBLICACAO))).doesNotThrowAnyException();
        assertThat(doc.getSituacaoLocal()).isEqualTo(EM_PUBLICACAO);
    }

    // Os arquivos leem o documento numa transação própria (REQUIRES_NEW): gerar antes do commit
    // produziria arquivo com as cláusulas de emenda ainda pendentes e a situação BCA antiga.
    @Test
    void comTransacaoEmAndamentoOsArquivosSoSaoGeradosDepoisDoCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            var doc = documentoEm(PUBLICADO, EM_PUBLICACAO);
            doc.setUrlPdfTramitacao("pdf-congelado");

            service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA));

            // Antes do commit: nada gerado; a versão em tramitação já foi descartada (na transação).
            verify(documentoPdfService, never()).gerarEArmazenarPdf(any());
            verify(documentoHtmlService, never()).gerarEArmazenarHtml(any());
            assertThat(doc.getUrlPdfTramitacao()).isNull();

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            verify(documentoPdfService).gerarEArmazenarPdf(doc);
            verify(documentoHtmlService).gerarEArmazenarHtml(doc);
            assertThat(doc.getUrlPdf()).isEqualTo("pdf-gerado");
            assertThat(doc.getUrlHtml()).isEqualTo("html-gerado");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void aprovarComTransacaoEmAndamentoTambemGeraAVersaoEmTramitacaoSoDepoisDoCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            var doc = documentoEm(NAO_PUBLICADO, EM_REVISAO);

            service.changeStatus(DOC_ID, pedidoCompleto(EM_PUBLICACAO));
            verify(documentoPdfService, never()).gerarEArmazenarPdf(any());

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            assertThat(doc.getUrlPdfTramitacao()).isEqualTo("pdf-gerado");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void oArquivoVigenteEGeradoComODocumentoJaSemEtapa() {
        // A marca d'água da versão vigente é nenhuma: o arquivo é gerado DEPOIS de a etapa virar
        // SEM_ETAPA (ver DocumentoFoContext.buildStaticContentWatermark).
        var doc = documentoEm(NAO_PUBLICADO, EM_PUBLICACAO);
        var etapaNoMomentoDaGeracao = new SituacaoLocalEnum[1];
        when(documentoPdfService.gerarEArmazenarPdf(any())).thenAnswer(inv -> {
            etapaNoMomentoDaGeracao[0] = ((Documento) inv.getArgument(0)).getSituacaoLocal();
            return "pdf-gerado";
        });

        service.changeStatus(DOC_ID, publicacaoValida());

        assertThat(etapaNoMomentoDaGeracao[0]).isEqualTo(SEM_ETAPA);
        assertThat(doc.getUrlPdf()).isEqualTo("pdf-gerado");
    }

    @Test
    void cadaTransicaoFicaRegistradaNoHistorico() {
        var doc = documentoEm(NAO_PUBLICADO, RASCUNHO);

        service.changeStatus(DOC_ID, apenas(MINUTA));

        verify(documentoHistoricoService).registrar(doc, TipoAlteracaoEnum.ALTERACAO_STATUS,
                "RASCUNHO → MINUTA", RASCUNHO, MINUTA);
    }

    @Test
    void publicarUmaAlteracaoNaoMencionaMudancaDeSituacaoBcaNoHistorico() {
        var doc = documentoEm(PUBLICADO, EM_PUBLICACAO);

        service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA));

        verify(documentoHistoricoService).registrar(doc, TipoAlteracaoEnum.ALTERACAO_STATUS,
                "EM_PUBLICACAO → SEM_ETAPA", EM_PUBLICACAO, SEM_ETAPA);
    }

    @Test
    void revogarRegistraAMudancaDeSituacaoBcaNoHistorico() {
        var doc = documentoEm(PUBLICADO, EM_REVOGACAO);

        service.changeStatus(DOC_ID, somentePortariaEBca(SEM_ETAPA));

        verify(documentoHistoricoService).registrar(doc, TipoAlteracaoEnum.ALTERACAO_STATUS,
                "EM_REVOGACAO → SEM_ETAPA (situação BCA: PUBLICADO → REVOGADO)", EM_REVOGACAO, SEM_ETAPA);
    }
}
