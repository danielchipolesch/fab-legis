package br.com.danielchipolesch.domain.regras;

import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.regras.atonormativo.CicloDeVidaDeAtoNormativo;
import br.com.danielchipolesch.domain.regras.atonormativo.RegrasDeAtoNormativo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// O registro que entrega as regras de uma espécie (docs/arquitetura.md, "Regras por espécie normativa"): o
// restante do sistema nunca testa a espécie do documento, só pede a regra a quem a conhece.
class RegrasDasEspeciesTest {

    private static final RegrasDeAtoNormativo ATO = new RegrasDeAtoNormativo(null, null, null, null, null, null, null, new CicloDeVidaDeAtoNormativo());

    @Test
    void aEspecieSegueAsRegrasDeAtoNormativoPorPadrao() {
        var especie = new EspecieNormativa();

        assertThat(especie.getTipoDeRegras()).isEqualTo(TipoDeRegras.ATO_NORMATIVO);
        assertThat(new RegrasDasEspecies(List.of(ATO)).para(especie)).isSameAs(ATO);
    }

    @Test
    void tipoNuloCaiNasRegrasDeAtoNormativo() {
        assertThat(new RegrasDasEspecies(List.of(ATO)).para((TipoDeRegras) null)).isSameAs(ATO);
    }

    @Test
    void entregaAsRegrasDoTipoDaEspecie() {
        var regras = new RegrasDasEspecies(List.of(ATO)).para(TipoDeRegras.ATO_NORMATIVO);

        assertThat(regras.tipo()).isEqualTo(TipoDeRegras.ATO_NORMATIVO);
        assertThat(regras.cicloDeVida()).isInstanceOf(CicloDeVidaDeAtoNormativo.class);
    }

    @Test
    void semNenhumaRegraRegistradaFalhaComMensagemClara() {
        var vazio = new RegrasDasEspecies(List.of());

        assertThatThrownBy(() -> vazio.para(TipoDeRegras.ATO_NORMATIVO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ATO_NORMATIVO");
    }
}
