package br.com.danielchipolesch.domain.regimes;

import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.regimes.atonormativo.AtoNormativo;
import br.com.danielchipolesch.domain.regimes.atonormativo.CicloDeVidaDeAtoNormativo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// O registro que entrega as regras de uma espécie (docs/arquitetura.md, "Regimes normativos"): o restante
// do sistema nunca testa a espécie do documento, só pede a regra ao regime.
class RegimesNormativosTest {

    private static final AtoNormativo ATO = new AtoNormativo(null, null, null, null, null, new CicloDeVidaDeAtoNormativo());

    @Test
    void aEspecieRecebeORegimeDeAtoNormativoPorPadrao() {
        var especie = new EspecieNormativa();

        assertThat(especie.getRegime()).isEqualTo(RegimeNormativo.ATO_NORMATIVO);
        assertThat(new RegimesNormativos(List.of(ATO)).para(especie)).isSameAs(ATO);
    }

    @Test
    void regimeNuloCaiNoDeAtoNormativo() {
        assertThat(new RegimesNormativos(List.of(ATO)).para((RegimeNormativo) null)).isSameAs(ATO);
    }

    @Test
    void entregaAsRegrasDoRegimeDaEspecie() {
        var regime = new RegimesNormativos(List.of(ATO)).para(RegimeNormativo.ATO_NORMATIVO);

        assertThat(regime.regime()).isEqualTo(RegimeNormativo.ATO_NORMATIVO);
        assertThat(regime.cicloDeVida()).isInstanceOf(CicloDeVidaDeAtoNormativo.class);
    }

    @Test
    void semNenhumRegimeRegistradoFalhaComMensagemClara() {
        var vazio = new RegimesNormativos(List.of());

        assertThatThrownBy(() -> vazio.para(RegimeNormativo.ATO_NORMATIVO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ATO_NORMATIVO");
    }
}
