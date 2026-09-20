package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.VersaoDocumentoEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum.*;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Versão vigente (a da situação BCA) x versão em tramitação (a da etapa local em curso):
// docs/ciclo-de-vida.md.
class VersoesDocumentoTest {

    private static Documento doc(SituacaoBcaEnum bca, SituacaoLocalEnum local) {
        var d = new Documento();
        d.setSituacaoBca(bca);
        d.setSituacaoLocal(local);
        return d;
    }

    @ParameterizedTest
    @EnumSource(SituacaoLocalEnum.class)
    void naoPublicadoNaoTemVersaoVigente(SituacaoLocalEnum local) {
        assertThat(VersoesDocumento.temVersaoVigente(doc(NAO_PUBLICADO, local))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoBcaEnum.class, names = {"PUBLICADO", "REVOGADO"})
    void publicadoERevogadoTemVersaoVigente(SituacaoBcaEnum bca) {
        assertThat(VersoesDocumento.temVersaoVigente(doc(bca, SEM_ETAPA))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoLocalEnum.class, names = "SEM_ETAPA", mode = EnumSource.Mode.EXCLUDE)
    void qualquerEtapaEmCursoTemVersaoEmTramitacao(SituacaoLocalEnum local) {
        assertThat(VersoesDocumento.temVersaoEmTramitacao(doc(PUBLICADO, local))).isTrue();
    }

    @Test
    void semEtapaNaoTemVersaoEmTramitacao() {
        assertThat(VersoesDocumento.temVersaoEmTramitacao(doc(PUBLICADO, SEM_ETAPA))).isFalse();
        assertThat(VersoesDocumento.temVersaoEmTramitacao(doc(REVOGADO, SEM_ETAPA))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(SituacaoLocalEnum.class)
    void soEmPublicacaoEEmRevogacaoTemAVersaoEmTramitacaoArmazenada(SituacaoLocalEnum local) {
        boolean esperado = local == EM_PUBLICACAO || local == EM_REVOGACAO;
        assertThat(VersoesDocumento.emTramitacaoArmazenada(doc(PUBLICADO, local))).isEqualTo(esperado);
    }

    @Test
    void padraoEATramitacaoQuandoHa() {
        assertThat(VersoesDocumento.resolver(doc(PUBLICADO, EM_ALTERACAO), null)).isEqualTo(VersaoDocumentoEnum.TRAMITACAO);
        assertThat(VersoesDocumento.resolver(doc(NAO_PUBLICADO, MINUTA), null)).isEqualTo(VersaoDocumentoEnum.TRAMITACAO);
    }

    @Test
    void padraoEAVigenteQuandoNaoHaTramitacao() {
        assertThat(VersoesDocumento.resolver(doc(PUBLICADO, SEM_ETAPA), null)).isEqualTo(VersaoDocumentoEnum.VIGENTE);
        assertThat(VersoesDocumento.resolver(doc(REVOGADO, SEM_ETAPA), null)).isEqualTo(VersaoDocumentoEnum.VIGENTE);
    }

    @Test
    void ePossivelPedirAVigenteDuranteUmaAlteracao() {
        assertThat(VersoesDocumento.resolver(doc(PUBLICADO, EM_ALTERACAO), VersaoDocumentoEnum.VIGENTE))
                .isEqualTo(VersaoDocumentoEnum.VIGENTE);
    }

    @Test
    void naoHaVersaoVigenteAntesDaPrimeiraPublicacao() {
        assertThatThrownBy(() -> VersoesDocumento.resolver(doc(NAO_PUBLICADO, EM_PUBLICACAO), VersaoDocumentoEnum.VIGENTE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void naoHaVersaoEmTramitacaoSemEtapaEmCurso() {
        assertThatThrownBy(() -> VersoesDocumento.resolver(doc(PUBLICADO, SEM_ETAPA), VersaoDocumentoEnum.TRAMITACAO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── Revogação total: selo REVOGADO (nenhum elemento tachado) ────────────────

    @Test
    void seloDeRevogadoQuandoASituacaoBcaERevogado() {
        assertThat(VersoesDocumento.exibeSeloRevogado(doc(REVOGADO, SEM_ETAPA))).isTrue();
    }

    @Test
    void seloNaVersaoEmTramitacaoDaRevogacao() {
        assertThat(VersoesDocumento.exibeSeloRevogado(doc(PUBLICADO, EM_REVOGACAO))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoLocalEnum.class, names = "EM_REVOGACAO", mode = EnumSource.Mode.EXCLUDE)
    void semSeloEnquantoARevogacaoNaoEstaAprovadaNemPublicada(SituacaoLocalEnum local) {
        // Inclui ANALISE_REVOGACAO: o pedido ainda pode ser devolvido.
        assertThat(VersoesDocumento.exibeSeloRevogado(doc(PUBLICADO, local))).isFalse();
        assertThat(VersoesDocumento.exibeSeloRevogado(doc(NAO_PUBLICADO, local))).isFalse();
    }
}
