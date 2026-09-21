package intraer.fablegis.domain.util.tiptap;

import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Monta o conteúdo TipTap (JSON) de um elemento dos modelos de documento novo, com o texto de orientação -- os
// trechos entre colchetes, colchetes incluídos -- em vermelho, para chamar a atenção do autor para o que ele precisa
// substituir. É uma cor comum do TipTap (mark textStyle), gravada só no modelo do documento novo: daí em diante o
// texto é do autor, que mantém ou muda a cor pelo próprio editor. Mesmo formato que o editor grava para um
// elemento (ver makeNormEl em frontend/src/stores/editor.js).
public final class ConteudoTipTapComOrientacao {

    public static final String COR_ORIENTACAO = "#FF0000";

    private static final Pattern TRECHO_ENTRE_COLCHETES = Pattern.compile("\\[[^\\[\\]]*\\]");

    private ConteudoTipTapComOrientacao() {
    }

    public static String de(String texto, ObjectMapper objectMapper) {
        var trechos = new ArrayList<Map<String, Object>>();
        var m = TRECHO_ENTRE_COLCHETES.matcher(texto);
        int fim = 0;
        while (m.find()) {
            if (m.start() > fim) trechos.add(trecho(texto.substring(fim, m.start()), false));
            trechos.add(trecho(m.group(), true));
            fim = m.end();
        }
        if (fim < texto.length()) trechos.add(trecho(texto.substring(fim), false));
        return objectMapper.writeValueAsString(Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", trechos))));
    }

    private static Map<String, Object> trecho(String texto, boolean orientacao) {
        if (!orientacao) return Map.of("type", "text", "text", texto);
        return Map.of("type", "text", "text", texto,
                "marks", List.of(Map.of("type", "textStyle", "attrs", Map.of("color", COR_ORIENTACAO))));
    }
}
