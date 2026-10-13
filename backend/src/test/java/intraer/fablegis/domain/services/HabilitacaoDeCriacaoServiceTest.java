package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.domain.entities.usuario.HabilitacaoDaOm;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.regras.RegraDeHabilitacaoDaOm;
import intraer.fablegis.domain.regras.RegrasDaEspecieNormativa;
import intraer.fablegis.domain.regras.RegrasDasEspecies;
import intraer.fablegis.domain.regras.TipoDeEspecie;
import intraer.fablegis.infrastructure.repositories.EspecieNormativaRepository;
import intraer.fablegis.infrastructure.repositories.HabilitacaoDaOmRepository;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Quem pode CRIAR documentos de cada espécie (docs/autenticacao.md, "Habilitação de documentos"): a espécie convencional só
// pela OM habilitada pelo administrador; a NPA, por qualquer OM. Consultar e ser coautor não passam por aqui.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HabilitacaoDeCriacaoServiceTest {

    private static final long OM_HABILITADA = 1L;
    private static final long OM_COMUM = 2L;

    @Mock HabilitacaoDaOmRepository habilitacaoRepository;
    @Mock OrganizacaoMilitarRepository omRepository;
    @Mock EspecieNormativaRepository especieRepository;
    @Mock RegrasDasEspecies regras;

    HabilitacaoDeCriacaoService service;
    EspecieNormativa dca;
    EspecieNormativa ica;
    EspecieNormativa npa;

    @BeforeEach
    void preparar() {
        dca = especie(10, "DCA", TipoDeEspecie.CONVENCIONAL);
        ica = especie(11, "ICA", TipoDeEspecie.CONVENCIONAL);
        npa = especie(20, "NPA", TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA);
        regrasDeHabilitacao();
        when(especieRepository.findAll()).thenReturn(List.of(npa, ica, dca));
        when(habilitacaoRepository.existsByOmIdAndEspecieId(OM_HABILITADA, dca.getId())).thenReturn(true);
        when(habilitacaoRepository.findByOmId(OM_HABILITADA)).thenReturn(List.of(new HabilitacaoDaOm(OM_HABILITADA, dca.getId())));
        service = new HabilitacaoDeCriacaoService(habilitacaoRepository, omRepository, especieRepository, regras);
    }

    private static EspecieNormativa especie(long id, String sigla, TipoDeEspecie tipo) {
        var e = new EspecieNormativa();
        e.setId(id);
        e.setSigla(sigla);
        e.setNome(sigla);
        e.setDescricao(sigla);
        e.setTipoDeEspecie(tipo);
        return e;
    }

    // A espécie convencional exige habilitação da OM; a comunicação oficial padronizada (NPA), não -- como as regras reais.
    private void regrasDeHabilitacao() {
        var convencional = mock(RegrasDaEspecieNormativa.class);
        var comunicacao = mock(RegrasDaEspecieNormativa.class);
        RegraDeHabilitacaoDaOm exige = () -> true;
        RegraDeHabilitacaoDaOm naoExige = () -> false;
        when(convencional.habilitacao()).thenReturn(exige);
        when(comunicacao.habilitacao()).thenReturn(naoExige);
        when(regras.para(any(EspecieNormativa.class))).thenAnswer(inv ->
                ((EspecieNormativa) inv.getArgument(0)).getTipoDeEspecie() == TipoDeEspecie.CONVENCIONAL ? convencional : comunicacao);
    }

    // ─── Criar ───────────────────────────────────────────────────────────────

    @Test
    void aOmHabilitadaCriaAEspecieConvencionalParaAQualEstaHabilitada() {
        assertThat(service.podeCriar(OM_HABILITADA, dca)).isTrue();
    }

    @Test
    void aOmHabilitadaNaoCriaUmaEspecieConvencionalParaAQualNaoEstaHabilitada() {
        assertThat(service.podeCriar(OM_HABILITADA, ica)).isFalse();
    }

    @Test
    void umaOmSemHabilitacaoNaoCriaEspecieConvencional() {
        assertThat(service.podeCriar(OM_COMUM, dca)).isFalse();
        assertThat(service.podeCriar(null, dca)).isFalse();
    }

    @Test
    void qualquerOmCriaNpa() {
        assertThat(service.podeCriar(OM_COMUM, npa)).isTrue();
        assertThat(service.podeCriar(null, npa)).isTrue();
    }

    @Test
    void criarSemHabilitacaoEhNegadoComAMensagemQueMandaPedirAoAdministrador() {
        assertThatThrownBy(() -> service.exigirPodeCriar(OM_COMUM, dca))
                .isInstanceOf(OperacaoNaoAutorizadaException.class)
                .hasMessageContaining("DCA").hasMessageContaining("CENDOC");
        service.exigirPodeCriar(OM_HABILITADA, dca);   // habilitada: não lança
        service.exigirPodeCriar(OM_COMUM, npa);        // NPA: nunca lança
    }

    // ─── O que cada OM pode criar (a tela esconde o resto) ───────────────────

    @Test
    void umaOmSemHabilitacaoSoPodeCriarAsEspeciesQueNaoDependemDeHabilitacao() {
        var criaveis = service.especiesQuePodeCriar(OM_COMUM);

        assertThat(criaveis).extracting("sigla").containsExactly("NPA");
    }

    @Test
    void aOmHabilitadaPodeCriarAsSuasEspeciesMaisANpa() {
        var criaveis = service.especiesQuePodeCriar(OM_HABILITADA);

        assertThat(criaveis).extracting("sigla").containsExactly("DCA", "NPA");   // por sigla
        assertThat(criaveis).extracting("tipoDeEspecie").contains("CONVENCIONAL", "COMUNICACAO_OFICIAL_PADRONIZADA");
    }

    // ─── Gestão pelo administrador ───────────────────────────────────────────

    private OrganizacaoMilitar om(long id, String sigla) {
        var o = new OrganizacaoMilitar();
        o.setId(id);
        o.setSigla(sigla);
        o.setNome("OM " + sigla);
        return o;
    }

    @Test
    void definirSubstituiAsHabilitacoesDaOm() {
        when(omRepository.findById(OM_COMUM)).thenReturn(Optional.of(om(OM_COMUM, "EMAER")));
        when(especieRepository.findById(10L)).thenReturn(Optional.of(dca));
        when(especieRepository.findById(11L)).thenReturn(Optional.of(ica));

        var resposta = service.definir(OM_COMUM, List.of(11L, 10L, 10L));

        verify(habilitacaoRepository).deleteByOmId(OM_COMUM);
        var salvas = ArgumentCaptor.forClass(HabilitacaoDaOm.class);
        verify(habilitacaoRepository, org.mockito.Mockito.times(2)).save(salvas.capture());
        assertThat(salvas.getAllValues()).extracting(HabilitacaoDaOm::getEspecieNormativaId).containsExactlyInAnyOrder(10L, 11L);
        assertThat(resposta.especieIds()).containsExactly(10L, 11L);
    }

    @Test
    void definirListaVaziaTiraTodasAsHabilitacoes() {
        when(omRepository.findById(OM_HABILITADA)).thenReturn(Optional.of(om(OM_HABILITADA, "GABAER")));

        var resposta = service.definir(OM_HABILITADA, List.of());

        verify(habilitacaoRepository).deleteByOmId(OM_HABILITADA);
        verify(habilitacaoRepository, never()).save(any());
        assertThat(resposta.especieIds()).isEmpty();
    }

    @Test
    void naoSeHabilitaUmaEspecieQueNaoDependeDeHabilitacao() {
        when(omRepository.findById(OM_COMUM)).thenReturn(Optional.of(om(OM_COMUM, "EMAER")));
        when(especieRepository.findById(20L)).thenReturn(Optional.of(npa));

        assertThatThrownBy(() -> service.definir(OM_COMUM, List.of(20L)))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("NPA");
        verify(habilitacaoRepository, never()).deleteByOmId(any());
    }

    @Test
    void omOuEspecieInexistenteNaoSaoHabilitadas() {
        when(omRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.definir(99L, List.of(10L))).isInstanceOf(ResourceNotFoundException.class);

        when(omRepository.findById(OM_COMUM)).thenReturn(Optional.of(om(OM_COMUM, "EMAER")));
        when(especieRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.definir(OM_COMUM, List.of(404L))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarMostraCadaOmComAsSuasEspecies() {
        when(omRepository.findAll()).thenReturn(List.of(om(OM_COMUM, "EMAER"), om(OM_HABILITADA, "CENDOC")));
        when(habilitacaoRepository.findAll()).thenReturn(List.of(
                new HabilitacaoDaOm(OM_HABILITADA, 11L), new HabilitacaoDaOm(OM_HABILITADA, 10L)));

        var lista = service.listar();

        assertThat(lista).extracting("sigla").containsExactly("CENDOC", "EMAER");   // ordenadas por sigla
        assertThat(lista.get(0).especieIds()).containsExactly(10L, 11L);
        assertThat(lista.get(1).especieIds()).isEmpty();
    }
}
