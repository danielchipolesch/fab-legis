package intraer.fablegis.domain.services;

import org.junit.jupiter.api.Test;

// TEMPORÁRIO (diagnóstico do CI): imprime como o FOP escreve "Constituem" na árvore de áreas.
class DiagnosticoFonteTest {

    @Test
    void imprimeAArvoreDeAreasDeUmaPalavraComTi() throws Exception {
        String fo = "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\"><fo:layout-master-set>"
                + "<fo:simple-page-master master-name=\"a\" page-width=\"21cm\" page-height=\"29cm\"><fo:region-body/></fo:simple-page-master>"
                + "</fo:layout-master-set><fo:page-sequence master-reference=\"a\"><fo:flow flow-name=\"xsl-region-body\">"
                + "<fo:block font-family=\"Calibri\" font-size=\"12pt\">Constituem Boletim tim ti</fo:block></fo:flow></fo:page-sequence></fo:root>";
        var saida = new java.io.ByteArrayOutputStream();
        var fopFactory = FopFactoryProvider.get();
        var fop = fopFactory.newFop(org.apache.fop.apps.MimeConstants.MIME_FOP_AREA_TREE, fopFactory.newFOUserAgent(), saida);
        var spf = javax.xml.parsers.SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        var reader = spf.newSAXParser().getXMLReader();
        reader.setContentHandler(fop.getDefaultHandler());
        reader.parse(new org.xml.sax.InputSource(new java.io.StringReader(fo)));
        String xml = saida.toString(java.nio.charset.StandardCharsets.UTF_8);
        int i = xml.indexOf("<word");
        System.out.println("DIAG-FONTE >>> " + xml.substring(Math.max(0, i - 200), Math.min(xml.length(), i + 400)).replace("\n", "\n"));
        var sb = new StringBuilder();
        for (char c : xml.substring(i, Math.min(xml.length(), i + 400)).toCharArray()) sb.append(c < 128 ? String.valueOf(c) : String.format("<U+%04X>", (int) c));
        System.out.println("DIAG-FONTE-CHARS >>> " + sb);
        System.out.println("DIAG-FONTE-FONTS >>> " + System.getProperty("java.version") + " | " + System.getProperty("os.name") + " | " + java.util.Locale.getDefault());
    }
}
