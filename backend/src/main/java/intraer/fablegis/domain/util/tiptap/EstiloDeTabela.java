package intraer.fablegis.domain.util.tiptap;

import java.util.Map;
import java.util.regex.Pattern;

// Estilo e cor de uma tabela (atributos `estilo` e `cor` do nó `table`; `fundo` e `alinhamentoVertical` das células). A paleta e
// a lista de valores espelham frontend/src/utils/estiloDeTabela.js -- o editor, a prévia, o PDF e o HTML exportado leem os
// mesmos valores. Um valor desconhecido (documento de outra versão, dado corrompido) nunca vira estilo estranho: cai no padrão.
final class EstiloDeTabela {

    private EstiloDeTabela() {}

    record Cor(String cabecalho, String borda) {}

    static final String PADRAO = "padrao";
    static final String ZEBRADO = "zebrado";
    static final String COMPACTO = "compacto";
    static final String ABERTO = "aberto";

    static final String COR_DA_ZEBRA = "#f4f6fa";

    private static final Map<String, Cor> CORES = Map.of(
            "azul", new Cor("#e8edf6", "#999999"),
            "cinza", new Cor("#e6e6e6", "#999999"),
            "verde", new Cor("#e3f0e6", "#8fa896"),
            "vinho", new Cor("#f3e3e6", "#a8909a"),
            "sem", new Cor("transparent", "#999999"));

    private static final Pattern HEXADECIMAL = Pattern.compile("^#[0-9a-fA-F]{6}$");

    static Cor cor(String valor) {
        return CORES.get(nomeDaCor(valor));
    }

    // O nome da cor se for um dos conhecidos; senão a cor padrão.
    static String nomeDaCor(String valor) {
        return valor != null && CORES.containsKey(valor) ? valor : "azul";
    }

    static String estilo(String valor) {
        return ZEBRADO.equals(valor) || COMPACTO.equals(valor) || ABERTO.equals(valor) ? valor : PADRAO;
    }

    // Cor de fundo da célula: só #RRGGBB (o valor vai parar em atributo do FO e em `style` do HTML). Senão, null.
    static String fundo(String valor) {
        return valor != null && HEXADECIMAL.matcher(valor).matches() ? valor.toLowerCase() : null;
    }

    // top | middle | bottom
    static String alinhamentoVertical(String valor) {
        return "middle".equals(valor) || "bottom".equals(valor) ? valor : "top";
    }

    // Alinhamento vertical no XSL-FO (display-align da fo:table-cell).
    static String displayAlignDoFo(String alinhamentoVertical) {
        return switch (alinhamentoVertical(alinhamentoVertical)) {
            case "middle" -> "center";
            case "bottom" -> "after";
            default -> "before";
        };
    }

    // A tabela só tem legenda numerada ("Tabela N — título") quando tem título; a fonte sozinha aparece sem número.
    static boolean temTitulo(String titulo) {
        return titulo != null && !titulo.isBlank();
    }
}
