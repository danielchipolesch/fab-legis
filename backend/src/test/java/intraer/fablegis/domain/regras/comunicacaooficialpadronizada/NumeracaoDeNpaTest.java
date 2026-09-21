package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import intraer.fablegis.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;
import static org.assertj.core.api.Assertions.assertThat;

// Numeração da NPA (docs/dominio.md, "NPA"): algarismo arábico pelo CAMINHO do elemento -- capítulo 1, seção 1.1,
// subseção 1.1.1, parágrafo 1.1.1.1 (direto sob capítulo, seção ou subseção), alínea a). Seção, subseção e parágrafo
// do mesmo pai dividem uma única sequência. Todo elemento é numerado.
class NumeracaoDeNpaTest {

    private static final AtomicLong IDS = new AtomicLong(1);

    private final NumeracaoDeNpa numeracao = new NumeracaoDeNpa();

    private static ItemAnexoParteNormativaResponseDto no(ItemAnexoParteNormativaTipoEnum tipo,
                                                         ItemAnexoParteNormativaResponseDto... filhos) {
        return new ItemAnexoParteNormativaResponseDto(IDS.getAndIncrement(), null, tipo, 1, null, null, null,
                ElementoEmendaStatusEnum.INALTERADO, null, null, null, null, null, null, false, null, null,
                List.of(filhos));
    }

    private String rotulo(java.util.Map<Long, ?> mapa, ItemAnexoParteNormativaResponseDto item) {
        var e = (intraer.fablegis.domain.regras.ElementoNumeracao) mapa.get(item.id());
        return e == null ? null : e.label();
    }

    @Test
    void osCapitulosSaoNumeradosComAlgarismoArabicoEmSequencia() {
        var c1 = no(CAPITULO);
        var c2 = no(CAPITULO);
        var c3 = no(CAPITULO);

        var r = numeracao.calcular(List.of(c1, c2, c3));

        assertThat(rotulo(r, c1)).isEqualTo("1");
        assertThat(rotulo(r, c2)).isEqualTo("2");
        assertThat(rotulo(r, c3)).isEqualTo("3");
    }

    @Test
    void secaoSubsecaoEParagrafoSeguemOCaminhoDoPai() {
        var paragrafo = no(PARAGRAFO);
        var subsecao = no(SUBSECAO_NORMATIVA, paragrafo);
        var secao = no(SECAO_NORMATIVA, subsecao);
        var capitulo = no(CAPITULO, secao);

        var r = numeracao.calcular(List.of(capitulo));

        assertThat(rotulo(r, capitulo)).isEqualTo("1");
        assertThat(rotulo(r, secao)).isEqualTo("1.1");
        assertThat(rotulo(r, subsecao)).isEqualTo("1.1.1");
        assertThat(rotulo(r, paragrafo)).isEqualTo("1.1.1.1");
    }

    @Test
    void oParagrafoPodeFicarDiretoSobOCapitulo() {
        var paragrafo = no(PARAGRAFO);
        var capitulo = no(CAPITULO);
        var terceiro = no(CAPITULO, paragrafo);

        var r = numeracao.calcular(List.of(capitulo, no(CAPITULO), terceiro));

        assertThat(rotulo(r, paragrafo)).isEqualTo("3.1");
    }

    @Test
    void oParagrafoPodeFicarDiretoSobUmaSecao() {
        var paragrafo = no(PARAGRAFO);
        var secao = no(SECAO_NORMATIVA, paragrafo);
        var capitulo = no(CAPITULO, no(SECAO_NORMATIVA), secao);

        var r = numeracao.calcular(List.of(no(CAPITULO), capitulo));

        assertThat(rotulo(r, secao)).isEqualTo("2.2");
        assertThat(rotulo(r, paragrafo)).isEqualTo("2.2.1");
    }

    @Test
    void secaoEParagrafoDoMesmoPaiDividemUmaUnicaSequencia() {
        var secao1 = no(SECAO_NORMATIVA);
        var paragrafo = no(PARAGRAFO);
        var secao2 = no(SECAO_NORMATIVA);
        var capitulo = no(CAPITULO, secao1, paragrafo, secao2);

        var r = numeracao.calcular(List.of(capitulo));

        assertThat(rotulo(r, secao1)).isEqualTo("1.1");
        assertThat(rotulo(r, paragrafo)).isEqualTo("1.2");
        assertThat(rotulo(r, secao2)).isEqualTo("1.3");
    }

    @Test
    void subsecaoEParagrafoDeUmaSecaoDividemUmaUnicaSequencia() {
        var paragrafo1 = no(PARAGRAFO);
        var subsecao = no(SUBSECAO_NORMATIVA);
        var paragrafo2 = no(PARAGRAFO);
        var secao = no(SECAO_NORMATIVA, paragrafo1, subsecao, paragrafo2);

        var r = numeracao.calcular(List.of(no(CAPITULO, secao)));

        assertThat(rotulo(r, paragrafo1)).isEqualTo("1.1.1");
        assertThat(rotulo(r, subsecao)).isEqualTo("1.1.2");
        assertThat(rotulo(r, paragrafo2)).isEqualTo("1.1.3");
    }

    @Test
    void asAlineasDeUmParagrafoSaoLetradasPelaPosicao() {
        var a = no(ALINEA);
        var b = no(ALINEA);
        var c = no(ALINEA);
        var paragrafo = no(PARAGRAFO, a, b, c);

        var r = numeracao.calcular(List.of(no(CAPITULO, no(SECAO_NORMATIVA, paragrafo))));

        assertThat(rotulo(r, a)).isEqualTo("a)");
        assertThat(rotulo(r, b)).isEqualTo("b)");
        assertThat(rotulo(r, c)).isEqualTo("c)");
    }

    @Test
    void asAlineasNaoConsomemANumeracaoDosIrmaosDoParagrafo() {
        var paragrafo1 = no(PARAGRAFO, no(ALINEA), no(ALINEA));
        var paragrafo2 = no(PARAGRAFO, no(ALINEA));
        var secao = no(SECAO_NORMATIVA, paragrafo1, paragrafo2);

        var r = numeracao.calcular(List.of(no(CAPITULO, secao)));

        assertThat(rotulo(r, paragrafo1)).isEqualTo("1.1.1");
        assertThat(rotulo(r, paragrafo2)).isEqualTo("1.1.2");
        assertThat(rotulo(r, paragrafo2.children().get(0))).isEqualTo("a)");
    }

    @Test
    void aNumeracaoDeUmCapituloNaoAfetaAOutro() {
        var s1 = no(SECAO_NORMATIVA);
        var s2 = no(SECAO_NORMATIVA);
        var capitulo1 = no(CAPITULO, no(SECAO_NORMATIVA), s1);
        var capitulo2 = no(CAPITULO, s2);

        var r = numeracao.calcular(List.of(capitulo1, capitulo2));

        assertThat(rotulo(r, s1)).isEqualTo("1.2");
        assertThat(rotulo(r, s2)).isEqualTo("2.1");
    }

    @Test
    void todoElementoRecebeNumeroEONumeroCorrespondeAPosicao() {
        var secao = no(SECAO_NORMATIVA);
        var paragrafo = no(PARAGRAFO);
        var capitulo = no(CAPITULO, secao, paragrafo);

        var r = numeracao.calcular(List.of(capitulo));

        assertThat(r).hasSize(3);
        assertThat(r.get(paragrafo.id()).numero()).isEqualTo(2);
        assertThat(r.get(paragrafo.id()).letra()).isNull();
        assertThat(r.get(paragrafo.id()).semNumero()).isFalse();
    }

    @Test
    void naoHaSufixoDeLetraNemEmendaMesmoParaElementoMarcadoComoIncluido() {
        // A NPA não tem alteração: mesmo que o dado traga a marca de emenda, ela não muda o número.
        var incluido = new ItemAnexoParteNormativaResponseDto(IDS.getAndIncrement(), null, PARAGRAFO, 1, null, null, null,
                ElementoEmendaStatusEnum.INCLUIDO, null, null, null, null, null, null, true, null, null, List.of());
        var posterior = no(PARAGRAFO);

        var r = numeracao.calcular(List.of(no(CAPITULO, incluido, posterior)));

        assertThat(rotulo(r, incluido)).isEqualTo("1.1");
        assertThat(rotulo(r, posterior)).isEqualTo("1.2");
    }

    @Test
    void letraDaAlineaContinuaDepoisDoZ() {
        assertThat(NumeracaoDeNpa.letra(1)).isEqualTo("a");
        assertThat(NumeracaoDeNpa.letra(26)).isEqualTo("z");
        assertThat(NumeracaoDeNpa.letra(27)).isEqualTo("aa");
        assertThat(NumeracaoDeNpa.letra(28)).isEqualTo("ab");
    }
}
