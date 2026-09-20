package br.com.danielchipolesch.domain.regras.atonormativo;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.InvalidInputException;
import br.com.danielchipolesch.infrastructure.repositories.AssuntoBasicoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Criação de um ato normativo (docs/dominio.md, "Numeração"): identificado por ESPÉCIE + ASSUNTO BÁSICO +
// SEQUENCIAL ("DCA 11-3"); o sequencial é o menor número livre da espécie naquele assunto.
class CriacaoDeAtoNormativoTest {

    private AssuntoBasicoRepository assuntos;
    private DocumentoRepository documentos;
    private CriacaoDeAtoNormativo criacao;
    private EspecieNormativa dca;
    private AssuntoBasico assunto11;
    private Usuario autor;

    @BeforeEach
    void preparar() {
        assuntos = mock(AssuntoBasicoRepository.class);
        documentos = mock(DocumentoRepository.class);
        criacao = new CriacaoDeAtoNormativo(assuntos, documentos);

        dca = new EspecieNormativa();
        dca.setSigla("DCA");
        assunto11 = new AssuntoBasico();
        assunto11.setCodigo("11");
        var om = new OrganizacaoMilitar();
        autor = new Usuario();
        autor.setOm(om);
    }

    private Documento existente(int sequencial) {
        var d = new Documento();
        d.setNumeroSecundario(sequencial);
        return d;
    }

    @Test
    void oPrimeiroDocumentoDoAssuntoRecebeOSequencialUm() {
        when(assuntos.findById(1L)).thenReturn(Optional.of(assunto11));
        when(documentos.findByEspecieNormativaAndAssuntoBasico(any(), any())).thenReturn(List.of());

        var doc = criacao.montar(new DocumentoRequestCreateDto(1L, 1L, null, "Título"), dca, autor);

        assertThat(doc.getIdentificacao()).isEqualTo("DCA 11-1");
        assertThat(doc.getNumeroSecundario()).isEqualTo(1);
        assertThat(doc.getAssuntoBasico()).isSameAs(assunto11);
    }

    @Test
    void oDocumentoNasceEmRascunhoNaoPublicadoComAutorEOmDeQuemCria() {
        when(assuntos.findById(1L)).thenReturn(Optional.of(assunto11));
        when(documentos.findByEspecieNormativaAndAssuntoBasico(any(), any())).thenReturn(List.of());

        var doc = criacao.montar(new DocumentoRequestCreateDto(1L, 1L, null, "Título"), dca, autor);

        assertThat(doc.getSituacaoLocal()).isEqualTo(SituacaoLocalEnum.RASCUNHO);
        assertThat(doc.getSituacaoBca()).isEqualTo(SituacaoBcaEnum.NAO_PUBLICADO);
        assertThat(doc.getAutor()).isSameAs(autor);
        assertThat(doc.getOm()).isSameAs(autor.getOm());
        assertThat(doc.getTituloDocumento()).isEqualTo("Título");
    }

    @Test
    void reaproveitaOMenorSequencialLivre() {
        when(assuntos.findById(1L)).thenReturn(Optional.of(assunto11));
        when(documentos.findByEspecieNormativaAndAssuntoBasico(any(), any()))
                .thenReturn(List.of(existente(1), existente(3)));

        var doc = criacao.montar(new DocumentoRequestCreateDto(1L, 1L, null, "Título"), dca, autor);

        assertThat(doc.getIdentificacao()).isEqualTo("DCA 11-2");
    }

    @Test
    void semLacunaUsaOProximoDaSequencia() {
        assertThat(CriacaoDeAtoNormativo.proximoSequencial(List.of(2, 1, 3))).isEqualTo(4);
        assertThat(CriacaoDeAtoNormativo.proximoSequencial(List.of())).isEqualTo(1);
        assertThat(CriacaoDeAtoNormativo.proximoSequencial(List.of(2))).isEqualTo(1);
    }

    @Test
    void semAssuntoBasicoOPedidoERecusado() {
        assertThatThrownBy(() -> criacao.montar(new DocumentoRequestCreateDto(1L, null, null, "Título"), dca, autor))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("assunto básico");
    }

    @Test
    void aCopiaRecebeOProximoSequencialEFicaEmRascunhoComOAutorDeQuemCopia() {
        var original = new Documento();
        original.setEspecieNormativa(dca);
        original.setAssuntoBasico(assunto11);
        original.setTituloDocumento("Original");
        when(documentos.findByEspecieNormativaAndAssuntoBasico(any(), any())).thenReturn(List.of(existente(1)));

        var copia = criacao.montarCopiaDe(original, autor);

        assertThat(copia.getIdentificacao()).isEqualTo("DCA 11-2");
        assertThat(copia.getTituloDocumento()).isEqualTo("Original");
        assertThat(copia.getSituacaoLocal()).isEqualTo(SituacaoLocalEnum.RASCUNHO);
        assertThat(copia.getAutor()).isSameAs(autor);
    }
}
