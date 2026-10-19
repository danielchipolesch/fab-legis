package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.regras.RegrasDaEspecieNormativa;
import intraer.fablegis.domain.regras.RegrasDasEspecies;
import intraer.fablegis.domain.regras.RegrasDoCicloDeVidaDoDocumento;
import intraer.fablegis.domain.regras.TipoDeEspecie;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void aRevisaoEAPublicacaoSaoAsEtapasDeCadaPapelNoFluxoNormalENaRevogacao() {
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

    private static Usuario pessoa(Long omId, PapelEnum... papeis) {
        var om = new OrganizacaoMilitar();
        om.setId(omId);
        var u = new Usuario();
        u.setId(7L);
        u.setOm(om);
        var conjunto = EnumSet.noneOf(PapelEnum.class);
        conjunto.addAll(java.util.List.of(papeis));
        u.setPapeis(conjunto);
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
        final Predicate convencionais = mock(Predicate.class);
        final Predicate npasDaOm = mock(Predicate.class);
        final Predicate soCop = mock(Predicate.class);
        final Predicate publicaConvencionais = mock(Predicate.class);
        final Predicate publicaNpas = mock(Predicate.class);
        final Predicate publicaTudo = mock(Predicate.class);
        final Predicate ouConvencionais = mock(Predicate.class);
        final Predicate ouNpas = mock(Predicate.class);
        final Predicate ouTudo = mock(Predicate.class);
        final Predicate aprovaEPublicaConvencionais = mock(Predicate.class);
        final List<List<Predicate>> tiposQueOuViu = new ArrayList<>();
        final RegrasDasEspecies regras;

        Cena() {
            Path<Object> om = mock(Path.class);
            Path<Object> omId = mock(Path.class);
            Path<Object> especie = mock(Path.class);
            Path<Object> tipo = mock(Path.class);
            when(root.get("situacaoLocal")).thenReturn(situacao);
            when(root.get("om")).thenReturn(om);
            when(om.get("id")).thenReturn(omId);
            when(root.get("especieNormativa")).thenReturn(especie);
            when(especie.get("tipoDeEspecie")).thenReturn(tipo);
            when(situacao.in(DocumentoSpecifications.SITUACOES_DE_REVISAO)).thenReturn(naRevisao);
            when(situacao.in(DocumentoSpecifications.SITUACOES_DE_PUBLICACAO)).thenReturn(naPublicacao);
            when(cb.equal(omId, 1L)).thenReturn(naOmDeQuemPergunta);
            when(cb.equal(tipo, TipoDeEspecie.CONVENCIONAL)).thenReturn(convencionais);
            when(cb.equal(tipo, TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA)).thenReturn(soCop);
            when(cb.and(naRevisao, naOmDeQuemPergunta)).thenReturn(comoAprovador);
            when(cb.and(soCop, naOmDeQuemPergunta)).thenReturn(npasDaOm); // a NPA é de uso interno: só a da OM de quem publica
            when(cb.disjunction()).thenReturn(ninguem);

            // cb.or(...) recebe os tipos que a pessoa publica: o teste registra QUAIS e devolve um predicado por combinação
            when(cb.or(any(Predicate[].class))).thenAnswer(i -> {
                List<Predicate> tipos = Arrays.stream(i.getArguments()).map(Predicate.class::cast).toList();
                tiposQueOuViu.add(tipos);
                if (tipos.size() == 2) return ouTudo;
                return tipos.get(0) == convencionais ? ouConvencionais : ouNpas;
            });
            when(cb.and(naPublicacao, ouConvencionais)).thenReturn(publicaConvencionais);
            when(cb.and(naPublicacao, ouNpas)).thenReturn(publicaNpas);
            when(cb.and(naPublicacao, ouTudo)).thenReturn(publicaTudo);
            when(cb.or(comoAprovador, publicaConvencionais)).thenReturn(aprovaEPublicaConvencionais);

            regras = new RegrasDasEspecies(List.of(
                    regrasDe(TipoDeEspecie.CONVENCIONAL, PapelEnum.PUB_BCA, false),
                    regrasDe(TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA, PapelEnum.PUB_BI, true)));
        }

        private static RegrasDaEspecieNormativa regrasDe(TipoDeEspecie tipo, PapelEnum quemPublica, boolean usoInterno) {
            var regras = mock(RegrasDaEspecieNormativa.class);
            var ciclo = mock(RegrasDoCicloDeVidaDoDocumento.class);
            when(regras.tipo()).thenReturn(tipo);
            when(regras.deUsoInternoDaOm()).thenReturn(usoInterno);
            when(regras.cicloDeVida()).thenReturn(ciclo);
            when(ciclo.papelQuePublica()).thenReturn(quemPublica);
            return regras;
        }

        Predicate aguardandoAcao(Usuario usuario) {
            return DocumentoSpecifications.aguardandoAcaoDe(usuario, regras).toPredicate(root, mock(CriteriaQuery.class), cb);
        }

        Predicate aguardandoPublicacao(Usuario usuario) {
            return DocumentoSpecifications.aguardandoPublicacaoDe(usuario, regras).toPredicate(root, mock(CriteriaQuery.class), cb);
        }
    }

    // Aprovador: o que está em revisão na SUA OM (qualquer aprovador da OM age); ninguém é "atribuído".
    @Test
    void oAprovadorVeOQueAguardaAprovacaoNaPropriaOm() {
        var c = new Cena();
        assertThat(c.aguardandoAcao(pessoa(1L, PapelEnum.ELB, PapelEnum.APR_OM))).isSameAs(c.comoAprovador);
    }

    // Publicador do BCA (só no CENDOC): as ESPÉCIES CONVENCIONAIS em publicação, de qualquer OM -- não as NPAs.
    @Test
    void oPublicadorDoBcaVeAsConvencionaisAguardandoPublicacaoDeQualquerOm() {
        var c = new Cena();
        assertThat(c.aguardandoPublicacao(pessoa(1L, PapelEnum.ELB, PapelEnum.PUB_BCA))).isSameAs(c.publicaConvencionais);
        assertThat(c.tiposQueOuViu.getLast()).containsExactly(c.convencionais);
    }

    // Publicador do Boletim Interno (qualquer OM): as NPAs em publicação da PRÓPRIA OM -- uso interno da OM, nunca as de outra.
    @Test
    void oPublicadorDoBoletimInternoVeAsNpasDaPropriaOm() {
        var c = new Cena();
        assertThat(c.aguardandoPublicacao(pessoa(1L, PapelEnum.ELB, PapelEnum.PUB_BI))).isSameAs(c.publicaNpas);
        assertThat(c.tiposQueOuViu.getLast()).containsExactly(c.npasDaOm);
    }

    @Test
    void quemTemOsDoisPapeisDePublicarVeAsDuasFilas() {
        var c = new Cena();
        assertThat(c.aguardandoPublicacao(pessoa(1L, PapelEnum.PUB_BCA, PapelEnum.PUB_BI))).isSameAs(c.publicaTudo);
        assertThat(c.tiposQueOuViu.getLast()).containsExactlyInAnyOrder(c.convencionais, c.npasDaOm);
    }

    @Test
    void quemEAprovadorEPublicadorVeAsDuasFilasNoCardAguardandoMinhaAcao() {
        var c = new Cena();
        assertThat(c.aguardandoAcao(pessoa(1L, PapelEnum.APR_OM, PapelEnum.PUB_BCA))).isSameAs(c.aprovaEPublicaConvencionais);
    }

    @Test
    void semPapelDeAprovadorNemDePublicadorNadaAguardaAPessoa() {
        var c = new Cena();
        assertThat(c.aguardandoAcao(pessoa(1L, PapelEnum.ELB))).isSameAs(c.ninguem);
        assertThat(c.aguardandoPublicacao(pessoa(1L, PapelEnum.ELB, PapelEnum.APR_OM))).isSameAs(c.ninguem);
    }
}
