package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.regras.AcaoDeEtapa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum.*;
import static intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;
import static org.assertj.core.api.Assertions.assertThat;

// Ciclo de vida da NPA (docs/ciclo-de-vida.md): o mesmo das convencionais (rascunho, minuta, publicação e revogação), mas SEM alteração.
// Para mudar uma NPA publicada cria-se outra e revoga-se a anterior.
class CicloDeVidaDeNpaTest {

    private final CicloDeVidaDeNpa ciclo = new CicloDeVidaDeNpa();

    private AcaoDeEtapa acao(SituacaoLocalEnum atual, SituacaoLocalEnum destino, SituacaoBcaEnum bca) {
        return ciclo.acaoPara(atual, destino, bca).orElse(null);
    }

    @Test
    void oCaminhoDeElaboracaoAteAPublicacao() {
        // Rascunho -> Minuta só ao ENVIAR PARA APROVAÇÃO; a Minuta é o documento em aprovação, que a autoridade aprova.
        assertThat(acao(RASCUNHO, MINUTA, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.ENVIAR_PARA_APROVACAO);
        assertThat(acao(MINUTA, EM_PUBLICACAO, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.APROVAR);
        assertThat(acao(EM_PUBLICACAO, SEM_ETAPA, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.PUBLICAR);
    }

    @Test
    void devolverLevaSempreAosAutoresNoRascunho() {
        assertThat(acao(MINUTA, RASCUNHO, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.DEVOLVER);
        assertThat(acao(EM_PUBLICACAO, RASCUNHO, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.DEVOLVER);
        // não se "devolve" para a própria minuta, e a etapa Em revisão não existe mais
        assertThat(acao(EM_PUBLICACAO, MINUTA, NAO_PUBLICADO)).isNull();
    }

    @Test
    void aAutoridadeArquivaUmaMinuta() {
        assertThat(acao(MINUTA, ARQUIVADO, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.ARQUIVAR);
        // só da minuta: nem rascunho, nem etapa de publicação se arquivam
        assertThat(acao(RASCUNHO, ARQUIVADO, NAO_PUBLICADO)).isNull();
        assertThat(acao(EM_PUBLICACAO, ARQUIVADO, NAO_PUBLICADO)).isNull();
    }

    @Test
    void naoExisteAlteracao() {
        // Nem iniciar, nem a etapa EM_ALTERACAO como origem ou destino.
        assertThat(acao(SEM_ETAPA, EM_ALTERACAO, PUBLICADO)).isNull();
        assertThat(acao(EM_ALTERACAO, MINUTA, PUBLICADO)).isNull();
        assertThat(acao(EM_ALTERACAO, SEM_ETAPA, PUBLICADO)).isNull();
        assertThat(acao(MINUTA, EM_ALTERACAO, PUBLICADO)).isNull();
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
    void soUmRascunhoPodeSerCancelado() {
        assertThat(acao(RASCUNHO, CANCELADO, NAO_PUBLICADO)).isEqualTo(AcaoDeEtapa.CANCELAR_DOCUMENTO);
        // na minuta (em aprovação) não se cancela: a autoridade devolve aos autores ou arquiva
        assertThat(acao(MINUTA, CANCELADO, NAO_PUBLICADO)).isNull();
        assertThat(acao(SEM_ETAPA, CANCELADO, PUBLICADO)).isNull();
    }

    @ParameterizedTest
    @EnumSource(SituacaoLocalEnum.class)
    void deCanceladoNaoSaiParaLugarNenhum(SituacaoLocalEnum destino) {
        assertThat(acao(CANCELADO, destino, NAO_PUBLICADO)).isNull();
    }

    @ParameterizedTest
    @EnumSource(SituacaoLocalEnum.class)
    void deArquivadoNaoSaiParaLugarNenhum(SituacaoLocalEnum destino) {
        assertThat(acao(ARQUIVADO, destino, NAO_PUBLICADO)).isNull();
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

    // "Substituir": só uma NPA publicada e parada (sem etapa em curso) pode ser substituída.
    @Test
    void soUmaNpaPublicadaESemEtapaEmCursoPodeSerSubstituida() {
        assertThat(ciclo.podeSerSubstituido(SEM_ETAPA, PUBLICADO)).isTrue();
    }

    @Test
    void quemAindaNaoFoiPublicadoNaoSeSubstitui() {
        assertThat(ciclo.podeSerSubstituido(RASCUNHO, NAO_PUBLICADO)).isFalse();
        assertThat(ciclo.podeSerSubstituido(MINUTA, NAO_PUBLICADO)).isFalse();
        assertThat(ciclo.podeSerSubstituido(EM_PUBLICACAO, NAO_PUBLICADO)).isFalse();
        assertThat(ciclo.podeSerSubstituido(SEM_ETAPA, NAO_PUBLICADO)).isFalse();
    }

    @Test
    void comRevogacaoEmCursoOuJaRevogadaNaoSeSubstitui() {
        assertThat(ciclo.podeSerSubstituido(ANALISE_REVOGACAO, PUBLICADO)).isFalse();
        assertThat(ciclo.podeSerSubstituido(EM_REVOGACAO, PUBLICADO)).isFalse();
        assertThat(ciclo.podeSerSubstituido(SEM_ETAPA, REVOGADO)).isFalse();
        assertThat(ciclo.podeSerSubstituido(CANCELADO, NAO_PUBLICADO)).isFalse();
        assertThat(ciclo.podeSerSubstituido(ARQUIVADO, NAO_PUBLICADO)).isFalse();
    }
}
