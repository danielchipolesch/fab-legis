package intraer.fablegis.domain.regras;

// Numeração de um elemento da parte normativa, nas regras da espécie que a calculou.
// numero/letra: identidade permanente do elemento na estrutura. label: numero+letra já formatados
// conforme o tipo e a espécie (ex.: romano p/ capítulo, ordinal/cardinal p/ artigo, "1.1.1" numa NPA).
public record ElementoNumeracao(int numero, String letra, String label) {
    public boolean semNumero() {
        return numero <= 0;
    }
}
