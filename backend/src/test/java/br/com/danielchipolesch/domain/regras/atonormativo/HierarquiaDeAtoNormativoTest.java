package br.com.danielchipolesch.domain.regras.atonormativo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;
import static org.assertj.core.api.Assertions.assertThat;

// Hierarquia de um ato normativo (LC 95/1998, art. 10): capítulo > seção > subseção > artigo > parágrafo/inciso >
// alínea > subalínea. O menu "adicionar elemento" oferece só o que cabe em cada nível.
class HierarquiaDeAtoNormativoTest {

    private final HierarquiaDeAtoNormativo hierarquia = new HierarquiaDeAtoNormativo();

    @Test
    void naRaizCabemCapituloEArtigo() {
        assertThat(hierarquia.filhosPermitidos(null)).containsExactly(CAPITULO, ARTIGO);
    }

    @Test
    void oArtigoPodeTerParagrafoEInciso() {
        assertThat(hierarquia.filhosPermitidos(ARTIGO)).containsExactly(PARAGRAFO, PARAGRAFO_UNICO, INCISO);
    }

    @Test
    void oIncisoPodeTerAlineaEAAlineaPodeTerSubalinea() {
        assertThat(hierarquia.filhosPermitidos(INCISO)).containsExactly(ALINEA);
        assertThat(hierarquia.filhosPermitidos(ALINEA)).containsExactly(SUB_ALINEA);
        assertThat(hierarquia.filhosPermitidos(SUB_ALINEA)).isEqualTo(List.of());
    }

    @Test
    void oBackendNaoRecusaCombinacoesDeUmAtoNormativo() {
        // Quem impõe a ordem é o editor; documentos existentes e a edição em massa dependem dessa flexibilidade.
        assertThat(hierarquia.permite(ARTIGO, ARTIGO)).isTrue();
    }
}
