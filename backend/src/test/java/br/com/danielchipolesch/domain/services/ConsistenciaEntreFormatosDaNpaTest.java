package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.npaDtos.AssinaturaDaNpaDto;
import br.com.danielchipolesch.application.dtos.npaDtos.CamposDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.regras.npa.CamposDeNpa;
import br.com.danielchipolesch.domain.regras.npa.LeiauteHtmlDeNpa;
import br.com.danielchipolesch.domain.regras.npa.NumeracaoDeNpa;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Regra do CLAUDE.md ("Consistência entre formatos"): cada formato do documento é gerado por um construtor próprio, e
// o que descreve o documento tem de sair igual em todos. Aqui, PDF e HTML da NPA mostram os mesmos textos.
class ConsistenciaEntreFormatosDaNpaTest {

    private static final AtomicLong IDS = new AtomicLong(1);

    private static ItemAnexoParteNormativaResponseDto item(ItemAnexoParteNormativaTipoEnum tipo, String titulo, String texto,
                                                           ItemAnexoParteNormativaResponseDto... filhos) {
        var conteudo = texto == null ? null : "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":"
                + "[{\"type\":\"text\",\"text\":\"" + texto + "\"}]}]}";
        return new ItemAnexoParteNormativaResponseDto(IDS.getAndIncrement(), null, tipo, 1, titulo, conteudo, null,
                ElementoEmendaStatusEnum.INALTERADO, null, null, null, null, null, null, false, null, null, List.of(filhos));
    }

    private static String arvoreDeAreas(String fo) throws Exception {
        var saida = new java.io.ByteArrayOutputStream();
        var fopFactory = FopFactoryProvider.get();
        var fop = fopFactory.newFop(org.apache.fop.apps.MimeConstants.MIME_FOP_AREA_TREE, fopFactory.newFOUserAgent(), saida);
        var spf = javax.xml.parsers.SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        var reader = spf.newSAXParser().getXMLReader();
        reader.setContentHandler(fop.getDefaultHandler());
        reader.parse(new org.xml.sax.InputSource(new java.io.StringReader(fo)));
        return saida.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String textoCorrido(String marcado) {
        return marcado.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replaceAll("\\s+", " ");
    }

    @Test
    void pdfEHtmlMostramOsMesmosTextosDoCabecalhoDoCorpoEDoFecho() throws Exception {
        var campos = mock(CamposDeNpa.class);
        when(campos.camposParaLeiaute(anyLong())).thenReturn(new CamposDaNpaDto("DIVISÃO DE SUPORTE", "Brasília",
                List.of(new AssinaturaDaNpaDto("Elaborado por", List.of("FULANO", "Major")))));
        var mapper = new ObjectMapper();
        var pdf = new DocumentoFoNpaBuilder(mapper, null, new NumeracaoDeNpa(), campos);
        var html = new LeiauteHtmlDeNpa(mapper, null, new NumeracaoDeNpa(), campos);

        var especie = new EspecieNormativa();
        var om = new OrganizacaoMilitar();
        om.setNome("Grupo de Apoio");
        var doc = new Documento();
        doc.setId(1L);
        doc.setEspecieNormativa(especie);
        doc.setIdentificacao("NPA-AGO-01");
        doc.setTituloDocumento("Funcionamento da Divisão");
        doc.setOm(om);
        doc.setSituacaoBca(SituacaoBcaEnum.PUBLICADO);
        doc.setSituacaoLocal(SituacaoLocalEnum.SEM_ETAPA);
        doc.setDtAprovacao(Timestamp.valueOf("2026-03-12 10:00:00"));
        doc.setBcaReferencia("Boletim Interno Ostensivo nº 15, de 2 de abril de 2026");
        doc.setDtBcaReferencia(Timestamp.valueOf("2026-04-02 08:00:00"));
        var normativos = List.of(
                item(CAPITULO, "Disposições Preliminares", null,
                        item(SECAO_NORMATIVA, "Referências", null,
                                item(PARAGRAFO, null, "Constituem referências:",
                                        item(ALINEA, null, "a Constituição Federal;")))),
                item(CAPITULO, "Disposições Finais", null, item(PARAGRAFO, null, "Casos omissos.")));
        var anexos = List.of(new AnexoResponseDto(1L, "Organograma", null, 1));

        var textoPdf = textoCorrido(arvoreDeAreas(pdf.gerarFo(doc, List.of(), normativos, anexos)));
        var textoHtml = textoCorrido(html.gerarHtml(doc, List.of(), normativos, anexos));

        var deveAparecerNosDois = List.of(
                "COMANDO DA AERONÁUTICA", "GRUPO DE APOIO", "DIVISÃO DE SUPORTE", "NPA-AGO-01", "OSTENSIVA",
                "Funcionamento da Divisão", "12 MAR 2026", "BIO 15", "02 ABR 2026",
                "A - Organograma.", "ANEXO A", "ORGANOGRAMA",
                "1 DISPOSIÇÕES PRELIMINARES", "1.1 REFERÊNCIAS", "1.1.1 Constituem referências:",
                "a) a Constituição Federal;", "2 DISPOSIÇÕES FINAIS", "2.1 Casos omissos.",
                "Brasília, 12 de março de 2026", "Elaborado por", "FULANO", "Major",
                "(Publicada no Boletim Interno Ostensivo nº 15, de 2 de abril de 2026)");
        for (var texto : deveAparecerNosDois) {
            assertThat(textoPdf).as("PDF: " + texto).contains(texto);
            assertThat(textoHtml).as("HTML: " + texto).contains(texto);
        }
    }
}
