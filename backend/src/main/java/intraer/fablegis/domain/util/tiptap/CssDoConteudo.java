package intraer.fablegis.domain.util.tiptap;

// CSS das tabelas e figuras que o TipTapHtmlSerializer emite dentro do conteúdo de um elemento.
//  - TABELAS_E_FIGURAS: a base (grade, cabeçalho, figura), para os layouts de HTML que não trazem a sua (o da NPA); o layout
//    de Espécie Convencional tem as suas regras de base, equivalentes.
//  - NUMERACAO_E_ESTILO_DAS_TABELAS: comum aos dois -- legenda numerada "Tabela N — título" e "Figura N — título" (contadores
//    CSS, como na prévia), cor e estilo da tabela. Espelha frontend/src/css/tabelas.css e a paleta de EstiloDeTabela.
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

    public static final String NUMERACAO_E_ESTILO_DAS_TABELAS = """
            body { counter-reset: figura tabela; }
            figure { counter-increment: figura; }
            .figura-titulo::before { content: "Figura " counter(figura) " \2014  "; }
            .doc-tabela .tabela-titulo { counter-increment: tabela; font-size: 10pt; font-style: italic; margin: 0 0 3pt; text-align: center; text-indent: 0; }
            .doc-tabela .tabela-titulo::before { content: "Tabela " counter(tabela) " \2014  "; }
            .doc-tabela p { display: block; }
            .doc-tabela .tabela-fonte { font-size: 9pt; color: #555; margin: 3pt 0 0; text-align: center; text-indent: 0; }
            .doc-tabela table { --tb-cab: #e8edf6; --tb-borda: #999999; }
            .doc-tabela table[data-cor="cinza"] { --tb-cab: #e6e6e6; }
            .doc-tabela table[data-cor="verde"] { --tb-cab: #e3f0e6; --tb-borda: #8fa896; }
            .doc-tabela table[data-cor="vinho"] { --tb-cab: #f3e3e6; --tb-borda: #a8909a; }
            .doc-tabela table[data-cor="sem"] { --tb-cab: transparent; }
            .doc-tabela table td, .doc-tabela table th { border: 1px solid var(--tb-borda); }
            .doc-tabela table th { background: var(--tb-cab); font-weight: bold; }
            .doc-tabela table[data-estilo="zebrado"] tbody tr:nth-child(even) td { background-color: #f4f6fa; }
            .doc-tabela table[data-estilo="compacto"] td, .doc-tabela table[data-estilo="compacto"] th { padding: 1pt 3pt; font-size: 8.5pt; }
            .doc-tabela table[data-estilo="aberto"] td, .doc-tabela table[data-estilo="aberto"] th { border-left: 0; border-right: 0; }
            """;
}
