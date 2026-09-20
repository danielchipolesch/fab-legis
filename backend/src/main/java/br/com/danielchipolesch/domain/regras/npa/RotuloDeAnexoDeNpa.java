package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.domain.regras.RotuloDosAnexos;
import org.springframework.stereotype.Component;

// Numa NPA os anexos ficam ao final do documento, rotulados A, B, C... (e listados no campo ANEXOS do cabeçalho).
// Não há "ANEXO I" reservado ao corpo normativo: o primeiro anexo (ordem 1) é o ANEXO A.
@Component
public class RotuloDeAnexoDeNpa implements RotuloDosAnexos {

    @Override
    public String rotulo(int ordem) {
        return "ANEXO " + letra(ordem);
    }

    // Só a letra ("A", "B"...): usada também na lista do cabeçalho ("A - Título; B - Título; e C - Título").
    public static String letra(int ordem) {
        var sb = new StringBuilder();
        for (int n = ordem; n > 0; n = (n - 1) / 26) {
            sb.insert(0, (char) ('A' + (n - 1) % 26));
        }
        return sb.toString();
    }
}
