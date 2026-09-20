package br.com.danielchipolesch.domain.util.tiptap;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * O texto entre colchetes ("[descrever a finalidade da publicação]") é o que o modelo de novo
 * documento deixa para o autor preencher: aparece em vermelho no PDF, no HTML e na prévia
 * (docs/exportacao-pdf.md). É decidido na hora de renderizar, não gravado no conteúdo -- assim o
 * vermelho some sozinho quando o autor substitui o trecho e apaga os colchetes.
 */
public final class TextoEntreColchetes {

    public static final String COR = "#FF0000";

    private static final Pattern PADRAO = Pattern.compile("\\[[^\\[\\]]*\\]");

    private TextoEntreColchetes() {}

    public record Trecho(String texto, boolean entreColchetes) {}

    // Divide o texto em trechos, marcando os que estão entre colchetes (colchetes incluídos).
    // Sem nenhum colchete casado, devolve um único trecho.
    public static List<Trecho> dividir(String texto) {
        var trechos = new ArrayList<Trecho>();
        if (texto == null || texto.isEmpty()) return trechos;
        var m = PADRAO.matcher(texto);
        int fim = 0;
        while (m.find()) {
            if (m.start() > fim) trechos.add(new Trecho(texto.substring(fim, m.start()), false));
            trechos.add(new Trecho(m.group(), true));
            fim = m.end();
        }
        if (fim < texto.length()) trechos.add(new Trecho(texto.substring(fim), false));
        return trechos;
    }
}
