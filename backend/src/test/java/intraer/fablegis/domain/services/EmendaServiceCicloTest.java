package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.emendaDtos.EmendaAcaoEnum;
import intraer.fablegis.application.dtos.emendaDtos.EmendaElementoRequestDto;
import intraer.fablegis.application.dtos.emendaDtos.EmendaIncluirRequestDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.EmendaHistorico;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemParteFinal;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemPartePreliminar;
import intraer.fablegis.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.handlers.exceptions.ConflitoEdicaoException;
import intraer.fablegis.domain.handlers.exceptions.RegraDeNegocioException;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import intraer.fablegis.infrastructure.repositories.EmendaHistoricoRepository;
import intraer.fablegis.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import intraer.fablegis.infrastructure.repositories.ItemParteFinalRepository;
import intraer.fablegis.infrastructure.repositories.ItemPartePreliminarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// O ciclo de emenda de um documento em EM_ALTERACAO (docs/ciclo-de-vida.md, "Ciclo de emenda"; LC 95/1998, art. 12):
// alterar, revogar, desfazer, incluir e reordenar artigos incluídos, mais o mapa de alteração (Quadro de Justificativas,
// NSCA 5-3, Anexo XXIV). A consolidação na publicação e as pendências têm testes próprios
// (EmendaServiceConsolidacaoTest, EmendaServicePendentesTest).
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmendaServiceCicloTest {

    private static final long DOC = 7L;
    private static final long OUTRO_DOC = 8L;
    private static final String CLAUSULA = "(redação dada pela Portaria X, publicada no BCA Y)";

    @Mock DocumentoRepository documentoRepository;
    @Mock ItemAnexoParteNormativaRepository normativaRepository;
    @Mock ItemPartePreliminarRepository preliminarRepository;
    @Mock ItemParteFinalRepository finalRepository;
    @Mock EmendaHistoricoRepository historicoRepository;

    EmendaService service;
    Documento documento;

    @BeforeEach
    void preparar() {
        documento = documento(DOC, SituacaoLocalEnum.EM_ALTERACAO, 3);
        when(documentoRepository.findById(DOC)).thenReturn(Optional.of(documento));
        when(documentoRepository.getReferenceById(DOC)).thenReturn(documento);
        // O serviço real de concorrência (só toca o próprio documento): a trava otimista faz parte da regra.
        service = new EmendaService(documentoRepository, normativaRepository, preliminarRepository, finalRepository,
                historicoRepository, new DocumentoConcorrenciaService(documentoRepository));
    }

    // ─── Construção dos cenários ─────────────────────────────────────────────

    private static Documento documento(long id, SituacaoLocalEnum situacao, int versao) {
        var d = new Documento();
        d.setId(id);
        d.setSituacaoLocal(situacao);
        d.setVersao(versao);
        return d;
    }

    private ItemAnexoParteNormativa artigo(long id, ElementoEmendaStatusEnum status, String clausula, int ordem) {
        var i = new ItemAnexoParteNormativa();
        i.setId(id);
        i.setDocumento(documento);
        i.setTipo(ItemAnexoParteNormativaTipoEnum.ARTIGO);
        i.setTitulo("Art. " + id);
        i.setConteudo("texto " + id);
        i.setEmendaStatus(status);
        i.setClausulaEmenda(clausula);
        i.setElementOrder(ordem);
        return i;
    }

    private ItemAnexoParteNormativa normativo(ItemAnexoParteNormativa item) {
        when(normativaRepository.findById(item.getId())).thenReturn(Optional.of(item));
        return item;
    }

    private static EmendaElementoRequestDto pedido(EmendaAcaoEnum acao, String novoConteudo, String novoTitulo, String justificativa) {
        return new EmendaElementoRequestDto(acao, novoConteudo, novoTitulo, justificativa, null);
    }

    private EmendaHistorico historicoGravado() {
        var captor = ArgumentCaptor.forClass(EmendaHistorico.class);
        verify(historicoRepository).save(captor.capture());
        return captor.getValue();
    }

    private void emendarNormativo(long id, EmendaElementoRequestDto req) {
        service.emendar(DOC, "PARTE_NORMATIVA", id, req);
    }

    // ─── Pré-condições ───────────────────────────────────────────────────────

    @Test
    void soSeEmendaDocumentoEmAlteracao() {
        documento.setSituacaoLocal(SituacaoLocalEnum.RASCUNHO);
        normativo(artigo(1, ElementoEmendaStatusEnum.INALTERADO, null, 1));

        assertThatThrownBy(() -> emendarNormativo(1, pedido(EmendaAcaoEnum.ALTERAR, "novo", null, "motivo")))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("EM_ALTERACAO");
        verify(normativaRepository, never()).save(any());
    }

    @Test
    void documentoInexistenteEDeixaDeSerEncontrado() {
        assertThatThrownBy(() -> service.emendar(99L, "PARTE_NORMATIVA", 1L, pedido(EmendaAcaoEnum.REVOGAR, null, null, "m")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void secaoQueNaoEUmaSecaoEErroDeQuemPediu() {
        assertThatThrownBy(() -> service.emendar(DOC, "PARTE_QUALQUER", 1L, pedido(EmendaAcaoEnum.REVOGAR, null, null, "m")))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Seção inválida");
    }

    @Test
    void elementoDeOutroDocumentoNaoEhEncontrado() {
        var item = artigo(1, ElementoEmendaStatusEnum.INALTERADO, null, 1);
        item.setDocumento(documento(OUTRO_DOC, SituacaoLocalEnum.EM_ALTERACAO, 1));
        normativo(item);

        assertThatThrownBy(() -> emendarNormativo(1, pedido(EmendaAcaoEnum.REVOGAR, null, null, "m")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void versaoDesatualizadaDoDocumentoEConflitoDeEdicao() {
        normativo(artigo(1, ElementoEmendaStatusEnum.INALTERADO, null, 1));
        var req = new EmendaElementoRequestDto(EmendaAcaoEnum.REVOGAR, null, null, "m", 2);   // o documento está na versão 3

        assertThatThrownBy(() -> emendarNormativo(1, req)).isInstanceOf(ConflitoEdicaoException.class);
        verify(normativaRepository, never()).save(any());
    }

    // ─── Alterar e revogar ───────────────────────────────────────────────────

    @Test
    void alterarGuardaARedacaoNovaAoLadoDaVigenteERegistraOHistorico() {
        var item = normativo(artigo(1, ElementoEmendaStatusEnum.INALTERADO, null, 1));

        emendarNormativo(1, pedido(EmendaAcaoEnum.ALTERAR, "texto novo", "Art. 1º novo", "ajuste de redação"));

        assertThat(item.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.ALTERADO);
        assertThat(item.getConteudo()).isEqualTo("texto 1");            // a redação em vigor segue intacta (será tachada)
        assertThat(item.getConteudoEmenda()).isEqualTo("texto novo");
        assertThat(item.getTituloEmenda()).isEqualTo("Art. 1º novo");
        assertThat(item.getJustificativaEmenda()).isEqualTo("ajuste de redação");
        assertThat(item.getClausulaEmenda()).isNull();                  // a cláusula só nasce na publicação
        verify(normativaRepository).save(item);

        var h = historicoGravado();
        assertThat(h.getAcao()).isEqualTo(EmendaAcaoEnum.ALTERAR);
        assertThat(h.getSecao()).isEqualTo(SecaoDocumentoEnum.PARTE_NORMATIVA);
        assertThat(h.getConteudoAnterior()).isEqualTo("texto 1");
        assertThat(h.getConteudoNovo()).isEqualTo("texto novo");
        assertThat(h.getJustificativa()).isEqualTo("ajuste de redação");
    }

    @Test
    void revogarMarcaORevogadoMantendoOTexto() {
        var item = normativo(artigo(1, ElementoEmendaStatusEnum.INALTERADO, null, 1));

        emendarNormativo(1, pedido(EmendaAcaoEnum.REVOGAR, null, null, "perdeu o objeto"));

        assertThat(item.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.REVOGADO);
        assertThat(item.getConteudo()).isEqualTo("texto 1");
        assertThat(historicoGravado().getAcao()).isEqualTo(EmendaAcaoEnum.REVOGAR);
    }

    @Test
    void alterarOuRevogarExigeJustificativa() {
        normativo(artigo(1, ElementoEmendaStatusEnum.INALTERADO, null, 1));

        for (var acao : List.of(EmendaAcaoEnum.ALTERAR, EmendaAcaoEnum.REVOGAR)) {
            assertThatThrownBy(() -> emendarNormativo(1, pedido(acao, "x", null, "  ")))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Justificativa");
        }
        verify(normativaRepository, never()).save(any());
    }

    @Test
    void asTresPartesSaoEmendadasPelaMesmaRegra() {
        var preliminar = new ItemPartePreliminar();
        preliminar.setId(2L);
        preliminar.setDocumento(documento);
        preliminar.setConteudo("ementa");
        preliminar.setEmendaStatus(ElementoEmendaStatusEnum.INALTERADO);
        when(preliminarRepository.findById(2L)).thenReturn(Optional.of(preliminar));
        var parteFinal = new ItemParteFinal();
        parteFinal.setId(3L);
        parteFinal.setDocumento(documento);
        parteFinal.setConteudo("fecho");
        parteFinal.setEmendaStatus(ElementoEmendaStatusEnum.INALTERADO);
        when(finalRepository.findById(3L)).thenReturn(Optional.of(parteFinal));

        service.emendar(DOC, "PARTE_PRELIMINAR", 2L, pedido(EmendaAcaoEnum.ALTERAR, "ementa nova", null, "m"));
        service.emendar(DOC, "parte_final", 3L, pedido(EmendaAcaoEnum.REVOGAR, null, null, "m"));

        assertThat(preliminar.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.ALTERADO);
        assertThat(preliminar.getConteudoEmenda()).isEqualTo("ementa nova");
        assertThat(parteFinal.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.REVOGADO);
    }

    // ─── Desfazer ────────────────────────────────────────────────────────────

    @Test
    void desfazerUmaAlteracaoPendenteVoltaAoInalterado() {
        var item = normativo(artigo(1, ElementoEmendaStatusEnum.ALTERADO, null, 1));
        item.setConteudoEmenda("texto novo");
        item.setTituloEmenda("t novo");
        item.setJustificativaEmenda("m");

        emendarNormativo(1, pedido(EmendaAcaoEnum.DESFAZER, null, null, null));

        assertThat(item.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.INALTERADO);
        assertThat(item.getConteudoEmenda()).isNull();
        assertThat(item.getTituloEmenda()).isNull();
        assertThat(item.getJustificativaEmenda()).isNull();
        assertThat(item.getConteudo()).isEqualTo("texto 1");
        assertThat(historicoGravado().getAcao()).isEqualTo(EmendaAcaoEnum.DESFAZER);
    }

    @Test
    void desfazerUmaInclusaoPendenteApagaOElemento() {
        var item = normativo(artigo(1, ElementoEmendaStatusEnum.INCLUIDO, null, 1));

        emendarNormativo(1, pedido(EmendaAcaoEnum.DESFAZER, null, null, null));

        verify(normativaRepository).delete(item);
        verify(normativaRepository, never()).save(any());
    }

    // O pai (cascade ALL) re-persistiria o filho apagado no flush: desfazer a inclusão de um
    // artigo dentro de um capítulo só vale se o filho sair da coleção do pai.
    @Test
    void desfazerUmaInclusaoPendenteDentroDeUmCapituloTiraOElementoDosFilhosDoPai() {
        var capitulo = normativo(artigo(10, ElementoEmendaStatusEnum.INALTERADO, null, 1));
        capitulo.setTipo(ItemAnexoParteNormativaTipoEnum.CAPITULO);
        var existente = artigo(11, ElementoEmendaStatusEnum.INALTERADO, null, 1);
        var incluido = normativo(artigo(12, ElementoEmendaStatusEnum.INCLUIDO, null, 2));
        existente.setParent(capitulo);
        incluido.setParent(capitulo);
        capitulo.setChildren(new java.util.ArrayList<>(List.of(existente, incluido)));

        emendarNormativo(12, pedido(EmendaAcaoEnum.DESFAZER, null, null, null));

        verify(normativaRepository).delete(incluido);
        assertThat(capitulo.getChildren()).containsExactly(existente);
    }

    @Test
    void emendaJaPublicadaNaoSeDesfaz() {
        normativo(artigo(1, ElementoEmendaStatusEnum.ALTERADO, CLAUSULA, 1));

        assertThatThrownBy(() -> emendarNormativo(1, pedido(EmendaAcaoEnum.DESFAZER, null, null, null)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("já publicado");
    }

    @Test
    void revogacaoPublicadaEPermanenteParaQualquerAcao() {
        normativo(artigo(1, ElementoEmendaStatusEnum.REVOGADO, CLAUSULA, 1));

        for (var acao : EmendaAcaoEnum.values()) {
            assertThatThrownBy(() -> emendarNormativo(1, pedido(acao, "x", null, "m")))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("permanente");
        }
    }

    @Test
    void revogacaoAindaPendenteAindaPodeSerDesfeita() {
        var item = normativo(artigo(1, ElementoEmendaStatusEnum.REVOGADO, null, 1));

        emendarNormativo(1, pedido(EmendaAcaoEnum.DESFAZER, null, null, null));

        assertThat(item.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.INALTERADO);
    }

    // ─── Inclusão pendente: edição livre ─────────────────────────────────────

    @Test
    void artigoIncluidoAindaNaoPublicadoEditaLivreSemJustificativaNemHistorico() {
        var item = normativo(artigo(1, ElementoEmendaStatusEnum.INCLUIDO, null, 1));

        emendarNormativo(1, pedido(EmendaAcaoEnum.ALTERAR, "redação melhor", "Art. 1º-A", null));

        assertThat(item.getConteudo()).isEqualTo("redação melhor");
        assertThat(item.getTitulo()).isEqualTo("Art. 1º-A");
        assertThat(item.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.INCLUIDO);
        verify(historicoRepository, never()).save(any());
    }

    @Test
    void inclusaoPendenteNaoSeRevogaSeDescarta() {
        normativo(artigo(1, ElementoEmendaStatusEnum.INCLUIDO, null, 1));

        assertThatThrownBy(() -> emendarNormativo(1, pedido(EmendaAcaoEnum.REVOGAR, null, null, "m")))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("excluir");
    }

    // ─── O ciclo se repete sobre o que já foi publicado ──────────────────────

    @Test
    void novaAlteracaoSobreUmaJaPublicadaPartiDaRedacaoVigenteEGuardaAClausulaAnterior() {
        var item = normativo(artigo(1, ElementoEmendaStatusEnum.ALTERADO, CLAUSULA, 1));
        item.setConteudoEmenda("redação vigente");
        item.setTituloEmenda("título vigente");

        emendarNormativo(1, pedido(EmendaAcaoEnum.ALTERAR, "redação de agora", null, "nova mudança"));

        assertThat(item.getConteudo()).isEqualTo("redação vigente");        // a vigente vira a "anterior" (tachada)
        assertThat(item.getTitulo()).isEqualTo("título vigente");
        assertThat(item.getConteudoEmenda()).isEqualTo("redação de agora");
        assertThat(item.getClausulaEmendaAnterior()).isEqualTo(CLAUSULA);  // a antiga segue visível, riscada
        assertThat(item.getClausulaEmenda()).isNull();                      // pendente de novo, a cláusula nova vem na publicação
        assertThat(item.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.ALTERADO);
    }

    @Test
    void inclusaoPublicadaPodeSerRevogadaMasExigeJustificativa() {
        var item = normativo(artigo(1, ElementoEmendaStatusEnum.INCLUIDO, CLAUSULA, 1));

        assertThatThrownBy(() -> emendarNormativo(1, pedido(EmendaAcaoEnum.REVOGAR, null, null, null)))
                .isInstanceOf(RegraDeNegocioException.class);

        emendarNormativo(1, pedido(EmendaAcaoEnum.REVOGAR, null, null, "obsoleto"));
        assertThat(item.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.REVOGADO);
        assertThat(item.getClausulaEmendaAnterior()).isEqualTo(CLAUSULA);
        assertThat(item.getClausulaEmenda()).isNull();
    }

    // ─── Incluir ─────────────────────────────────────────────────────────────

    private EmendaIncluirRequestDto inclusao(Long parentId, String justificativa) {
        return new EmendaIncluirRequestDto(ItemAnexoParteNormativaTipoEnum.ARTIGO, "Art. novo", "texto do novo", parentId, 5,
                justificativa, null);
    }

    @Test
    void incluirExigeJustificativa() {
        assertThatThrownBy(() -> service.incluir(DOC, "PARTE_NORMATIVA", inclusao(null, " ")))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Justificativa");
        verify(normativaRepository, never()).save(any());
    }

    @Test
    void incluirNaParteNormativaCriaElementoIncluidoMarcadoPorEmendaERegistraOHistorico() {
        when(normativaRepository.save(any())).thenAnswer(inv -> {
            ItemAnexoParteNormativa salvo = inv.getArgument(0);
            salvo.setId(50L);
            return salvo;
        });
        var pai = artigo(9, ElementoEmendaStatusEnum.INALTERADO, null, 1);
        pai.setTipo(ItemAnexoParteNormativaTipoEnum.CAPITULO);
        normativo(pai);

        service.incluir(DOC, "PARTE_NORMATIVA", inclusao(9L, "necessário"));

        var captor = ArgumentCaptor.forClass(ItemAnexoParteNormativa.class);
        verify(normativaRepository).save(captor.capture());
        var criado = captor.getValue();
        assertThat(criado.getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.INCLUIDO);
        assertThat(criado.isIncluidoPorEmenda()).isTrue();
        assertThat(criado.getParent()).isSameAs(pai);
        assertThat(criado.getElementOrder()).isEqualTo(5);
        assertThat(criado.getClausulaEmenda()).isNull();

        var h = historicoGravado();
        assertThat(h.getAcao()).isEqualTo(EmendaAcaoEnum.INCLUIR);
        assertThat(h.getElementoId()).isEqualTo(50L);
        assertThat(h.getConteudoAnterior()).isNull();
        assertThat(h.getConteudoNovo()).isEqualTo("texto do novo");
    }

    @Test
    void paiDeOutroDocumentoNaoEhEncontradoNaInclusao() {
        var pai = artigo(9, ElementoEmendaStatusEnum.INALTERADO, null, 1);
        pai.setDocumento(documento(OUTRO_DOC, SituacaoLocalEnum.EM_ALTERACAO, 1));
        normativo(pai);

        assertThatThrownBy(() -> service.incluir(DOC, "PARTE_NORMATIVA", inclusao(9L, "m")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void incluirNasPartesPreliminarEFinalCriaElementoIncluido() {
        when(preliminarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(finalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.incluir(DOC, "PARTE_PRELIMINAR", inclusao(null, "m"));
        service.incluir(DOC, "PARTE_FINAL", inclusao(null, "m"));

        var pre = ArgumentCaptor.forClass(ItemPartePreliminar.class);
        verify(preliminarRepository).save(pre.capture());
        assertThat(pre.getValue().getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.INCLUIDO);
        var fim = ArgumentCaptor.forClass(ItemParteFinal.class);
        verify(finalRepository).save(fim.capture());
        assertThat(fim.getValue().getEmendaStatus()).isEqualTo(ElementoEmendaStatusEnum.INCLUIDO);
    }

    // ─── Reordenar artigos incluídos ─────────────────────────────────────────

    // Três artigos na raiz, na ordem 1, 2, 3 (ids 11, 12, 13).
    private List<ItemAnexoParteNormativa> raiz(ItemAnexoParteNormativa a, ItemAnexoParteNormativa b, ItemAnexoParteNormativa c) {
        var lista = List.of(a, b, c);
        when(normativaRepository.findRootItemsByDocumentoId(DOC)).thenReturn(lista);
        lista.forEach(this::normativo);
        return lista;
    }

    @Test
    void doisArtigosIncluidosNaoPublicadosTrocamDePosicao() {
        var original = artigo(11, ElementoEmendaStatusEnum.INALTERADO, null, 1);
        var incluidoA = artigo(12, ElementoEmendaStatusEnum.INCLUIDO, null, 2);
        var incluidoB = artigo(13, ElementoEmendaStatusEnum.INCLUIDO, null, 3);
        raiz(original, incluidoA, incluidoB);

        service.reordenarIncluido(DOC, "PARTE_NORMATIVA", 13L, "CIMA");

        assertThat(incluidoB.getElementOrder()).isEqualTo(2);
        assertThat(incluidoA.getElementOrder()).isEqualTo(3);
        assertThat(original.getElementOrder()).isEqualTo(1);
        verify(normativaRepository).save(incluidoA);
        verify(normativaRepository).save(incluidoB);
    }

    @Test
    void nuncaTrocaDePosicaoComUmArtigoOriginalPoisSeriaRenumeralo() {
        var original = artigo(11, ElementoEmendaStatusEnum.INALTERADO, null, 1);
        var incluido = artigo(12, ElementoEmendaStatusEnum.INCLUIDO, null, 2);
        var outro = artigo(13, ElementoEmendaStatusEnum.INALTERADO, null, 3);
        raiz(original, incluido, outro);

        assertThatThrownBy(() -> service.reordenarIncluido(DOC, "PARTE_NORMATIVA", 12L, "CIMA"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("outro artigo incluído");
        assertThatThrownBy(() -> service.reordenarIncluido(DOC, "PARTE_NORMATIVA", 12L, "BAIXO"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("outro artigo incluído");
        verify(normativaRepository, never()).save(any());
    }

    @Test
    void artigoIncluidoJaPublicadoTemPosicaoDefinitivaENaoSeReordena() {
        var publicado = artigo(12, ElementoEmendaStatusEnum.INCLUIDO, CLAUSULA, 2);
        var incluido = artigo(13, ElementoEmendaStatusEnum.INCLUIDO, null, 3);
        raiz(artigo(11, ElementoEmendaStatusEnum.INALTERADO, null, 1), publicado, incluido);

        assertThatThrownBy(() -> service.reordenarIncluido(DOC, "PARTE_NORMATIVA", 12L, "BAIXO"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("ainda não publicados");
        assertThatThrownBy(() -> service.reordenarIncluido(DOC, "PARTE_NORMATIVA", 13L, "CIMA"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void soSeReordenaArtigo() {
        var inciso = artigo(12, ElementoEmendaStatusEnum.INCLUIDO, null, 2);
        inciso.setTipo(ItemAnexoParteNormativaTipoEnum.INCISO);
        raiz(artigo(11, ElementoEmendaStatusEnum.INCLUIDO, null, 1), inciso, artigo(13, ElementoEmendaStatusEnum.INCLUIDO, null, 3));

        assertThatThrownBy(() -> service.reordenarIncluido(DOC, "PARTE_NORMATIVA", 12L, "CIMA"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void reordenarSoNaParteNormativa() {
        assertThatThrownBy(() -> service.reordenarIncluido(DOC, "PARTE_FINAL", 1L, "CIMA"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("parte normativa");
    }

    @Test
    void naoSeMovePraAlemDasExtremidades() {
        var a = artigo(11, ElementoEmendaStatusEnum.INCLUIDO, null, 1);
        var b = artigo(12, ElementoEmendaStatusEnum.INCLUIDO, null, 2);
        var c = artigo(13, ElementoEmendaStatusEnum.INCLUIDO, null, 3);
        raiz(a, b, c);

        assertThatThrownBy(() -> service.reordenarIncluido(DOC, "PARTE_NORMATIVA", 11L, "CIMA"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("nessa direção");
        assertThatThrownBy(() -> service.reordenarIncluido(DOC, "PARTE_NORMATIVA", 13L, "BAIXO"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("nessa direção");
    }

    @Test
    void reordenarExigeDocumentoEmAlteracao() {
        documento.setSituacaoLocal(SituacaoLocalEnum.RASCUNHO);

        assertThatThrownBy(() -> service.reordenarIncluido(DOC, "PARTE_NORMATIVA", 1L, "CIMA"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("EM_ALTERACAO");
    }

    // ─── Mapa de alteração ───────────────────────────────────────────────────

    private static EmendaHistorico linhaDoHistorico(long elemento, EmendaAcaoEnum acao, String ciclo, LocalDateTime quando, String novo) {
        return EmendaHistorico.builder().id(elemento * 100 + quando.getMinute()).documentoId(DOC)
                .secao(SecaoDocumentoEnum.PARTE_NORMATIVA).elementoId(elemento).acao(acao)
                .conteudoNovo(novo).cicloReferencia(ciclo).dtEmenda(quando).build();
    }

    @Test
    void mapaTrazOsPendentesDoElementoVivoAntesDosCiclosPublicados() {
        var alterado = artigo(1, ElementoEmendaStatusEnum.ALTERADO, null, 1);
        alterado.setConteudoEmenda("redação nova");
        alterado.setTituloEmenda("t novo");
        var incluido = artigo(2, ElementoEmendaStatusEnum.INCLUIDO, null, 2);
        var revogado = artigo(3, ElementoEmendaStatusEnum.REVOGADO, null, 3);
        var jaPublicado = artigo(4, ElementoEmendaStatusEnum.ALTERADO, CLAUSULA, 4);   // não é pendente
        var inalterado = artigo(5, ElementoEmendaStatusEnum.INALTERADO, null, 5);
        when(normativaRepository.findAllByDocumentoId(DOC)).thenReturn(List.of(alterado, incluido, revogado, jaPublicado, inalterado));

        var mapa = service.listarMapaAlteracao(DOC);

        assertThat(mapa).hasSize(3);
        assertThat(mapa).allSatisfy(l -> assertThat(l.cicloReferencia()).isNull());
        var linhaAlterado = mapa.get(0);
        assertThat(linhaAlterado.acao()).isEqualTo(EmendaAcaoEnum.ALTERAR);
        assertThat(linhaAlterado.textoAnterior()).isEqualTo("texto 1");
        assertThat(linhaAlterado.textoNovo()).isEqualTo("redação nova");
        assertThat(linhaAlterado.tituloNovo()).isEqualTo("t novo");
        var linhaIncluido = mapa.get(1);
        assertThat(linhaIncluido.acao()).isEqualTo(EmendaAcaoEnum.INCLUIR);
        assertThat(linhaIncluido.textoAnterior()).isNull();
        assertThat(linhaIncluido.textoNovo()).isEqualTo("texto 2");
        var linhaRevogado = mapa.get(2);
        assertThat(linhaRevogado.acao()).isEqualTo(EmendaAcaoEnum.REVOGAR);
        assertThat(linhaRevogado.textoAnterior()).isEqualTo("texto 3");
        assertThat(linhaRevogado.textoNovo()).isNull();
    }

    @Test
    void mapaDosCiclosPublicadosMantemSoAUltimaLinhaPorElementoIgnoraDesfazerECicloAberto() {
        var t0 = LocalDateTime.of(2026, 3, 1, 10, 0);
        var antiga = linhaDoHistorico(1, EmendaAcaoEnum.ALTERAR, "Portaria 1 (BCA 1)", t0, "primeira tentativa");
        var maisNova = linhaDoHistorico(1, EmendaAcaoEnum.ALTERAR, "Portaria 1 (BCA 1)", t0.plusMinutes(5), "versão final");
        var desfeita = linhaDoHistorico(2, EmendaAcaoEnum.DESFAZER, "Portaria 1 (BCA 1)", t0.plusMinutes(6), null);
        var cicloAberto = linhaDoHistorico(3, EmendaAcaoEnum.ALTERAR, null, t0.plusMinutes(7), "ainda sem publicar");
        var outroCiclo = linhaDoHistorico(1, EmendaAcaoEnum.REVOGAR, "Portaria 2 (BCA 2)", t0.plusMinutes(30), null);
        // A consulta devolve as mais novas primeiro.
        when(historicoRepository.findByDocumentoIdOrderByDtEmendaDesc(DOC))
                .thenReturn(List.of(outroCiclo, cicloAberto, desfeita, maisNova, antiga));

        var mapa = service.listarMapaAlteracao(DOC);

        assertThat(mapa).hasSize(2);                                            // sem pendentes vivos (os mocks devolvem vazio)
        assertThat(mapa.get(0).cicloReferencia()).isEqualTo("Portaria 2 (BCA 2)");   // ciclo mais recente primeiro
        assertThat(mapa.get(1).cicloReferencia()).isEqualTo("Portaria 1 (BCA 1)");
        assertThat(mapa.get(1).textoNovo()).isEqualTo("versão final");          // reedição antes da publicação não fica obsoleta
    }

    @Test
    void umMesmoElementoEmDoisCiclosApareceNosDois() {
        var t0 = LocalDateTime.of(2026, 3, 1, 10, 0);
        when(historicoRepository.findByDocumentoIdOrderByDtEmendaDesc(DOC)).thenReturn(List.of(
                linhaDoHistorico(1, EmendaAcaoEnum.ALTERAR, "Portaria 2 (BCA 2)", t0.plusMinutes(30), "b"),
                linhaDoHistorico(1, EmendaAcaoEnum.ALTERAR, "Portaria 1 (BCA 1)", t0, "a")));

        assertThat(service.listarMapaAlteracao(DOC)).extracting("cicloReferencia")
                .containsExactly("Portaria 2 (BCA 2)", "Portaria 1 (BCA 1)");
    }
}
