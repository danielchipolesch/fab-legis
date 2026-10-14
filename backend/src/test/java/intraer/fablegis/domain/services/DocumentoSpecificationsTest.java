package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PerfilEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
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
    void emTramitacaoSaoAsEtapasEmQueODocumentoAindaEstaEmTrabalho() {
        assertThat(DocumentoSpecifications.SITUACOES_EM_TRAMITACAO).containsExactlyInAnyOrder(
                SituacaoLocalEnum.RASCUNHO, SituacaoLocalEnum.MINUTA, SituacaoLocalEnum.EM_ALTERACAO,
                SituacaoLocalEnum.EM_REVISAO, SituacaoLocalEnum.EM_PUBLICACAO,
                SituacaoLocalEnum.ANALISE_REVOGACAO, SituacaoLocalEnum.EM_REVOGACAO);
        // Parado (SEM_ETAPA) e cancelado não são "em tramitação".
        assertThat(DocumentoSpecifications.SITUACOES_EM_TRAMITACAO)
                .doesNotContain(SituacaoLocalEnum.SEM_ETAPA, SituacaoLocalEnum.CANCELADO);
    }

    @Test
    void aRevisaoEAPublicacaoSaoAsEtapasDeCadaPerfilNoFluxoNormalENaRevogacao() {
        assertThat(DocumentoSpecifications.SITUACOES_DE_REVISAO)
                .containsExactlyInAnyOrder(SituacaoLocalEnum.EM_REVISAO, SituacaoLocalEnum.ANALISE_REVOGACAO);
        assertThat(DocumentoSpecifications.SITUACOES_DE_PUBLICACAO)
                .containsExactlyInAnyOrder(SituacaoLocalEnum.EM_PUBLICACAO, SituacaoLocalEnum.EM_REVOGACAO);
        // Todo documento que espera a ação de alguém também está em tramitação.
        assertThat(DocumentoSpecifications.SITUACOES_EM_TRAMITACAO)
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

    // O card "Documentos em Tramitação nas OMs": em etapa de trabalho, de qualquer OM, mas não os da própria pessoa (esses estão
    // em "Meus Documentos em Tramitação", e um documento não aparece duas vezes no hub).
    @SuppressWarnings("unchecked")
    @Test
    void emTramitacaoDeOutrosSaoOsEmEtapaDeTrabalhoQueNaoSaoMeus() {
        Root<Documento> root = mock(Root.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        CriteriaQuery<Object> query = mock(CriteriaQuery.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        Path<Object> situacao = mock(Path.class);
        when(root.get("situacaoLocal")).thenReturn(situacao);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate emEtapaDeTrabalho = mock(Predicate.class);
        Predicate meus = mock(Predicate.class);
        Predicate naoMeus = mock(Predicate.class);
        Predicate resultado = mock(Predicate.class);
        when(situacao.in(DocumentoSpecifications.SITUACOES_EM_TRAMITACAO)).thenReturn(emEtapaDeTrabalho);
        // "meus" = autor OU coautor (a mesma aba "meus"): um OR entre "sou o autor" e "estou entre os coautores".
        Predicate souOAutor = mock(Predicate.class);
        when(cb.equal(org.mockito.ArgumentMatchers.<jakarta.persistence.criteria.Expression<?>>any(), org.mockito.ArgumentMatchers.eq(7L)))
                .thenReturn(souOAutor);
        when(cb.or(org.mockito.ArgumentMatchers.any(Predicate.class), org.mockito.ArgumentMatchers.any(Predicate.class))).thenReturn(meus);
        when(cb.not(meus)).thenReturn(naoMeus);
        when(cb.and(emEtapaDeTrabalho, naoMeus)).thenReturn(resultado);

        var predicado = DocumentoSpecifications.emTramitacaoDeOutros(7L).toPredicate(root, query, cb);

        assertThat(predicado).isSameAs(resultado);
    }

    // ─── Aguardando minha ação: sem atribuição pessoal ─────────────────────────

    private static Usuario pessoa(Long omId, PerfilEnum... perfis) {
        var om = new OrganizacaoMilitar();
        om.setId(omId);
        var u = new Usuario();
        u.setId(7L);
        u.setOm(om);
        var conjunto = EnumSet.noneOf(PerfilEnum.class);
        conjunto.addAll(java.util.List.of(perfis));
        u.setPerfis(conjunto);
        return u;
    }

    @SuppressWarnings("unchecked")
    private static class Cena {
        final Root<Documento> root = mock(Root.class);
        final Path<Object> situacao = mock(Path.class);
        final CriteriaBuilder cb = mock(CriteriaBuilder.class);
        final Predicate naRevisao = mock(Predicate.class);
        final Predicate naPublicacao = mock(Predicate.class);
        final Predicate naOmDeQuemPergunta = mock(Predicate.class);
        final Predicate comoAprovador = mock(Predicate.class);
        final Predicate ninguem = mock(Predicate.class);
        final Predicate ambos = mock(Predicate.class);

        Cena() {
            Path<Object> om = mock(Path.class);
            Path<Object> omId = mock(Path.class);
            when(root.get("situacaoLocal")).thenReturn(situacao);
            when(root.get("om")).thenReturn(om);
            when(om.get("id")).thenReturn(omId);
            when(situacao.in(DocumentoSpecifications.SITUACOES_DE_REVISAO)).thenReturn(naRevisao);
            when(situacao.in(DocumentoSpecifications.SITUACOES_DE_PUBLICACAO)).thenReturn(naPublicacao);
            when(cb.equal(omId, 1L)).thenReturn(naOmDeQuemPergunta);
            when(cb.and(naRevisao, naOmDeQuemPergunta)).thenReturn(comoAprovador);
            when(cb.disjunction()).thenReturn(ninguem);
            when(cb.or(comoAprovador, naPublicacao)).thenReturn(ambos);
        }

        Predicate para(Usuario usuario) {
            return DocumentoSpecifications.aguardandoAcaoDe(usuario).toPredicate(root, mock(CriteriaQuery.class), cb);
        }
    }

    // Aprovador: o que está em revisão na SUA OM (qualquer aprovador da OM age); ninguém é "atribuído".
    @Test
    void oAprovadorVeOQueAguardaAprovacaoNaPropriaOm() {
        var c = new Cena();
        assertThat(c.para(pessoa(1L, PerfilEnum.ELB, PerfilEnum.APR_OM))).isSameAs(c.comoAprovador);
    }

    // Publicador (só existe no CENDOC): o que está em publicação, de qualquer OM.
    @Test
    void oPublicadorVeOQueAguardaPublicacaoDeQualquerOm() {
        var c = new Cena();
        assertThat(c.para(pessoa(1L, PerfilEnum.ELB, PerfilEnum.PUB_BCA))).isSameAs(c.naPublicacao);
    }

    @Test
    void quemEAprovadorEPublicadorVeAsDuasFilas() {
        var c = new Cena();
        assertThat(c.para(pessoa(1L, PerfilEnum.APR_OM, PerfilEnum.PUB_BCA))).isSameAs(c.ambos);
    }

    @Test
    void semPerfilDeAprovadorNemDePublicadorNadaAguardaAPessoa() {
        var c = new Cena();
        assertThat(c.para(pessoa(1L, PerfilEnum.ELB))).isSameAs(c.ninguem);
    }
}
