package intraer.fablegis.domain.regras;

import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.domain.regras.convencional.CicloDeVidaDeEspecieConvencional;
import intraer.fablegis.domain.regras.convencional.RegrasDeEspecieConvencional;
import intraer.fablegis.domain.regras.comunicacaooficialpadronizada.RegrasDeComunicacaoOficialPadronizada;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// O registro que entrega as regras de uma espécie (docs/arquitetura.md, "Regras por espécie normativa"): o
// restante do sistema nunca testa a espécie do documento, só pede a regra a quem a conhece.
class RegrasDasEspeciesTest {

    private static final RegrasDeEspecieConvencional ATO = new RegrasDeEspecieConvencional(null, null, null, null, null, null, null, null, null, new CicloDeVidaDeEspecieConvencional());

    @Test
    void aEspecieSegueAsRegrasDeEspecieConvencionalPorPadrao() {
        var especie = new EspecieNormativa();

        assertThat(especie.getTipoDeEspecie()).isEqualTo(TipoDeEspecie.CONVENCIONAL);
        assertThat(new RegrasDasEspecies(List.of(ATO)).para(especie)).isSameAs(ATO);
    }

    @Test
    void tipoNuloCaiNasRegrasDeEspecieConvencional() {
        assertThat(new RegrasDasEspecies(List.of(ATO)).para((TipoDeEspecie) null)).isSameAs(ATO);
    }

    @Test
    void entregaAsRegrasDoTipoDaEspecie() {
        var regras = new RegrasDasEspecies(List.of(ATO)).para(TipoDeEspecie.CONVENCIONAL);

        assertThat(regras.tipo()).isEqualTo(TipoDeEspecie.CONVENCIONAL);
        assertThat(regras.cicloDeVida()).isInstanceOf(CicloDeVidaDeEspecieConvencional.class);
    }

    @Test
    void umaEspecieDeNpaRecebeAsRegrasDaNpaEAsDemaisAsDeEspecieConvencional() {
        var npaRegras = new RegrasDeComunicacaoOficialPadronizada(null, null, null, null, null, null, null, null, null, null);
        var registro = new RegrasDasEspecies(List.of(ATO, npaRegras));
        var especieNpa = new EspecieNormativa();
        especieNpa.setTipoDeEspecie(TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA);

        assertThat(registro.para(especieNpa)).isSameAs(npaRegras);
        assertThat(registro.para(new EspecieNormativa())).isSameAs(ATO);
        assertThat(npaRegras.tipo()).isEqualTo(TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA);
    }

    @Test
    void semNenhumaRegraRegistradaFalhaComMensagemClara() {
        var vazio = new RegrasDasEspecies(List.of());

        assertThatThrownBy(() -> vazio.para(TipoDeEspecie.CONVENCIONAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONVENCIONAL");
    }
}
