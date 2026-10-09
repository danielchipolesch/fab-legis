package intraer.fablegis.domain.services;

import intraer.fablegis.domain.handlers.exceptions.FalhaNaRenderizacaoException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

// Fachada sobre o Apache FOP: do texto XSL-FO para os bytes do PDF. Todo gerador de PDF do sistema (DocumentoPdfService,
// MapaAlteracaoPdfService) passa por aqui, em vez de repetir a montagem do FOP/SAX -- e o endurecimento do parser, que agora
// vive num lugar só: o FO é gerado pelo sistema, mas leva texto digitado por pessoas, então o parser nunca aceita
// declaração DOCTYPE nem entidades externas (XXE).
final class RenderizadorDePdf {

    private RenderizadorDePdf() {
    }

    // descricao: o que está sendo gerado, para a mensagem de erro ("PDF", "PDF do mapa de alteração"...).
    static byte[] renderizar(String fo, String descricao) {
        try (var saida = new ByteArrayOutputStream()) {
            var fabrica = FopFactoryProvider.get();
            Fop fop = fabrica.newFop(MimeConstants.MIME_PDF, fabrica.newFOUserAgent(), saida);
            XMLReader leitor = leitorSeguro();
            leitor.setContentHandler(fop.getDefaultHandler());
            leitor.parse(new InputSource(new StringReader(fo)));
            return saida.toByteArray();
        } catch (Exception e) {
            throw new FalhaNaRenderizacaoException("Erro ao renderizar " + descricao + ": " + e.getMessage(), e);
        }
    }

    private static XMLReader leitorSeguro() throws ParserConfigurationException, SAXException {
        var spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return spf.newSAXParser().getXMLReader();
    }
}
