package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Criação de uma NPA (docs/dominio.md, "NPA"): identificação em texto livre; sem assunto básico nem sequencial
// gerado; o assunto do cabeçalho é o título do documento.
class CriacaoDeNpaTest {

    private final CriacaoDeNpa criacao = new CriacaoDeNpa();
    private EspecieNormativa npa;
    private Usuario autor;

    @BeforeEach
    void preparar() {
        npa = new EspecieNormativa();
        npa.setSigla("NPA");
        autor = new Usuario();
        autor.setOm(new OrganizacaoMilitar());
    }

    @Test
    void aIdentificacaoEOTextoLivreInformado() {
        var doc = criacao.montar(new DocumentoRequestCreateDto(1L, null, "NPA-AGO-01", "Funcionamento da Divisão"), npa, autor);

        assertThat(doc.getIdentificacao()).isEqualTo("NPA-AGO-01");
        assertThat(doc.getTituloDocumento()).isEqualTo("Funcionamento da Divisão");
    }

    @Test
    void aIdentificacaoAceitaOFormatoComTracosDePreenchimento() {
        var doc = criacao.montar(new DocumentoRequestCreateDto(1L, null, "NPA 44-__/2026", "Assunto"), npa, autor);

        assertThat(doc.getIdentificacao()).isEqualTo("NPA 44-__/2026");
    }

    @Test
    void espacosNasPontasDaIdentificacaoSaoRemovidos() {
        var doc = criacao.montar(new DocumentoRequestCreateDto(1L, null, "  NPA-AGO-01  ", "Assunto"), npa, autor);

        assertThat(doc.getIdentificacao()).isEqualTo("NPA-AGO-01");
    }

    @Test
    void naoUsaAssuntoBasicoNemSequencial() {
        // Mesmo que o pedido traga um assunto básico, a NPA o ignora.
        var doc = criacao.montar(new DocumentoRequestCreateDto(1L, 99L, "NPA-1", "Assunto"), npa, autor);

        assertThat(doc.getAssuntoBasico()).isNull();
        assertThat(doc.getNumeroSecundario()).isNull();
    }

    @Test
    void nasceEmRascunhoNaoPublicadoComAutorEOmDeQuemCria() {
        var doc = criacao.montar(new DocumentoRequestCreateDto(1L, null, "NPA-1", "Assunto"), npa, autor);

        assertThat(doc.getSituacaoLocal()).isEqualTo(SituacaoLocalEnum.RASCUNHO);
        assertThat(doc.getSituacaoBca()).isEqualTo(SituacaoBcaEnum.NAO_PUBLICADO);
        assertThat(doc.getAutor()).isSameAs(autor);
        assertThat(doc.getOm()).isSameAs(autor.getOm());
        assertThat(doc.getEspecieNormativa()).isSameAs(npa);
    }

    @Test
    void semIdentificacaoOPedidoERecusado() {
        assertThatThrownBy(() -> criacao.montar(new DocumentoRequestCreateDto(1L, null, null, "Assunto"), npa, autor))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("identificação");
        assertThatThrownBy(() -> criacao.montar(new DocumentoRequestCreateDto(1L, null, "   ", "Assunto"), npa, autor))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("identificação");
    }

    @Test
    void identificacaoLongaDemaisERecusada() {
        var longa = "N".repeat(CriacaoDeNpa.TAMANHO_MAXIMO_DA_IDENTIFICACAO + 1);

        assertThatThrownBy(() -> criacao.montar(new DocumentoRequestCreateDto(1L, null, longa, "Assunto"), npa, autor))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("120");
    }

    @Test
    void aCopiaMantemAIdentificacaoEOTituloEFicaEmRascunhoComOAutorDeQuemCopia() {
        var original = new Documento();
        original.setEspecieNormativa(npa);
        original.setIdentificacao("NPA-AGO-01");
        original.setTituloDocumento("Original");
        var outroAutor = new Usuario();
        outroAutor.setOm(new OrganizacaoMilitar());

        var copia = criacao.montarCopiaDe(original, outroAutor);

        assertThat(copia.getIdentificacao()).isEqualTo("NPA-AGO-01");
        assertThat(copia.getTituloDocumento()).isEqualTo("Original");
        assertThat(copia.getSituacaoLocal()).isEqualTo(SituacaoLocalEnum.RASCUNHO);
        assertThat(copia.getAutor()).isSameAs(outroAutor);
        assertThat(copia.getOm()).isSameAs(outroAutor.getOm());
    }

    // O código (identificação) da NPA pode ser corrigido nos metadados do editor -- só em Rascunho ou Minuta.
    private Documento npaEm(SituacaoLocalEnum etapa) {
        var doc = new Documento();
        doc.setEspecieNormativa(npa);
        doc.setIdentificacao("NPA-AGO-01");
        doc.setSituacaoLocal(etapa);
        return doc;
    }

    @Test
    void oCodigoDaNpaPodeSerAlteradoEmRascunhoEMinuta() {
        assertThat(criacao.novaIdentificacao(npaEm(SituacaoLocalEnum.RASCUNHO), "NPA-AGO-02")).contains("NPA-AGO-02");
        assertThat(criacao.novaIdentificacao(npaEm(SituacaoLocalEnum.MINUTA), "  NPA 44-__/2026 ")).contains("NPA 44-__/2026");
    }

    @Test
    void semMudancaNaoHaNovaIdentificacao() {
        // Nula ou igual à atual: é o autosave do editor, que reenvia o documento inteiro -- vale em qualquer etapa.
        assertThat(criacao.novaIdentificacao(npaEm(SituacaoLocalEnum.RASCUNHO), null)).isEmpty();
        assertThat(criacao.novaIdentificacao(npaEm(SituacaoLocalEnum.EM_REVISAO), "NPA-AGO-01")).isEmpty();
        assertThat(criacao.novaIdentificacao(npaEm(SituacaoLocalEnum.EM_REVISAO), " NPA-AGO-01 ")).isEmpty();
    }

    @Test
    void depoisDeMinutaOCodigoDaNpaNaoMudaMais() {
        for (var etapa : new SituacaoLocalEnum[]{SituacaoLocalEnum.EM_REVISAO, SituacaoLocalEnum.EM_PUBLICACAO, SituacaoLocalEnum.SEM_ETAPA}) {
            assertThatThrownBy(() -> criacao.novaIdentificacao(npaEm(etapa), "NPA-AGO-02"))
                    .isInstanceOf(StatusCannotBeUpdatedException.class).hasMessageContaining("Rascunho ou Minuta");
        }
    }

    @Test
    void oNovoCodigoDaNpaTemAsMesmasRegrasDeQuandoFoiCriada() {
        assertThatThrownBy(() -> criacao.novaIdentificacao(npaEm(SituacaoLocalEnum.RASCUNHO), "   "))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("identificação");
        assertThatThrownBy(() -> criacao.novaIdentificacao(npaEm(SituacaoLocalEnum.RASCUNHO),
                "N".repeat(CriacaoDeNpa.TAMANHO_MAXIMO_DA_IDENTIFICACAO + 1)))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("120");
    }
}
