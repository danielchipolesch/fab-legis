package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.regras.TipoDeEspecie;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// O módulo do sistema (docs/funcionalidades.md, "Módulos"): cada tela de módulo lista só os documentos cuja espécie é do
// tipo dele -- os de um módulo nunca aparecem na tabela do outro.
class DocumentoSpecificationsTest {

    @SuppressWarnings("unchecked")
    @Test
    void oModuloListaSoOsDocumentosDaquelesTipoDeEspecie() {
        Root<Documento> root = mock(Root.class);
        Path<Object> especie = mock(Path.class);
        Path<Object> tipoDaEspecie = mock(Path.class);
        when(root.get("especieNormativa")).thenReturn(especie);
        when(especie.get("tipoDeEspecie")).thenReturn(tipoDaEspecie);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate igual = mock(Predicate.class);
        when(cb.equal(tipoDaEspecie, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA)).thenReturn(igual);

        var predicado = DocumentoSpecifications.tipoDeEspecie(TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA)
                .toPredicate(root, mock(CriteriaQuery.class), cb);

        assertThat(predicado).isSameAs(igual);
    }

    @SuppressWarnings("unchecked")
    @Test
    void semModuloNaoRestringeNada() {
        var predicado = DocumentoSpecifications.tipoDeEspecie(null)
                .toPredicate(mock(Root.class), mock(CriteriaQuery.class), mock(CriteriaBuilder.class));

        assertThat(predicado).isNull();
    }

    // Os cards do hub (docs/funcionalidades.md, "Hub"): quais etapas contam em cada um.
    @Test
    void emAndamentoSaoAsEtapasEmQueODocumentoAindaEstaEmTrabalho() {
        assertThat(DocumentoSpecifications.SITUACOES_EM_ANDAMENTO).containsExactlyInAnyOrder(
                SituacaoLocalEnum.RASCUNHO, SituacaoLocalEnum.MINUTA, SituacaoLocalEnum.EM_ALTERACAO,
                SituacaoLocalEnum.EM_REVISAO, SituacaoLocalEnum.EM_PUBLICACAO,
                SituacaoLocalEnum.ANALISE_REVOGACAO, SituacaoLocalEnum.EM_REVOGACAO);
        // Parado (SEM_ETAPA) e cancelado não são "em andamento".
        assertThat(DocumentoSpecifications.SITUACOES_EM_ANDAMENTO)
                .doesNotContain(SituacaoLocalEnum.SEM_ETAPA, SituacaoLocalEnum.CANCELADO);
    }

    @Test
    void aRevisaoEAPublicacaoSaoAsEtapasDeCadaPapelNoFluxoNormalENaRevogacao() {
        assertThat(DocumentoSpecifications.SITUACOES_DE_REVISAO)
                .containsExactlyInAnyOrder(SituacaoLocalEnum.EM_REVISAO, SituacaoLocalEnum.ANALISE_REVOGACAO);
        assertThat(DocumentoSpecifications.SITUACOES_DE_PUBLICACAO)
                .containsExactlyInAnyOrder(SituacaoLocalEnum.EM_PUBLICACAO, SituacaoLocalEnum.EM_REVOGACAO);
        // Todo documento que espera a ação de alguém também está em andamento.
        assertThat(DocumentoSpecifications.SITUACOES_EM_ANDAMENTO)
                .containsAll(DocumentoSpecifications.SITUACOES_DE_REVISAO)
                .containsAll(DocumentoSpecifications.SITUACOES_DE_PUBLICACAO);
    }

    @Test
    void publicadosERevogadosSaoAsDuasSituacoesOficiaisEmVigorOuJaEmVigor() {
        assertThat(DocumentoSpecifications.SITUACOES_OFICIAIS_PUBLICADAS)
                .isEqualTo(EnumSet.of(SituacaoBcaEnum.PUBLICADO, SituacaoBcaEnum.REVOGADO));
    }

    // Cada documento fica num só lugar do hub: um publicado que está sendo alterado (etapa em curso) sai do card "Publicados e
    // revogados" e volta quando a etapa termina.
    @SuppressWarnings("unchecked")
    @Test
    void publicadosERevogadosSaoOsQueNaoTemEtapaEmCurso() {
        Root<Documento> root = mock(Root.class);
        Path<Object> bca = mock(Path.class);
        Path<Object> local = mock(Path.class);
        when(root.get("situacaoBca")).thenReturn(bca);
        when(root.get("situacaoLocal")).thenReturn(local);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate emVigorOuRevogado = mock(Predicate.class);
        Predicate semEtapa = mock(Predicate.class);
        Predicate resultado = mock(Predicate.class);
        when(bca.in(DocumentoSpecifications.SITUACOES_OFICIAIS_PUBLICADAS)).thenReturn(emVigorOuRevogado);
        when(cb.equal(local, SituacaoLocalEnum.SEM_ETAPA)).thenReturn(semEtapa);
        when(cb.and(emVigorOuRevogado, semEtapa)).thenReturn(resultado);

        var predicado = DocumentoSpecifications.publicadosERevogados().toPredicate(root, mock(CriteriaQuery.class), cb);

        assertThat(predicado).isSameAs(resultado);
    }

    @SuppressWarnings("unchecked")
    @Test
    void aguardandoMinhaAcaoEOAtribuidoAMimComoRevisorNaRevisaoOuComoPublicadorNaPublicacao() {
        Root<Documento> root = mock(Root.class);
        Path<Object> revisor = mock(Path.class);
        Path<Object> revisorId = mock(Path.class);
        Path<Object> publicador = mock(Path.class);
        Path<Object> publicadorId = mock(Path.class);
        Path<Object> situacao = mock(Path.class);
        when(root.get("revisorAtribuido")).thenReturn(revisor);
        when(revisor.get("id")).thenReturn(revisorId);
        when(root.get("publicadorAtribuido")).thenReturn(publicador);
        when(publicador.get("id")).thenReturn(publicadorId);
        when(root.get("situacaoLocal")).thenReturn(situacao);

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate ehORevisor = mock(Predicate.class);
        Predicate ehOPublicador = mock(Predicate.class);
        Predicate naRevisao = mock(Predicate.class);
        Predicate naPublicacao = mock(Predicate.class);
        Predicate comoRevisor = mock(Predicate.class);
        Predicate comoPublicador = mock(Predicate.class);
        Predicate resultado = mock(Predicate.class);
        when(cb.equal(revisorId, 7L)).thenReturn(ehORevisor);
        when(cb.equal(publicadorId, 7L)).thenReturn(ehOPublicador);
        when(situacao.in(DocumentoSpecifications.SITUACOES_DE_REVISAO)).thenReturn(naRevisao);
        when(situacao.in(DocumentoSpecifications.SITUACOES_DE_PUBLICACAO)).thenReturn(naPublicacao);
        when(cb.and(ehORevisor, naRevisao)).thenReturn(comoRevisor);
        when(cb.and(ehOPublicador, naPublicacao)).thenReturn(comoPublicador);
        when(cb.or(comoRevisor, comoPublicador)).thenReturn(resultado);

        var predicado = DocumentoSpecifications.aguardandoAcaoDe(7L).toPredicate(root, mock(CriteriaQuery.class), cb);

        assertThat(predicado).isSameAs(resultado);
    }
}
