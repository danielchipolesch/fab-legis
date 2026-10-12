package intraer.fablegis.domain.util.tiptap;

// CSS das tabelas e figuras que o TipTapHtmlSerializer emite dentro do conteúdo de um elemento, comum aos layouts de HTML que
// não trazem o seu próprio (o da NPA). O layout de Espécie Convencional tem as suas regras próprias, equivalentes.
public final class CssDoConteudo {

    private CssDoConteudo() {}

    public static final String TABELAS_E_FIGURAS = """
            table:not(.cabecalho) { border-collapse: collapse; table-layout: fixed; width: 100%; margin: 4pt 0; font-size: 10pt; }
            table:not(.cabecalho) td, table:not(.cabecalho) th { border: 1px solid #999; padding: 3pt 6pt; vertical-align: top; }
            table:not(.cabecalho) th { background: rgba(11,61,145,0.06); font-weight: bold; text-align: center; }
            table:not(.cabecalho) p { margin: 0; text-indent: 0; display: block; }
            tr { page-break-inside: avoid; }
            thead { display: table-header-group; }
            figure { display: block; text-align: center; margin: 10pt auto; max-width: 100%; page-break-inside: avoid; }
            figure img { max-width: 100%; height: auto; max-height: 500pt; display: block; margin: 0 auto; }
            .figura-titulo { font-size: 10pt; font-style: italic; margin: 0; text-align: center; }
            .figura-fonte  { font-size: 9pt; color: #555; margin: 3pt 0 0; text-align: center; }
            """;
}
