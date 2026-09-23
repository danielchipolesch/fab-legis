package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.application.dtos.itemAnexoParteNormativaDtos.SecaoItemRequestDto;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Gramática de elementos da NPA: capítulo na raiz; seção sob capítulo; subseção sob seção; parágrafo sob capítulo,
// seção ou subseção; alínea só sob parágrafo. Sem artigo, inciso, parágrafo único nem subalínea.
class HierarquiaDeNpaTest {

    private final HierarquiaDeNpa hierarquia = new HierarquiaDeNpa();

    private static SecaoItemRequestDto item(ItemAnexoParteNormativaTipoEnum tipo, SecaoItemRequestDto... filhos) {
        return new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_NORMATIVA, tipo, 1, null, null, null, List.of(filhos));
    }

    @Test
    void naRaizSoCabeCapitulo() {
        assertThat(hierarquia.filhosPermitidos(null)).containsExactly(CAPITULO);
        assertThat(hierarquia.permite(null, CAPITULO)).isTrue();
        assertThat(hierarquia.permite(null, PARAGRAFO)).isFalse();
        assertThat(hierarquia.permite(null, SECAO_NORMATIVA)).isFalse();
    }

    @Test
    void sobOCapituloCabemSecaoEParagrafo() {
        assertThat(hierarquia.filhosPermitidos(CAPITULO)).containsExactly(SECAO_NORMATIVA, PARAGRAFO);
        assertThat(hierarquia.permite(CAPITULO, SUBSECAO_NORMATIVA)).isFalse();
        assertThat(hierarquia.permite(CAPITULO, ALINEA)).isFalse();
    }

    @Test
    void sobASecaoCabemSubsecaoEParagrafo() {
        assertThat(hierarquia.filhosPermitidos(SECAO_NORMATIVA)).containsExactly(SUBSECAO_NORMATIVA, PARAGRAFO);
        assertThat(hierarquia.permite(SECAO_NORMATIVA, SECAO_NORMATIVA)).isFalse();
    }

    @Test
    void sobASubsecaoSoCabeParagrafo() {
        assertThat(hierarquia.filhosPermitidos(SUBSECAO_NORMATIVA)).containsExactly(PARAGRAFO);
    }

    @Test
    void aAlineaSoExisteSobUmParagrafo() {
        assertThat(hierarquia.permite(PARAGRAFO, ALINEA)).isTrue();
        assertThat(hierarquia.permite(CAPITULO, ALINEA)).isFalse();
        assertThat(hierarquia.permite(SECAO_NORMATIVA, ALINEA)).isFalse();
        assertThat(hierarquia.permite(SUBSECAO_NORMATIVA, ALINEA)).isFalse();
        assertThat(hierarquia.permite(null, ALINEA)).isFalse();
    }

    @Test
    void aAlineaNaoTemFilhos() {
        assertThat(hierarquia.filhosPermitidos(ALINEA)).isEmpty();
    }

    @Test
    void tiposDoAtoNormativoNaoExistemNaNpa() {
        for (var pai : List.of(CAPITULO, SECAO_NORMATIVA, SUBSECAO_NORMATIVA, PARAGRAFO)) {
            assertThat(hierarquia.permite(pai, ARTIGO)).isFalse();
            assertThat(hierarquia.permite(pai, INCISO)).isFalse();
            assertThat(hierarquia.permite(pai, PARAGRAFO_UNICO)).isFalse();
            assertThat(hierarquia.permite(pai, SUB_ALINEA)).isFalse();
        }
    }

    @Test
    void validaUmaArvoreCompletaPermitida() {
        var arvore = List.of(
                item(CAPITULO,
                        item(SECAO_NORMATIVA,
                                item(SUBSECAO_NORMATIVA, item(PARAGRAFO, item(ALINEA))),
                                item(PARAGRAFO)),
                        item(PARAGRAFO)));

        assertThatCode(() -> hierarquia.validar(arvore)).doesNotThrowAnyException();
    }

    @Test
    void recusaArtigoNaRaiz() {
        assertThatThrownBy(() -> hierarquia.validar(List.of(item(ARTIGO))))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("ARTIGO");
    }

    @Test
    void recusaAlineaForaDeParagrafoMesmoEmProfundidade() {
        var arvore = List.of(item(CAPITULO, item(SECAO_NORMATIVA, item(ALINEA))));

        assertThatThrownBy(() -> hierarquia.validar(arvore))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("ALINEA")
                .hasMessageContaining("SECAO_NORMATIVA");
    }

    @Test
    void recusaSecaoDentroDeSecao() {
        var arvore = List.of(item(CAPITULO, item(SECAO_NORMATIVA, item(SECAO_NORMATIVA))));

        assertThatThrownBy(() -> hierarquia.validar(arvore)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void arvoreSemFilhosOuNulaEValida() {
        assertThatCode(() -> hierarquia.validar(List.of(new SecaoItemRequestDto(
                null, SecaoDocumentoEnum.PARTE_NORMATIVA, CAPITULO, 1, "T", null, null, null)))).doesNotThrowAnyException();
    }
}
