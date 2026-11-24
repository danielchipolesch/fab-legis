package intraer.fablegis.domain.entities.estruturaDocumento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

// Regra de imutabilidade (docs/ciclo-de-vida.md): o documento só aceita edição em RASCUNHO e EM_ALTERACAO (os autores) e em MINUTA
// (quem aprova edita). De EM_PUBLICACAO em diante, e em todo o fluxo de revogação, ninguém edita.
class SituacaoLocalEnumTest {

    @ParameterizedTest
    @EnumSource(value = SituacaoLocalEnum.class, names = {"RASCUNHO", "MINUTA", "EM_ALTERACAO"})
    void asEtapasDeEdicaoAceitamEdicao(SituacaoLocalEnum etapa) {
        assertThat(etapa.aceitaEdicao()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoLocalEnum.class,
            names = {"EM_PUBLICACAO", "ANALISE_REVOGACAO", "EM_REVOGACAO", "CANCELADO", "ARQUIVADO", "SEM_ETAPA"})
    void asDemaisEtapasEstaoCongeladas(SituacaoLocalEnum etapa) {
        assertThat(etapa.aceitaEdicao()).isFalse();
    }

    @Test
    void todaEtapaEstaClassificada() {
        // Se uma etapa nova for criada, este teste obriga a decidir se ela aceita edição (as duas listas acima somam todas).
        assertThat(SituacaoLocalEnum.values()).hasSize(9);
    }
}
