package intraer.fablegis.infrastructure.runners;

import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.domain.entities.usuario.HabilitacaoDaOm;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// De saída, só o GABAER e o CENDOC criam as espécies convencionais (todas); o resto é do administrador. E só no primeiro boot.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HabilitacaoPadraoRunnerTest {

    @Mock HabilitacaoDaOmRepository habilitacaoRepository;
    @Mock OrganizacaoMilitarRepository omRepository;
    @Mock EspecieNormativaRepository especieRepository;
    @Mock RegrasDasEspecies regras;

    HabilitacaoPadraoRunner runner;

    private static EspecieNormativa especie(long id, String sigla, TipoDeEspecie tipo) {
        var e = new EspecieNormativa();
        e.setId(id);
        e.setSigla(sigla);
        e.setTipoDeEspecie(tipo);
        return e;
    }

    private static OrganizacaoMilitar om(long id, String sigla) {
        var o = new OrganizacaoMilitar();
        o.setId(id);
        o.setSigla(sigla);
        return o;
    }

    @BeforeEach
    void preparar() {
        var convencional = mock(RegrasDaEspecieNormativa.class);
        var comunicacao = mock(RegrasDaEspecieNormativa.class);
        when(convencional.habilitacao()).thenReturn(() -> true);
        when(comunicacao.habilitacao()).thenReturn(() -> false);
        when(regras.para(any(EspecieNormativa.class))).thenAnswer(inv ->
                ((EspecieNormativa) inv.getArgument(0)).getTipoDeEspecie() == TipoDeEspecie.CONVENCIONAL ? convencional : comunicacao);
        when(especieRepository.findAll()).thenReturn(List.of(
                especie(1, "DCA", TipoDeEspecie.CONVENCIONAL), especie(2, "ICA", TipoDeEspecie.CONVENCIONAL),
                especie(3, "NPA", TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA)));
        when(omRepository.findBySigla("GABAER")).thenReturn(Optional.of(om(10, "GABAER")));
        when(omRepository.findBySigla("CENDOC")).thenReturn(Optional.of(om(20, "CENDOC")));
        runner = new HabilitacaoPadraoRunner(habilitacaoRepository, omRepository, especieRepository, regras);
    }

    @Test
    void noPrimeiroBootGabaerECendocRecebemTodasAsEspeciesConvencionaisEMaisNinguem() {
        when(habilitacaoRepository.count()).thenReturn(0L);

        runner.run();

        var salvas = ArgumentCaptor.forClass(HabilitacaoDaOm.class);
        verify(habilitacaoRepository, times(4)).save(salvas.capture());   // 2 OMs x 2 espécies convencionais
        assertThat(salvas.getAllValues())
                .extracting(h -> h.getOrganizacaoMilitarId() + ":" + h.getEspecieNormativaId())
                .containsExactlyInAnyOrder("10:1", "10:2", "20:1", "20:2");   // a NPA (id 3) não entra: toda OM a cria
    }

    @Test
    void depoisDoPrimeiroBootATabelaEDoAdministradorENaoSeRefaz() {
        when(habilitacaoRepository.count()).thenReturn(7L);

        runner.run();

        verify(habilitacaoRepository, never()).save(any());
    }

    @Test
    void umaOmPadraoAusenteNaoImpedeAsOutras() {
        when(habilitacaoRepository.count()).thenReturn(0L);
        when(omRepository.findBySigla("GABAER")).thenReturn(Optional.empty());

        runner.run();

        verify(habilitacaoRepository, times(2)).save(any());   // só o CENDOC
    }
}
