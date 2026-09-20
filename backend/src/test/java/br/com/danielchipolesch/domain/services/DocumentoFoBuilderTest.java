package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// A Portaria (1ª página do PDF) só existe depois da 1ª publicação -- ver VersoesDocumento.exibePortaria
// e docs/exportacao-pdf.md. Sem Spring nem FOP: só a estrutura do XSL-FO gerado.
class DocumentoFoBuilderTest {

    private final DocumentoFoBuilder builder = new DocumentoFoBuilder();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(builder, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(builder, "numeracaoService", new NumeracaoService());
    }

    private static Documento documento(SituacaoBcaEnum bca, SituacaoLocalEnum local) {
        var especie = new EspecieNormativa();
        especie.setSigla("ICA");
        especie.setNome("Instrução do Comando da Aeronáutica");
        var assunto = new AssuntoBasico();
        assunto.setCodigo("5");
        assunto.setNome("Publicações");
        var doc = new Documento();
        doc.setId(1L);
        doc.setEspecieNormativa(especie);
        doc.setAssuntoBasico(assunto);
        doc.setNumeroSecundario(3);
        doc.setTituloDocumento("Documento de teste");
        doc.setDtCriacao(Timestamp.valueOf("2026-03-05 10:00:00"));
        var om = new br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar();
        om.setNome("OM de teste");
        doc.setOm(om);
        doc.setSituacaoBca(bca);
        doc.setSituacaoLocal(local);
        return doc;
    }

    private String fo(Documento doc) {
        return builder.buildFo(doc, List.of(), List.of(), List.of());
    }

    private static long sequencias(String fo) {
        return fo.split("<fo:page-sequence", -1).length - 1;
    }

    @Test
    void naoPublicadoNaoTemAPaginaDaPortaria() {
        for (var local : List.of(SituacaoLocalEnum.RASCUNHO, SituacaoLocalEnum.MINUTA, SituacaoLocalEnum.EM_REVISAO,
                SituacaoLocalEnum.EM_PUBLICACAO)) {
            var fo = fo(documento(SituacaoBcaEnum.NAO_PUBLICADO, local));

            // A epígrafe padrão da Portaria ("PORTARIA Nº ___") não aparece; sobram capa + corpo.
            assertThat(fo).doesNotContain("PORTARIA Nº");
            assertThat(sequencias(fo)).isEqualTo(2);
        }
    }

    @Test
    void publicadoTemAPortariaAntesDaCapa() {
        var fo = fo(documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA));

        assertThat(fo).contains("PORTARIA Nº");
        assertThat(sequencias(fo)).isEqualTo(3);
    }

    @Test
    void aPortariaInicialContinuaDuranteUmaAlteracaoEnoRevogado() {
        assertThat(fo(documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.EM_ALTERACAO))).contains("PORTARIA Nº");
        assertThat(fo(documento(SituacaoBcaEnum.REVOGADO, SituacaoLocalEnum.SEM_ETAPA))).contains("PORTARIA Nº");
    }

    @Test
    void oSeloRevogadoEstaNaPortariaDoDocumentoRevogado() {
        assertThat(fo(documento(SituacaoBcaEnum.REVOGADO, SituacaoLocalEnum.SEM_ETAPA))).contains(">REVOGADO<");
        assertThat(fo(documento(SituacaoBcaEnum.PUBLICADO, SituacaoLocalEnum.SEM_ETAPA))).doesNotContain(">REVOGADO<");
    }
}
