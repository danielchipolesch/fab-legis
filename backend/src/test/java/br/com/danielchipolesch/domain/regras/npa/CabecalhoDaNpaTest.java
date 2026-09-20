package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.npaDtos.CamposDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Os textos do cabeçalho e do fecho da NPA, resolvidos uma só vez para PDF, HTML e prévia (docs/dominio.md, "NPA").
class CabecalhoDaNpaTest {

    private static final CamposDaNpaDto CAMPOS = new CamposDaNpaDto("DIVISÃO DE SUPORTE", "Brasília", List.of());

    private static Documento documento(SituacaoBcaEnum bca) {
        var om = new OrganizacaoMilitar();
        om.setNome("Grupo de Apoio");
        var doc = new Documento();
        doc.setIdentificacao("NPA-AGO-01");
        doc.setTituloDocumento("Funcionamento");
        doc.setOm(om);
        doc.setSituacaoBca(bca);
        return doc;
    }

    private static AnexoResponseDto anexo(int ordem, String titulo) {
        return new AnexoResponseDto((long) ordem, titulo, null, ordem);
    }

    @Test
    void asTresLinhasDeCimaSaoComandoOmESetorEmissor() {
        var c = CabecalhoDaNpa.de(documento(SituacaoBcaEnum.NAO_PUBLICADO), CAMPOS, List.of());

        assertThat(c.linhasDeCima()).containsExactly("COMANDO DA AERONÁUTICA", "GRUPO DE APOIO", "DIVISÃO DE SUPORTE");
    }

    @Test
    void aDistribuicaoESempreOstensivaEOAssuntoEOTitulo() {
        var c = CabecalhoDaNpa.de(documento(SituacaoBcaEnum.NAO_PUBLICADO), CAMPOS, List.of());

        assertThat(c.distribuicao()).isEqualTo("OSTENSIVA");
        assertThat(c.assunto()).isEqualTo("Funcionamento");
        assertThat(c.identificacao()).isEqualTo("NPA-AGO-01");
    }

    @Test
    void aEmissaoEADataDaAprovacaoOuEmBranco() {
        var doc = documento(SituacaoBcaEnum.NAO_PUBLICADO);
        assertThat(CabecalhoDaNpa.de(doc, CAMPOS, List.of()).emissao()).isEqualTo("__/__/____");

        doc.setDtAprovacao(Timestamp.valueOf("2026-03-05 10:00:00"));
        assertThat(CabecalhoDaNpa.de(doc, CAMPOS, List.of()).emissao()).isEqualTo("05/03/2026");
    }

    @Test
    void oFechoUsaLocalEADataDaAprovacaoPorExtenso() {
        var doc = documento(SituacaoBcaEnum.NAO_PUBLICADO);
        assertThat(CabecalhoDaNpa.de(doc, CAMPOS, List.of()).localEData()).isEqualTo("Brasília, ___ de __________ de ____");

        doc.setDtAprovacao(Timestamp.valueOf("2026-12-01 10:00:00"));
        assertThat(CabecalhoDaNpa.de(doc, CAMPOS, List.of()).localEData()).isEqualTo("Brasília, 1 de dezembro de 2026");
    }

    @Test
    void aEfetivacaoEALinhaDaPublicacaoSoExistemDepoisDePublicada() {
        var naoPublicada = CabecalhoDaNpa.de(documento(SituacaoBcaEnum.NAO_PUBLICADO), CAMPOS, List.of());
        assertThat(naoPublicada.efetivacao()).isEqualTo("A ser preenchida na publicação");
        assertThat(naoPublicada.publicadaNo()).isNull();

        var doc = documento(SituacaoBcaEnum.PUBLICADO);
        doc.setBcaReferencia(" 15 ");
        doc.setDtBcaReferencia(Timestamp.valueOf("2026-04-02 08:00:00"));
        var publicada = CabecalhoDaNpa.de(doc, CAMPOS, List.of());

        assertThat(publicada.efetivacao()).isEqualTo("Boletim Interno Ostensivo nº 15, de 2 de abril de 2026");
        assertThat(publicada.publicadaNo()).isEqualTo("(Publicada no Boletim Interno Ostensivo nº 15, de 2 de abril de 2026)");
    }

    @Test
    void publicadaSemNumeroNemDataMostraOsEspacosEmBranco() {
        var c = CabecalhoDaNpa.de(documento(SituacaoBcaEnum.PUBLICADO), CAMPOS, List.of());

        assertThat(c.efetivacao()).isEqualTo("Boletim Interno Ostensivo nº __, de __ de ______ de ____");
    }

    @Test
    void umaNpaRevogadaContinuaMostrandoAPublicacaoOriginal() {
        var doc = documento(SituacaoBcaEnum.REVOGADO);
        doc.setBcaReferencia("15");
        doc.setDtBcaReferencia(Timestamp.valueOf("2026-04-02 08:00:00"));

        assertThat(CabecalhoDaNpa.de(doc, CAMPOS, List.of()).publicadaNo()).contains("nº 15");
    }

    @Test
    void semAnexosDizNaoHa() {
        assertThat(CabecalhoDaNpa.listaDeAnexos(List.of())).isEqualTo("NÃO HÁ");
        assertThat(CabecalhoDaNpa.listaDeAnexos(null)).isEqualTo("NÃO HÁ");
    }

    @Test
    void aListaDeAnexosSegueOFormatoDoLayout() {
        assertThat(CabecalhoDaNpa.listaDeAnexos(List.of(anexo(1, "Organograma")))).isEqualTo("A - Organograma");
        assertThat(CabecalhoDaNpa.listaDeAnexos(List.of(anexo(1, "Organograma"), anexo(2, "Fluxograma"))))
                .isEqualTo("A - Organograma; e B - Fluxograma");
        assertThat(CabecalhoDaNpa.listaDeAnexos(List.of(anexo(1, "X"), anexo(2, "Y"), anexo(3, "Z"))))
                .isEqualTo("A - X; B - Y; e C - Z");
    }

    @Test
    void osAnexosSaoListadosPelaOrdemMesmoQueVenhamDesordenados() {
        assertThat(CabecalhoDaNpa.listaDeAnexos(List.of(anexo(2, "Y"), anexo(1, "X")))).isEqualTo("A - X; e B - Y");
    }
}
