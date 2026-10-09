package intraer.fablegis.domain.services;

import intraer.fablegis.domain.handlers.exceptions.FalhaNaRenderizacaoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// A fachada do FOP (docs/exportacao-pdf.md): XSL-FO em, PDF fora; o parser nunca aceita DOCTYPE nem entidades externas
// (XXE), porque o FO leva texto digitado por pessoas.
class RenderizadorDePdfTest {

    private static final String FO_MINIMO = """
            <?xml version="1.0" encoding="UTF-8"?>
            <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
              <fo:layout-master-set>
                <fo:simple-page-master master-name="p" page-width="21cm" page-height="29.7cm">
                  <fo:region-body/>
                </fo:simple-page-master>
              </fo:layout-master-set>
              <fo:page-sequence master-reference="p">
                <fo:flow flow-name="xsl-region-body"><fo:block>%s</fo:block></fo:flow>
              </fo:page-sequence>
            </fo:root>
            """;

    @Test
    void transformaOXslFoEmUmPdf() {
        byte[] pdf = RenderizadorDePdf.renderizar(FO_MINIMO.formatted("Olá"), "PDF");

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void recusaUmaDeclaracaoDoctype() {
        var comEntidadeExterna = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE fo:root [<!ENTITY segredo SYSTEM "file:///etc/passwd">]>
                """ + FO_MINIMO.formatted("&segredo;").replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "");

        assertThatThrownBy(() -> RenderizadorDePdf.renderizar(comEntidadeExterna, "PDF"))
                .isInstanceOf(FalhaNaRenderizacaoException.class)
                .hasMessageStartingWith("Erro ao renderizar PDF:");
    }

    @Test
    void umXmlInvalidoDizOQueEstavaSendoGerado() {
        assertThatThrownBy(() -> RenderizadorDePdf.renderizar("<fo:root>", "PDF do mapa de alteração"))
                .isInstanceOf(FalhaNaRenderizacaoException.class)
                .hasMessageStartingWith("Erro ao renderizar PDF do mapa de alteração:");
    }
}
