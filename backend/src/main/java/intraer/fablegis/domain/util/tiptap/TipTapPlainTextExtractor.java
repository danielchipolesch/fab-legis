package intraer.fablegis.domain.util.tiptap;

// Extrai só o texto visível de um doc TipTap (sem marcação/atributos) -- usado
// como último recurso quando o frontend não manda um fullTextContent pronto
// (ver DocumentoParteNormativaService.gerarFullTextContent). Sem isso, o
// fallback concatenava o JSON bruto de "conteudo" como se fosse texto, o que
// poluiria a busca full-text (ver tsv_busca em db/migration/V1__initial.sql) com chaves/aspas
// de marcação em vez de palavras pesquisáveis.
public class TipTapPlainTextExtractor {

    private TipTapPlainTextExtractor() {
    }

    public static String extrair(TipTapNode node) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        extrair(node, sb);
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    private static void extrair(TipTapNode node, StringBuilder sb) {
        if (node.getText() != null && !node.getText().isEmpty()) {
            sb.append(node.getText()).append(' ');
        }
        if (node.getContent() != null) {
            for (TipTapNode filho : node.getContent()) {
                extrair(filho, sb);
            }
        }
    }
}
