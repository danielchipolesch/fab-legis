package intraer.fablegis.domain.util.tiptap;

import java.util.ArrayList;
import java.util.List;

// A "planta" de uma tabela TipTap (nó `table`): em que coluna cada célula realmente começa -- levando em conta as células
// mescladas (colspan/rowspan), que fazem uma linha ter menos células que a tabela tem colunas --, quantas colunas a tabela tem
// de verdade, a largura que o usuário deu a cada coluna ao arrastá-la no editor (`colwidth`, em px) e quantas linhas do início
// são cabeçalho. É a mesma conta para o PDF (XslFoContentRenderer) e para o HTML (TipTapHtmlSerializer), para os dois
// formatos lerem a tabela do mesmo jeito.
final class GradeDeTabela {

    // coluna: índice (0...) da primeira coluna que a célula ocupa. Os spans já vêm com o mínimo de 1.
    record Celula(TipTapNode no, int linha, int coluna, int colspan, int rowspan, boolean cabecalho) {}

    private final List<List<Celula>> linhas;
    private final int colunas;
    // Largura de cada coluna em px, ou 0 quando o usuário não arrastou aquela coluna.
    private final double[] larguras;
    private final int linhasDeCabecalho;

    private GradeDeTabela(List<List<Celula>> linhas, int colunas, double[] larguras, int linhasDeCabecalho) {
        this.linhas = linhas;
        this.colunas = colunas;
        this.larguras = larguras;
        this.linhasDeCabecalho = linhasDeCabecalho;
    }

    static GradeDeTabela de(TipTapNode tabela) {
        var linhasDaTabela = new ArrayList<TipTapNode>();
        if (tabela != null && tabela.getContent() != null) {
            for (var linha : tabela.getContent()) {
                if ("tableRow".equals(linha.getType())) linhasDaTabela.add(linha);
            }
        }

        var celulas = new ArrayList<List<Celula>>();
        var ocupadaAte = new ArrayList<Integer>();   // por coluna: até que linha (exclusive) um rowspan ainda a ocupa
        var larguras = new ArrayList<Double>();
        int colunas = 0;

        for (int r = 0; r < linhasDaTabela.size(); r++) {
            var daLinha = new ArrayList<Celula>();
            int c = 0;
            var conteudo = linhasDaTabela.get(r).getContent();
            if (conteudo != null) {
                for (var no : conteudo) {
                    while (c < ocupadaAte.size() && ocupadaAte.get(c) > r) c++;
                    int colspan = Math.max(1, no.getAttrInt("colspan", 1));
                    int rowspan = Math.max(1, no.getAttrInt("rowspan", 1));
                    var larguraDasColunas = larguraDasColunas(no);
                    for (int k = 0; k < colspan; k++) {
                        while (ocupadaAte.size() <= c + k) { ocupadaAte.add(0); larguras.add(0.0); }
                        ocupadaAte.set(c + k, r + rowspan);
                        if (k < larguraDasColunas.size() && larguras.get(c + k) == 0.0) larguras.set(c + k, larguraDasColunas.get(k));
                    }
                    daLinha.add(new Celula(no, r, c, colspan, rowspan, "tableHeader".equals(no.getType())));
                    c += colspan;
                }
            }
            colunas = Math.max(colunas, Math.max(c, ocupadaAte.size()));
            celulas.add(daLinha);
        }
        // Linhas que só têm células cobertas por rowspan de cima não existem aqui; o tamanho final vem do que foi ocupado.
        colunas = Math.max(colunas, ocupadaAte.size());

        double[] vetor = new double[colunas];
        for (int i = 0; i < colunas && i < larguras.size(); i++) vetor[i] = larguras.get(i);
        return new GradeDeTabela(celulas, colunas, vetor, linhasDeCabecalho(celulas));
    }

    // `colwidth` é a lista de larguras (px) das colunas que a célula cobre, ou ausente.
    private static List<Double> larguraDasColunas(TipTapNode celula) {
        var resultado = new ArrayList<Double>();
        if (celula.getAttrs() != null && celula.getAttrs().get("colwidth") instanceof List<?> lista) {
            for (Object o : lista) {
                resultado.add(o instanceof Number n && n.doubleValue() > 0 ? n.doubleValue() : 0.0);
            }
        }
        return resultado;
    }

    // As linhas do início cujas células são todas de cabeçalho (tableHeader) repetem no topo de cada página do PDF. Só vale se
    // nenhuma célula desse trecho se estende (rowspan) para além dele -- senão repetir o cabeçalho partiria a célula -- e se
    // sobra ao menos uma linha de corpo.
    private static int linhasDeCabecalho(List<List<Celula>> celulas) {
        int k = 0;
        while (k < celulas.size() && !celulas.get(k).isEmpty() && celulas.get(k).stream().allMatch(Celula::cabecalho)) k++;
        if (k == 0 || k == celulas.size()) return 0;
        for (int r = 0; r < k; r++) {
            for (var celula : celulas.get(r)) {
                if (r + celula.rowspan() > k) return 0;
            }
        }
        return k;
    }

    List<List<Celula>> linhas() {
        return linhas;
    }

    int colunas() {
        return colunas;
    }

    int linhasDeCabecalho() {
        return linhasDeCabecalho;
    }

    // O usuário arrastou ao menos uma coluna.
    boolean temLarguras() {
        for (double l : larguras) if (l > 0) return true;
        return false;
    }

    // Largura relativa de cada coluna (px). As que o usuário não arrastou recebem a média das que ele arrastou (ou 1, todas
    // iguais, se nenhuma foi) -- só a proporção importa: o PDF e o HTML ajustam o total à largura disponível.
    double[] largurasRelativas() {
        double soma = 0;
        int conhecidas = 0;
        for (double l : larguras) if (l > 0) { soma += l; conhecidas++; }
        double padrao = conhecidas == 0 ? 1 : soma / conhecidas;
        double[] relativas = new double[colunas];
        for (int i = 0; i < colunas; i++) relativas[i] = larguras[i] > 0 ? larguras[i] : padrao;
        return relativas;
    }

    // Fração (0 a 1) da largura da tabela que a célula ocupa, somando as colunas que ela cobre.
    double fracaoDaLargura(Celula celula) {
        double[] relativas = largurasRelativas();
        double total = 0, daCelula = 0;
        for (int i = 0; i < relativas.length; i++) {
            total += relativas[i];
            if (i >= celula.coluna() && i < celula.coluna() + celula.colspan()) daCelula += relativas[i];
        }
        return total == 0 ? 1.0 / Math.max(1, colunas) : daCelula / total;
    }
}
