package intraer.fablegis.domain.regras.convencional;

import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// "Substituir" é só da NPA: um ato normativo publicado se muda pelo ciclo de emenda (docs/ciclo-de-vida.md).
class CicloDeVidaDeEspecieConvencionalSubstituicaoTest {

    @Test
    void umaEspecieConvencionalNuncaESubstituida() {
        var ciclo = new CicloDeVidaDeEspecieConvencional();
        for (var local : SituacaoLocalEnum.values()) {
            for (var bca : SituacaoBcaEnum.values()) {
                assertThat(ciclo.podeSerSubstituido(local, bca)).as(local + "/" + bca).isFalse();
            }
        }
    }
}
