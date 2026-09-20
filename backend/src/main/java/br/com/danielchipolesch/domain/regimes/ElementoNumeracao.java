package br.com.danielchipolesch.domain.regimes;

// Numeração de um elemento da parte normativa, no regime que a calculou.
// numero/letra: identidade permanente do elemento na estrutura. label: numero+letra já formatados
// conforme o tipo e o regime (ex.: romano p/ capítulo, ordinal/cardinal p/ artigo, "1.1.1" numa NPA).
public record ElementoNumeracao(int numero, String letra, String label) {
    public boolean semNumero() {
        return numero <= 0;
    }
}
