package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.regimes.atonormativo.LeiauteHtmlDeAtoNormativo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.List;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.ARTIGO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.CAPITULO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.PARAGRAFO;
import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO;
import static org.assertj.core.api.Assertions.assertThat;

// Exportação em HTML: a numeração (artigo/capítulo/seção/parágrafo) precisa seguir a
// MESMA regra do PDF e do editor -- em vigor nunca é renumerado, incluído por emenda
// recebe letra (LC 95/1998; Decreto 12.002/2024, art. 14, IV). Ver docs/dominio.md e
// a regra de consistência entre formatos no CLAUDE.md. Sem Spring nem banco.
class LeiauteHtmlDeAtoNormativoTest {

    private final LeiauteHtmlDeAtoNormativo service = new LeiauteHtmlDeAtoNormativo();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "numeracaoService", new NumeracaoService());
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    // ─── Fixtures ────────────────────────────────────────────────────────────────

    private static Documento documento() {
        var especie = new EspecieNormativa();
        especie.setSigla("ICA");
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
        return doc;
    }

    private static String conteudo(String texto) {
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\""
                + texto + "\"}]}]}";
    }

    private static ItemAnexoParteNormativaResponseDto no(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                          boolean incluidoPorEmenda, ElementoEmendaStatusEnum status,
                                                          ItemAnexoParteNormativaResponseDto... filhos) {
        boolean agrupamento = tipo == CAPITULO;
        return new ItemAnexoParteNormativaResponseDto(id, null, tipo, null, agrupamento ? "Titulo " + id : null,
                agrupamento ? null : conteudo("texto" + id), null, status, null, null, null, null, null, null,
                incluidoPorEmenda, null, null, List.of(filhos));
    }

    private static ItemAnexoParteNormativaResponseDto no(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                          ItemAnexoParteNormativaResponseDto... filhos) {
        return no(id, tipo, false, ElementoEmendaStatusEnum.INALTERADO, filhos);
    }

    private static ItemAnexoParteNormativaResponseDto incluido(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                                ItemAnexoParteNormativaResponseDto... filhos) {
        return no(id, tipo, true, ElementoEmendaStatusEnum.INCLUIDO, filhos);
    }

    private String html(ItemAnexoParteNormativaResponseDto... normativos) {
        return service.gerarHtml(documento(), List.of(), List.of(normativos), List.of());
    }

    // ─── Artigos ─────────────────────────────────────────────────────────────────

    @Test
    void numeraArtigosEmSequenciaSemEmenda() {
        var html = html(no(1, CAPITULO, no(2, ARTIGO), no(3, ARTIGO)));

        assertThat(html).contains("Art. 1º").contains("Art. 2º").doesNotContain("Art. 3º");
    }

    @Test
    void artigoIncluidoEntreDoisEmVigorRecebeLetraSemRenumerarOSeguinte() {
        var html = html(no(1, CAPITULO, no(2, ARTIGO), incluido(3, ARTIGO), no(4, ARTIGO)));

        assertThat(html).contains("Art. 1º-A");
        // O artigo seguinte continua sendo o 2º; uma contagem sequencial simples daria "Art. 3º".
        assertThat(html).contains("Art. 2º").doesNotContain("Art. 3º");
    }

    @Test
    void artigoIncluidoAntesDeUmRevogadoRecebeLetraEORevogadoMantemONumero() {
        var revogado = no(4, ARTIGO, false, ElementoEmendaStatusEnum.REVOGADO);

        var html = html(no(1, CAPITULO, no(2, ARTIGO), incluido(3, ARTIGO), revogado));

        assertThat(html).contains("Art. 1º-A").contains("Art. 2º").doesNotContain("Art. 3º");
    }

    // ─── Capítulos ───────────────────────────────────────────────────────────────

    @Test
    void capituloIncluidoEntreDoisEmVigorRecebeLetra() {
        var html = html(no(1, CAPITULO), incluido(2, CAPITULO), no(3, CAPITULO));

        assertThat(html).contains("CAPÍTULO I-A").contains("CAPÍTULO II").doesNotContain("CAPÍTULO III");
    }

    // ─── Parágrafos ──────────────────────────────────────────────────────────────

    @Test
    void paragrafoUnicoQueGanhaUmSegundoParagrafoViraPrimeiro() {
        var html = html(no(1, ARTIGO, no(2, PARAGRAFO_UNICO), incluido(3, PARAGRAFO)));

        assertThat(html).contains("§ 1º").contains("§ 2º").doesNotContain("Parágrafo único");
    }

    @Test
    void paragrafoUnicoSozinhoPermaneceUnico() {
        var html = html(no(1, ARTIGO, no(2, PARAGRAFO_UNICO)));

        assertThat(html).contains("Parágrafo único.").doesNotContain("§ 1º");
    }

    @Test
    void paragrafoIncluidoEntreDoisEmVigorRecebeLetra() {
        var html = html(no(1, ARTIGO, no(2, PARAGRAFO), incluido(3, PARAGRAFO), no(4, PARAGRAFO)));

        assertThat(html).contains("§ 1º-A").contains("§ 2º").doesNotContain("§ 3º");
    }

    @Test
    void paragrafoIncluidoAntesDeUmRevogadoRecebeLetraEORevogadoMantemONumero() {
        var revogado = no(4, PARAGRAFO, false, ElementoEmendaStatusEnum.REVOGADO);

        var html = html(no(1, ARTIGO, no(2, PARAGRAFO), incluido(3, PARAGRAFO), revogado));

        assertThat(html).contains("§ 1º-A").contains("§ 2º").doesNotContain("§ 3º");
    }

    // ─── Portaria: só depois da 1ª publicação ────────────────────────────────────

    private static final String AVISO = "<p class=\"aviso-nao-substitui\">";
    private static final String EMENTA = "<div class=\"ementa-bloco\">";

    @Test
    void documentoNaoPublicadoNaoTemPortariaNemOAvisoDoBca() {
        for (var local : SituacaoLocalEnum.values()) {
            var doc = documento();
            doc.setSituacaoBca(SituacaoBcaEnum.NAO_PUBLICADO);
            doc.setSituacaoLocal(local);

            var html = htmlDe(doc);

            assertThat(html).doesNotContain(EMENTA).doesNotContain(AVISO).doesNotContain("class=\"epigrafe\"");
            // O corpo continua: o HTML abre direto no ANEXO I.
            assertThat(html).contains("ANEXO I").contains("Art. 1º");
        }
    }

    @Test
    void documentoPublicadoTemAPortariaInicialMesmoDuranteUmaAlteracao() {
        for (var local : List.of(SituacaoLocalEnum.SEM_ETAPA, SituacaoLocalEnum.EM_ALTERACAO, SituacaoLocalEnum.EM_PUBLICACAO)) {
            var doc = documento();
            doc.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
            doc.setSituacaoLocal(local);

            var html = htmlDe(doc);

            assertThat(html).contains(EMENTA).contains(AVISO);
        }
    }

    // ─── Revogação total: selo, nunca tachado ────────────────────────────────────

    private String htmlDe(Documento doc) {
        return service.gerarHtml(doc, List.of(), List.of(no(1, ARTIGO, no(2, PARAGRAFO))), List.of());
    }

    @Test
    void documentoRevogadoTemOSeloVermelhoEMantemOsElementosSemTachar() {
        var doc = documento();
        doc.setSituacaoBca(SituacaoBcaEnum.REVOGADO);
        doc.setSituacaoLocal(SituacaoLocalEnum.SEM_ETAPA);

        var html = htmlDe(doc);

        assertThat(html).contains("class=\"selo-revogado\">REVOGADO</div>");
        // A revogação total não tacha elemento nenhum: o texto sai normal.
        assertThat(html).doesNotContain("<s>").doesNotContain("line-through\">texto");
    }

    @Test
    void oSeloFicaNaPaginaDaParteNormativaPreliminarAntesDoCorpo() {
        var doc = documento();
        doc.setSituacaoBca(SituacaoBcaEnum.REVOGADO);
        doc.setSituacaoLocal(SituacaoLocalEnum.SEM_ETAPA);

        var html = htmlDe(doc);

        // Dentro da primeira página (a da Portaria), que abre antes de qualquer artigo.
        assertThat(html.indexOf("selo-revogado\">REVOGADO")).isLessThan(html.indexOf("Art. 1º"));
        assertThat(html.indexOf("selo-revogado\">REVOGADO")).isLessThan(html.indexOf("<p class=\"bold\">MINISTÉRIO DA DEFESA"));
    }

    @Test
    void aVersaoEmTramitacaoDaRevogacaoTambemLevaOSelo() {
        var doc = documento();
        doc.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        doc.setSituacaoLocal(SituacaoLocalEnum.EM_REVOGACAO);

        assertThat(htmlDe(doc)).contains("class=\"selo-revogado\">REVOGADO</div>");
    }

    @Test
    void documentoPublicadoOuNaoPublicadoNaoTemSelo() {
        var publicado = documento();
        publicado.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        publicado.setSituacaoLocal(SituacaoLocalEnum.SEM_ETAPA);
        assertThat(htmlDe(publicado)).doesNotContain("class=\"selo-revogado\"");

        var analise = documento();
        analise.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        analise.setSituacaoLocal(SituacaoLocalEnum.ANALISE_REVOGACAO);
        assertThat(htmlDe(analise)).doesNotContain("class=\"selo-revogado\"");

        assertThat(htmlDe(documento())).doesNotContain("class=\"selo-revogado\"");
    }

    // ─── Parágrafo único que vira "§ 1º": linha riscada + repetição + cláusula ───

    private static ItemAnexoParteNormativaResponseDto paragrafo(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                                ElementoEmendaStatusEnum status,
                                                                String clausulaRenumeracao, boolean incluido) {
        return new ItemAnexoParteNormativaResponseDto(id, null, tipo, null, null, conteudo("texto" + id), null, status,
                null, null, null, null, null, clausulaRenumeracao, incluido, null, null, List.of());
    }

    private static ItemAnexoParteNormativaResponseDto artigoCom(ItemAnexoParteNormativaResponseDto... paragrafos) {
        return new ItemAnexoParteNormativaResponseDto(1L, null, ARTIGO, null, null, conteudo("caput"), null,
                ElementoEmendaStatusEnum.INALTERADO, null, null, null, null, null, null, false, null, null,
                List.of(paragrafos));
    }

    private String htmlPublicado(ItemAnexoParteNormativaResponseDto... paragrafos) {
        var doc = documento();
        doc.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        doc.setSituacaoLocal(SituacaoLocalEnum.EM_ALTERACAO);
        return service.gerarHtml(doc, List.of(), List.of(artigoCom(paragrafos)), List.of());
    }

    @Test
    void unicoRenumeradoSaiRiscadoERepeteOTextoSobOParagrafo1ComAClausula() {
        var html = htmlPublicado(
                paragrafo(10, PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO, null, false),
                paragrafo(11, PARAGRAFO, ElementoEmendaStatusEnum.INCLUIDO, null, true));

        // Linha inteira riscada, com o rótulo antigo...
        assertThat(html).containsPattern("emenda-strikethrough\"><span class=\"norm-lbl\">Parágrafo único\\.");
        assertThat(html).contains("texto10");
        // ...o mesmo texto repetido sob "§ 1º", e o novo parágrafo é o "§ 2º".
        assertThat(html).contains("§ 1º").contains("§ 2º");
        assertThat(html).contains("(redação dada pela Portaria DIRAD n° XYZ");
    }

    @Test
    void depoisDePublicadaAClausulaCongeladaSubstituiOPlaceholder() {
        var html = htmlPublicado(
                paragrafo(10, PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO,
                        "(redação dada pela Portaria COMAER/DIRAP n° 9, publicada no BCA n° 3)", false),
                paragrafo(11, PARAGRAFO, ElementoEmendaStatusEnum.INCLUIDO, null, true));

        assertThat(html).contains("(redação dada pela Portaria COMAER/DIRAP n° 9, publicada no BCA n° 3)")
                .doesNotContain("(redação dada pela Portaria DIRAD");
    }

    @Test
    void unicoSozinhoNaoTemRiscadoNemClausula() {
        var html = htmlPublicado(paragrafo(10, PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO, null, false));

        assertThat(html).contains("Parágrafo único").doesNotContain("(redação dada pela");
        assertThat(html).doesNotContainPattern("emenda-strikethrough\"><span class=\"norm-lbl\">Parágrafo único");
    }

    // ─── Cláusulas das emendas: mesmas regras do PDF ─────────────────────────────

    private static ItemAnexoParteNormativaResponseDto emendado(long id, ItemAnexoParteNormativaTipoEnum tipo,
                                                               ElementoEmendaStatusEnum status, String titulo,
                                                               String tituloEmenda, String conteudoEmenda,
                                                               String clausula, String clausulaAnterior) {
        return new ItemAnexoParteNormativaResponseDto(id, null, tipo, null, titulo,
                tipo == CAPITULO ? null : conteudo("texto" + id), null, status, conteudoEmenda, tituloEmenda, null,
                clausula, clausulaAnterior, null, status == ElementoEmendaStatusEnum.INCLUIDO, null, null, List.of());
    }

    private String htmlPublicadoCom(ItemAnexoParteNormativaResponseDto... itens) {
        var doc = documento();
        doc.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        doc.setSituacaoLocal(SituacaoLocalEnum.SEM_ETAPA);
        // A portaria da ÚLTIMA publicação não pode contaminar as cláusulas das emendas anteriores.
        doc.setPortariaReferencia("Portaria ULTIMA n° 99, de 1 de janeiro de 2027");
        doc.setBcaReferencia("BCA n° 99, de 2 de janeiro de 2027");
        return service.gerarHtml(doc, List.of(), List.of(itens), List.of());
    }

    @Test
    void cadaEmendaPublicadaMostraAPortariaDaSuaPropriaAlteracaoENaoASuaUltima() {
        var html = htmlPublicadoCom(
                emendado(1, ARTIGO, ElementoEmendaStatusEnum.ALTERADO, null, null, conteudo("novo1"),
                        "(redação dada pela Portaria A n° 1, publicada no BCA n° 10)", null),
                emendado(2, ARTIGO, ElementoEmendaStatusEnum.INCLUIDO, null, null, null,
                        "(incluído pela Portaria B n° 2, publicada no BCA n° 20)", null));

        assertThat(html).contains("(redação dada pela Portaria A n° 1, publicada no BCA n° 10)")
                .contains("(incluído pela Portaria B n° 2, publicada no BCA n° 20)")
                .doesNotContain("ULTIMA");
    }

    // Só a ALTERAÇÃO usa "redação dada pela"; inclusão e revogação continuam "incluído"/"revogado".
    @Test
    void aClausulaDeAlteracaoPendenteDizRedacaoDadaEAsDemaisNaoMudam() {
        var html = htmlPublicadoCom(
                emendado(1, ARTIGO, ElementoEmendaStatusEnum.ALTERADO, null, null, conteudo("novo"), null, null),
                emendado(2, ARTIGO, ElementoEmendaStatusEnum.INCLUIDO, null, null, null, null, null),
                emendado(3, ARTIGO, ElementoEmendaStatusEnum.REVOGADO, null, null, null, null, null));

        assertThat(html).contains("(redação dada pela Portaria DIRAD n° XYZ")
                .contains("(incluído pela Portaria DIRAD n° XYZ")
                .contains("(revogado pela Portaria DIRAD n° XYZ")
                .doesNotContain("(alterado pela");
    }

    @Test
    void emendaPendenteUsaOPlaceholderNuncaAPortariaDaUltimaPublicacao() {
        var html = htmlPublicadoCom(emendado(1, ARTIGO, ElementoEmendaStatusEnum.REVOGADO, null, null, null, null, null));

        assertThat(html).contains("(revogado pela Portaria DIRAD n° XYZ").doesNotContain("ULTIMA");
    }

    @Test
    void aClausulaAnteriorAparecePorRiscadaJuntoDoTextoQueElaDescreve() {
        var html = htmlPublicadoCom(emendado(1, ARTIGO, ElementoEmendaStatusEnum.ALTERADO, null, null, conteudo("novo"),
                "(redação dada pela Portaria B n° 2, publicada no BCA n° 20)",
                "(incluído pela Portaria A n° 1, publicada no BCA n° 10)"));

        assertThat(html).containsPattern("emenda-ref-block emenda-strikethrough\">\\(incluído pela Portaria A n° 1")
                .contains("(redação dada pela Portaria B n° 2, publicada no BCA n° 20)");
    }

    @Test
    void capituloAlteradoMostraTituloOriginalRiscadoNovoTituloEClausula() {
        var html = htmlPublicadoCom(emendado(1, CAPITULO, ElementoEmendaStatusEnum.ALTERADO, "Titulo antigo",
                "Titulo novo", null, "(redação dada pela Portaria B n° 2, publicada no BCA n° 20)",
                "(incluído pela Portaria A n° 1, publicada no BCA n° 10)"));

        assertThat(html).containsPattern("cap-titulo emenda-strikethrough\">TITULO ANTIGO")
                .contains("TITULO NOVO")
                .contains("(redação dada pela Portaria B n° 2, publicada no BCA n° 20)")
                .contains("(incluído pela Portaria A n° 1, publicada no BCA n° 10)");
    }

    @Test
    void capituloRevogadoMostraOTituloRiscadoEAClausula() {
        var html = htmlPublicadoCom(emendado(1, CAPITULO, ElementoEmendaStatusEnum.REVOGADO, "Titulo antigo",
                null, null, "(revogado pela Portaria B n° 2, publicada no BCA n° 20)", null));

        assertThat(html).containsPattern("cap-titulo emenda-strikethrough\">TITULO ANTIGO")
                .contains("(revogado pela Portaria B n° 2, publicada no BCA n° 20)");
    }

    @Test
    void emDocumentoNaoPublicadoOUnicoVira1oSemRiscadoNemClausula() {
        // documento() nasce NAO_PUBLICADO: numeração livre.
        var artigo = artigoCom(
                paragrafo(10, PARAGRAFO_UNICO, ElementoEmendaStatusEnum.INALTERADO, null, false),
                paragrafo(11, PARAGRAFO, ElementoEmendaStatusEnum.INALTERADO, null, false));

        var html = service.gerarHtml(documento(), List.of(), List.of(artigo), List.of());

        assertThat(html).contains("§ 1º").contains("§ 2º").doesNotContain("(redação dada pela");
    }
}
