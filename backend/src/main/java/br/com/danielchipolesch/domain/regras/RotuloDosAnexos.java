package br.com.danielchipolesch.domain.regras;

// Como os anexos de um documento são rotulados. Num ato normativo o ANEXO I é o sumário + corpo normativo,
// então os anexos de imagem começam no ANEXO II (romano); numa NPA os anexos são rotulados com letras (A, B, C...) e
// listados no campo ANEXOS do cabeçalho.
public interface RotuloDosAnexos {

    // ordem: posição do anexo (Anexo.ordem) -- 1 para o primeiro anexo de imagem. Ex.: "ANEXO II".
    String rotulo(int ordem);
}
