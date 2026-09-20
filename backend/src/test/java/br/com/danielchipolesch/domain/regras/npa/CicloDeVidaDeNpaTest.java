package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.regras.AcaoDeEtapa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum.*;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;
import static org.assertj.core.api.Assertions.assertThat;

// Ciclo de vida da NPA (docs/ciclo-de-vida.md): só publicação e revogação -- NÃO há alteração. Para mudar uma NPA
// publicada cria-se outra e revoga-se a anterior.
class CicloDeVidaDeNpaTest {

    private final CicloDeVidaDeNpa ciclo = new CicloDeVidaDeNpa();

    private AcaoDeEtapa acao(SituacaoLocalEnum atual, SituacaoLocalEnum destino, SituacaoBcaEnum bca) {
        return ciclo.acaoPara(atual, destino, bca).orElse(null);
    }

    @Test
    void oCaminhoDeElaboracaoAteAPublicacao() {
        assertThat(acao(RASCUNHO, MINUTA, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.MINUTAR);
        assertThat(acao(MINUTA, EM_REVISAO, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.ENVIAR_PARA_REVISAO);
        assertThat(acao(EM_REVISAO, EM_PUBLICACAO, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.APROVAR);
        assertThat(acao(EM_PUBLICACAO, SEM_ETAPA, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.PUBLICAR);
    }

    @Test
    void devolverLevaSempreAMinuta() {
        assertThat(acao(EM_REVISAO, MINUTA, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.DEVOLVER);
        assertThat(acao(EM_PUBLICACAO, MINUTA, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.DEVOLVER);
    }

    @Test
    void naoExisteAlteracao() {
        // Nem iniciar, nem a etapa EM_ALTERACAO como origem ou destino.
        assertThat(acao(SEM_ETAPA, EM_ALTERACAO, PUBLICADO)).isNull();
        assertThat(acao(EM_ALTERACAO, EM_REVISAO, PUBLICADO)).isNull();
        assertThat(acao(EM_ALTERACAO, SEM_ETAPA, PUBLICADO)).isNull();
        assertThat(acao(EM_REVISAO, EM_ALTERACAO, PUBLICADO)).isNull();
        assertThat(acao(EM_PUBLICACAO, EM_ALTERACAO, PUBLICADO)).isNull();
    }

    @Test
    void aRevogacaoSoComecaEmNpaPublicada() {
        assertThat(acao(SEM_ETAPA, ANALISE_REVOGACAO, PUBLICADO)).isEqualTo(AcaoDeEtapa.PEDIR_REVOGACAO);
        assertThat(acao(SEM_ETAPA, ANALISE_REVOGACAO, NAO_PUBLICADO)).isNull();
        assertThat(acao(SEM_ETAPA, ANALISE_REVOGACAO, REVOGADO)).isNull();
    }

    @Test
    void oCaminhoDaRevogacao() {
        assertThat(acao(ANALISE_REVOGACAO, EM_REVOGACAO, PUBLICADO)).isEqualTo(AcaoDeEtapa.APROVAR_REVOGACAO);
        assertThat(acao(ANALISE_REVOGACAO, SEM_ETAPA, PUBLICADO)).isEqualTo(AcaoDeEtapa.DEVOLVER_ANALISE);
        assertThat(acao(EM_REVOGACAO, SEM_ETAPA, PUBLICADO)).isEqualTo(AcaoDeEtapa.REVOGAR);
    }

    @Test
    void soUmDocumentoNaoPublicadoPodeSerCancelado() {
        assertThat(acao(RASCUNHO, CANCELADO, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.CANCELAR_DOCUMENTO);
        assertThat(acao(MINUTA, CANCELADO, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.CANCELAR_DOCUMENTO);
        assertThat(acao(EM_REVISAO, CANCELADO, NAO_PUBLICADO)).isNull();
        assertThat(acao(SEM_ETAPA, CANCELADO, PUBLICADO)).isNull();
    }

    @ParameterizedTest
    @EnumSource(SituacaoLocalEnum.class)
    void deCanceladoNaoSaiParaLugarNenhum(SituacaoLocalEnum destino) {
        assertThat(acao(CANCELADO, destino, NAO_PUBLICADO)).isNull();
    }

    @Test
    void nenhumaAcaoDeAlteracaoEProduzidaEmNenhumaTransicao() {
        for (var atual : SituacaoLocalEnum.values()) {
            for (var destino : SituacaoLocalEnum.values()) {
                for (var bca : SituacaoBcaEnum.values()) {
                    var acao = acao(atual, destino, bca);
                    assertThat(acao).isNotIn(AcaoDeEtapa.INICIAR_ALTERACAO, AcaoDeEtapa.CANCELAR_ALTERACAO);
                }
            }
        }
    }
}
