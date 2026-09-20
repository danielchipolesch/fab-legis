package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.regras.ElementoNumeracao;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Estrutura com que uma NPA nasce (layout do Anexo XII da NSCA 5-3): 1 DISPOSIÇÕES PRELIMINARES (1.1 Finalidade,
// 1.2 Âmbito, 1.3 Referências), 2 DISPOSIÇÕES GERAIS (2.1 Conceituações), 3 DISPOSIÇÕES FINAIS (3.1, parágrafo
// direto). A alínea só existe depois de um parágrafo -- por isso Referências nasce com "Constituem referências:".
class EstruturaInicialDeNpaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<ItemAnexoParteNormativa> salvos = new ArrayList<>();
    private EstruturaInicialDeNpa estrutura;

    @BeforeEach
    void preparar() {
        var ids = new AtomicLong(1);
        var repositorio = Mockito.mock(ItemAnexoParteNormativaRepository.class);
        when(repositorio.save(any())).thenAnswer(inv -> {
            ItemAnexoParteNormativa item = inv.getArgument(0);
            item.setId(ids.getAndIncrement());
            salvos.add(item);
            return item;
        });
        estrutura = new EstruturaInicialDeNpa(repositorio, MAPPER);
        estrutura.criarEm(new Documento());
    }

    private ItemAnexoParteNormativaResponseDto dto(ItemAnexoParteNormativa item) {
        var filhos = salvos.stream().filter(s -> s.getParent() == item).map(this::dto).toList();
        return new ItemAnexoParteNormativaResponseDto(item.getId(), null, item.getTipo(), item.getElementOrder(),
                item.getTitulo(), item.getConteudo(), item.getFullTextContent(), ElementoEmendaStatusEnum.INALTERADO,
                null, null, null, null, null, null, false, null, null, filhos);
    }

    private List<ItemAnexoParteNormativaResponseDto> arvore() {
        return salvos.stream().filter(s -> s.getParent() == null).map(this::dto).toList();
    }

    private ItemAnexoParteNormativa porTitulo(String titulo) {
        return salvos.stream().filter(s -> titulo.equals(s.getTitulo())).findFirst().orElseThrow();
    }

    @Test
    void nasceComOsTresCapitulosNaOrdemDoLayout() {
        var capitulos = salvos.stream().filter(s -> s.getTipo() == CAPITULO).toList();

        assertThat(capitulos).extracting(ItemAnexoParteNormativa::getTitulo)
                .containsExactly("DISPOSIÇÕES PRELIMINARES", "DISPOSIÇÕES GERAIS", "DISPOSIÇÕES FINAIS");
        assertThat(capitulos).extracting(ItemAnexoParteNormativa::getElementOrder).containsExactly(1, 2, 3);
    }

    @Test
    void osPreliminaresTemFinalidadeAmbitoEReferencias() {
        var secoes = salvos.stream().filter(s -> s.getParent() == porTitulo("DISPOSIÇÕES PRELIMINARES")).toList();

        assertThat(secoes).extracting(ItemAnexoParteNormativa::getTitulo)
                .containsExactly("Finalidade", "Âmbito", "Referências");
        assertThat(secoes).extracting(ItemAnexoParteNormativa::getTipo).containsOnly(SECAO_NORMATIVA);
    }

    @Test
    void asDisposicoesGeraisTemAConceituacoes() {
        var secoes = salvos.stream().filter(s -> s.getParent() == porTitulo("DISPOSIÇÕES GERAIS")).toList();

        assertThat(secoes).extracting(ItemAnexoParteNormativa::getTitulo).containsExactly("Conceituações");
    }

    @Test
    void asDisposicoesFinaisTemUmParagrafoDireto() {
        var filhos = salvos.stream().filter(s -> s.getParent() == porTitulo("DISPOSIÇÕES FINAIS")).toList();

        assertThat(filhos).hasSize(1);
        assertThat(filhos.get(0).getTipo()).isEqualTo(PARAGRAFO);
    }

    @Test
    void aAlineaDeReferenciasFicaSobUmParagrafoIntrodutorio() {
        var referencias = porTitulo("Referências");
        var filhos = salvos.stream().filter(s -> s.getParent() == referencias).toList();

        assertThat(filhos).hasSize(1);
        assertThat(filhos.get(0).getTipo()).isEqualTo(PARAGRAFO);
        assertThat(filhos.get(0).getFullTextContent()).isEqualTo("Constituem referências:");

        var alineas = salvos.stream().filter(s -> s.getParent() == filhos.get(0)).toList();
        assertThat(alineas).hasSize(1);
        assertThat(alineas.get(0).getTipo()).isEqualTo(ALINEA);
    }

    @Test
    void naoHaArtigoNemOutroTipoForaDaGramaticaDaNpa() {
        assertThat(salvos).extracting(ItemAnexoParteNormativa::getTipo)
                .containsOnly(CAPITULO, SECAO_NORMATIVA, PARAGRAFO, ALINEA);
    }

    @Test
    void aEstruturaRespeitaAHierarquiaDaNpa() {
        var hierarquia = new HierarquiaDeNpa();

        for (var item : salvos) {
            var pai = item.getParent() != null ? item.getParent().getTipo() : null;
            assertThat(hierarquia.permite(pai, item.getTipo()))
                    .as("%s sob %s", item.getTipo(), pai).isTrue();
        }
    }

    @Test
    void aNumeracaoDaEstruturaInicialSegueOLayout() {
        Map<Long, ElementoNumeracao> numeracao = new NumeracaoDeNpa().calcular(arvore());

        assertThat(numeracao.get(porTitulo("DISPOSIÇÕES PRELIMINARES").getId()).label()).isEqualTo("1");
        assertThat(numeracao.get(porTitulo("Finalidade").getId()).label()).isEqualTo("1.1");
        assertThat(numeracao.get(porTitulo("Âmbito").getId()).label()).isEqualTo("1.2");
        assertThat(numeracao.get(porTitulo("Referências").getId()).label()).isEqualTo("1.3");
        assertThat(numeracao.get(porTitulo("DISPOSIÇÕES GERAIS").getId()).label()).isEqualTo("2");
        assertThat(numeracao.get(porTitulo("Conceituações").getId()).label()).isEqualTo("2.1");
        assertThat(numeracao.get(porTitulo("DISPOSIÇÕES FINAIS").getId()).label()).isEqualTo("3");

        var disposicaoFinal = salvos.stream().filter(s -> s.getParent() == porTitulo("DISPOSIÇÕES FINAIS")).findFirst().orElseThrow();
        assertThat(numeracao.get(disposicaoFinal.getId()).label()).isEqualTo("3.1");
    }

    @Test
    void oTextoDeOrientacaoEntreColchetesNasceEmVermelho() {
        var finalidade = salvos.stream().filter(s -> s.getParent() == porTitulo("Finalidade")).findFirst().orElseThrow();

        var trecho = MAPPER.readTree(finalidade.getConteudo()).get("content").get(0).get("content").get(0);

        assertThat(trecho.get("text").asString()).isEqualTo("[Descrever a finalidade desta NPA.]");
        assertThat(trecho.get("marks").get(0).get("attrs").get("color").asString()).isEqualTo("#FF0000");
    }

    @Test
    void oTextoSemColchetesNaoTemCor() {
        var intro = salvos.stream().filter(s -> "Constituem referências:".equals(s.getFullTextContent())).findFirst().orElseThrow();

        var trecho = MAPPER.readTree(intro.getConteudo()).get("content").get(0).get("content").get(0);

        assertThat(trecho.get("marks")).isNull();
    }
}
