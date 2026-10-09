package intraer.fablegis.domain.entities.estruturaDocumento;

// Orientação da página A4 em que um anexo de imagem é impresso no PDF (e mostrado na prévia). Uma imagem larga
// (organograma, fluxograma, tabela) cabe melhor deitada. O HTML não tem páginas, então não se aplica a ele.
public enum OrientacaoDoAnexo {
    RETRATO,
    PAISAGEM;

    // Regra de sugestão: paisagem se a imagem é mais larga que alta; quadrada e dimensões inválidas ficam em retrato.
    // O frontend espelha esta regra (utils/orientacaoDoAnexo.js) para pré-selecionar o seletor no upload.
    public static OrientacaoDoAnexo sugeridaPara(int largura, int altura) {
        return largura > 0 && altura > 0 && largura > altura ? PAISAGEM : RETRATO;
    }
}
