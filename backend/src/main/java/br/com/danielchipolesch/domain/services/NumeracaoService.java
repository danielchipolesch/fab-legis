package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum.*;

// Numeração dos elementos da parte normativa (capítulo/seção/subseção/artigo),
// conforme o Decreto nº 12.002/2024 art. 9º e a vedação de renumeração da LC
// 95/1998. Extraído de DocumentoFoBuilder (onde a lógica vivia presa ao gerador de
// PDF) para ser reutilizável — usado tanto na geração do PDF quanto exposto via API
// (GET /v1/documentos/{id}/numeracao) para qualquer outro consumidor.
//
// Espelha (e deve continuar espelhando) `renumberElementsEmAlteracao` de
// frontend/src/utils/numbering.js:
// - Capítulo, seção e subseção: numeração local ao pai. Elemento INCLUIDO entre dois
//   elementos ativos (não-INCLUIDO/não-REVOGADO) do mesmo tipo recebe sufixo de letra
//   (ex.: "II-A") sem consumir a contagem; se estiver ao final da sequência, recebe
//   numeração normal.
// - Artigo: numeração GLOBAL (contínua por todo o documento), mesma regra de sufixo.
//   A marca `incluidoPorEmenda` é permanente — não o status ao vivo — para que um
//   artigo incluído por emenda nunca perca seu sufixo de letra mesmo depois de
//   alterado ou revogado.
@Service
public class NumeracaoService {

    private static final Set<ItemAnexoParteNormativaTipoEnum> TIPOS_AGRUPAMENTO =
            Set.of(CAPITULO, SECAO_NORMATIVA, SUBSECAO_NORMATIVA);

    // numero/letra: identidade permanente do elemento na estrutura. label: numero+letra
    // já formatados conforme o tipo (romano p/ agrupamentos, ordinal/cardinal p/ artigo).
    public record ElementoNumeracao(int numero, String letra, String label) {
        public boolean semNumero() { return numero <= 0; }
    }

    // ─── API pública ────────────────────────────────────────────────────────────

    public Map<Long, ElementoNumeracao> calcular(List<ItemAnexoParteNormativaResponseDto> normativos) {
        var flatArtigos = new ArrayList<ItemAnexoParteNormativaResponseDto>();
        collectArtigosFlat(normativos, flatArtigos);
        Map<Long, ElementoNumeracao> resultado = new HashMap<>();
        assignNumbering(normativos, resultado, flatArtigos, new int[]{0});
        return resultado;
    }

    // Parágrafos de UM artigo (numeração local ao artigo, por isso fora de calcular(), que
    // cobre capítulo/seção/subseção/artigo): devolve o número/letra/rótulo de cada um, na
    // mesma ordem da lista ("Parágrafo único", "§ 1º", "§ 2º-A"...).
    //
    // Parágrafo em vigor NUNCA é renumerado (Decreto nº 12.002/2024, art. 14, IV): o
    // incluído por emenda que ainda tem algum parágrafo ORIGINAL depois dele recebe letra
    // (§ 2º-A) sem consumir a contagem; ao final da sequência recebe número normal. Como
    // nos demais níveis, "original" é tudo que não foi incluído por emenda -- inclusive o
    // REVOGADO, que mantém o número que tinha -- e a decisão usa a marca permanente
    // incluidoPorEmenda, não o status ao vivo (alterar um incluído não muda o rótulo dos
    // anteriores). Espelha renumberElementsEmAlteracao (frontend/src/utils/numbering.js).
    // Parágrafo único que passou a "§ Nº" porque o artigo ganhou um 2º parágrafo (o ÚNICO caso de
    // renumeração de parágrafo: Decreto 12.002/2024, art. 14, IV). Nesse caso a linha "Parágrafo
    // único. texto" sai riscada e o texto se repete sob o novo número, com a cláusula de
    // renumeração -- nas prévias, PDF, HTML e no texto sugerido da portaria.
    //
    // Só vale para um único EM VIGOR: num documento ainda NAO_PUBLICADO a numeração é livre, e um
    // único incluído por emenda ainda pendente (nunca publicado) também não estava em vigor.
    // Depois da publicação da alteração a cláusula fica congelada (clausulaRenumeracao != null) e o
    // riscado é permanente.
    public static boolean unicoRenumerado(List<ItemAnexoParteNormativaResponseDto> paragrafos,
                                          ItemAnexoParteNormativaResponseDto paragrafo,
                                          boolean documentoPublicado) {
        if (paragrafo.elementType() != PARAGRAFO_UNICO || paragrafos.size() < 2) return false;
        if (paragrafo.clausulaRenumeracao() != null) return true;
        boolean inclusaoPendente = paragrafo.emendaStatus() == ElementoEmendaStatusEnum.INCLUIDO
                && paragrafo.clausulaEmenda() == null;
        return documentoPublicado && !inclusaoPendente;
    }

    // Cláusula da renumeração de um parágrafo único: a congelada na publicação da alteração ou, se
    // ainda pendente, a ao vivo com o mesmo placeholder XYZ/ABC das demais cláusulas pendentes.
    public static String clausulaRenumeracao(ItemAnexoParteNormativaResponseDto paragrafoUnico) {
        return paragrafoUnico.clausulaRenumeracao() != null ? paragrafoUnico.clausulaRenumeracao()
                : "(redação dada pela Portaria DIRAD n° XYZ, de DD de MÊS de AAAA,"
                  + " publicada no BCA n° ABC, de DD de mês de AAAA)";
    }

    public static List<ElementoNumeracao> numerarParagrafos(List<ItemAnexoParteNormativaResponseDto> paragrafos) {
        var resultado = new ArrayList<ElementoNumeracao>();
        boolean unicoOnly = paragrafos.size() == 1 && paragrafos.get(0).elementType() == PARAGRAFO_UNICO;
        if (unicoOnly) {
            resultado.add(new ElementoNumeracao(0, null, "Parágrafo único"));
            return resultado;
        }
        int pNum = 0, letterIdx = 0;
        for (int i = 0; i < paragrafos.size(); i++) {
            boolean isIncluido = paragrafos.get(i).incluidoPorEmenda();
            boolean atEnd = isIncluido && !temParagrafoOriginalDepois(paragrafos, i);
            String letra = null;
            if (!isIncluido || atEnd) {
                pNum++;
                letterIdx = 0;
            } else {
                letra = letterFor(letterIdx++);
            }
            resultado.add(new ElementoNumeracao(pNum, letra, "§ " + comSufixoLetra(pNum, letra)));
        }
        return resultado;
    }

    private static boolean temParagrafoOriginalDepois(List<ItemAnexoParteNormativaResponseDto> paragrafos, int idx) {
        for (int i = idx + 1; i < paragrafos.size(); i++) {
            if (!paragrafos.get(i).incluidoPorEmenda()) return true;
        }
        return false;
    }

    // Ponto de referência de um artigo para intervalos de sumário (ex.: "13-A").
    public String pontoFinalArtigo(ItemAnexoParteNormativaResponseDto item, Map<Long, ElementoNumeracao> numeracao) {
        var en = numeracao.get(item.id());
        if (en == null || en.semNumero()) return "";
        String base = fmtNum(en.numero());
        return en.letra() != null ? base + "-" + en.letra() : base;
    }

    // Intervalo de artigos de um agrupamento (capítulo/seção/subseção) para o
    // sumário: olha os filhos (estrutura em árvore) E os irmãos seguintes até o
    // próximo agrupamento (estrutura "achatada", onde artigos são irmãos da seção
    // em vez de filhos dela).
    public String intervaloArtigos(ItemAnexoParteNormativaResponseDto item,
                                    List<ItemAnexoParteNormativaResponseDto> siblings,
                                    int idx,
                                    Map<Long, ElementoNumeracao> numeracao) {
        var first = primeiroArtigo(item.children(), numeracao);
        var last  = ultimoArtigo(item.children(), numeracao);
        for (int j = idx + 1; j < siblings.size(); j++) {
            var sib = siblings.get(j);
            if (TIPOS_AGRUPAMENTO.contains(sib.elementType())) break;
            if (sib.elementType() == ARTIGO) {
                int n = numeroDe(sib, numeracao);
                if (n > 0) {
                    if (first == null || n < numeroDe(first, numeracao)) first = sib;
                    if (last  == null || n > numeroDe(last, numeracao))  last  = sib;
                }
            }
        }
        if (first == null) return "";
        if (last == null) last = first;
        String a = pontoFinalArtigo(first, numeracao);
        String b = pontoFinalArtigo(last, numeracao);
        return a.equals(b) ? a : a + "/" + b;
    }

    public boolean temAgrupamento(List<ItemAnexoParteNormativaResponseDto> normativos) {
        return normativos != null && normativos.stream().anyMatch(el -> TIPOS_AGRUPAMENTO.contains(el.elementType()));
    }

    // ─── Formatação numérica (Decreto 12.002/2024 art. 9º) ─────────────────────────

    public static String toRoman(int n) {
        if (n <= 0) return "";
        int[]    vals = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] syms = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        var sb = new StringBuilder();
        for (int i = 0; i < vals.length; i++)
            while (n >= vals[i]) { sb.append(syms[i]); n -= vals[i]; }
        return sb.toString();
    }

    public static String toLetter(int n) { return String.valueOf((char) ('a' + n - 1)); }

    // Separador de milhar (padrão brasileiro) — nunca aplicado a anos.
    public static String comSeparadorMilhar(int n) { return String.format("%,d", n).replace(",", "."); }

    public static String ordinalOrCardinal(int n) { return n <= 9 ? n + "º" : comSeparadorMilhar(n) + "."; }

    public static String fmtNum(int n) { return n <= 9 ? n + "º" : comSeparadorMilhar(n); }

    // ─── Cálculo interno ────────────────────────────────────────────────────────

    private void collectArtigosFlat(List<ItemAnexoParteNormativaResponseDto> items,
                                     List<ItemAnexoParteNormativaResponseDto> out) {
        if (items == null) return;
        for (var item : items) {
            if (item.elementType() == ARTIGO) out.add(item);
            collectArtigosFlat(item.children(), out);
        }
    }

    private boolean hasActiveArtigoAfterGlobal(ItemAnexoParteNormativaResponseDto item,
                                                List<ItemAnexoParteNormativaResponseDto> flatArtigos) {
        int idx = flatArtigos.indexOf(item);
        for (int i = idx + 1; i < flatArtigos.size(); i++) {
            var s = flatArtigos.get(i);
            if (!s.incluidoPorEmenda()) return true;
        }
        return false;
    }

    private boolean hasNonIncludedSameTypeAfter(List<ItemAnexoParteNormativaResponseDto> siblings, int idx,
                                                 ItemAnexoParteNormativaTipoEnum tipo) {
        for (int i = idx + 1; i < siblings.size(); i++) {
            var s = siblings.get(i);
            if (s.elementType() == tipo && !s.incluidoPorEmenda()) return true;
        }
        return false;
    }

    private void assignNumbering(List<ItemAnexoParteNormativaResponseDto> items,
                                  Map<Long, ElementoNumeracao> out,
                                  List<ItemAnexoParteNormativaResponseDto> flatArtigos,
                                  int[] artCounter) {
        if (items == null) return;
        int cap = 0, sec = 0, sub = 0;
        int capLetterIdx = 0, secLetterIdx = 0, subLetterIdx = 0, artLetterIdx = 0;
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            // Marca permanente, não o status ao vivo: um artigo incluído por emenda
            // mantém seu sufixo de letra mesmo depois de ser alterado ou revogado —
            // só assim emendaStatus fica livre para evoluir sem deslocar a numeração
            // sequencial dos artigos seguintes (vedado pela LC 95/1998).
            boolean isIncluido = item.incluidoPorEmenda();
            switch (item.elementType()) {
                case CAPITULO -> {
                    boolean atEnd = isIncluido && !hasNonIncludedSameTypeAfter(items, i, CAPITULO);
                    int n; String letra = null;
                    if (!isIncluido || atEnd) { n = ++cap; capLetterIdx = 0; }
                    else { n = cap; letra = letterFor(capLetterIdx++); }
                    out.put(item.id(), new ElementoNumeracao(n, letra, agrupamentoLabel(n, letra)));
                    assignNumbering(item.children(), out, flatArtigos, artCounter);
                }
                case SECAO_NORMATIVA -> {
                    boolean atEnd = isIncluido && !hasNonIncludedSameTypeAfter(items, i, SECAO_NORMATIVA);
                    int n; String letra = null;
                    if (!isIncluido || atEnd) { n = ++sec; secLetterIdx = 0; }
                    else { n = sec; letra = letterFor(secLetterIdx++); }
                    out.put(item.id(), new ElementoNumeracao(n, letra, agrupamentoLabel(n, letra)));
                    assignNumbering(item.children(), out, flatArtigos, artCounter);
                }
                case SUBSECAO_NORMATIVA -> {
                    boolean atEnd = isIncluido && !hasNonIncludedSameTypeAfter(items, i, SUBSECAO_NORMATIVA);
                    int n; String letra = null;
                    if (!isIncluido || atEnd) { n = ++sub; subLetterIdx = 0; }
                    else { n = sub; letra = letterFor(subLetterIdx++); }
                    out.put(item.id(), new ElementoNumeracao(n, letra, agrupamentoLabel(n, letra)));
                    assignNumbering(item.children(), out, flatArtigos, artCounter);
                }
                case ARTIGO -> {
                    int n; String letra = null;
                    if (!isIncluido) {
                        n = ++artCounter[0];
                        artLetterIdx = 0;
                    } else {
                        boolean atEnd = !hasActiveArtigoAfterGlobal(item, flatArtigos);
                        if (atEnd) { n = ++artCounter[0]; artLetterIdx = 0; }
                        else { n = artCounter[0]; letra = letterFor(artLetterIdx++); }
                    }
                    out.put(item.id(), new ElementoNumeracao(n, letra, artigoLabel(n, letra)));
                    assignNumbering(item.children(), out, flatArtigos, artCounter);
                }
                default -> { }
            }
        }
    }

    public static String letterFor(int idx) {
        return String.valueOf((char) ('A' + idx));
    }

    private static String agrupamentoLabel(int n, String letra) {
        return toRoman(n) + (letra != null ? "-" + letra : "");
    }

    // Sufixo de letra para elemento inserido por emenda entre dois já em vigor
    // (Art. 7º-A, § 2º-A…): com letra, o ponto do cardinal (a partir do 10º)
    // migra para o final, depois da letra — nunca fica entre o número e o
    // hífen. Usado tanto para artigo (aqui) quanto para parágrafo
    // (DocumentoFoCorpoBuilder — parágrafo é numerado local ao artigo, fora
    // do escopo desta classe).
    public static String comSufixoLetra(int n, String letra) {
        if (letra == null) return ordinalOrCardinal(n);
        return n <= 9 ? n + "º-" + letra : comSeparadorMilhar(n) + "-" + letra + ".";
    }

    private static String artigoLabel(int n, String letra) {
        return comSufixoLetra(n, letra);
    }

    private int numeroDe(ItemAnexoParteNormativaResponseDto item, Map<Long, ElementoNumeracao> numeracao) {
        var en = numeracao.get(item.id());
        return en != null ? en.numero() : -1;
    }

    private ItemAnexoParteNormativaResponseDto primeiroArtigo(List<ItemAnexoParteNormativaResponseDto> items,
                                                                Map<Long, ElementoNumeracao> numeracao) {
        if (items == null) return null;
        for (var item : items) {
            if (item.elementType() == ARTIGO && numeroDe(item, numeracao) > 0) return item;
            var found = primeiroArtigo(item.children(), numeracao);
            if (found != null) return found;
        }
        return null;
    }

    private ItemAnexoParteNormativaResponseDto ultimoArtigo(List<ItemAnexoParteNormativaResponseDto> items,
                                                              Map<Long, ElementoNumeracao> numeracao) {
        if (items == null) return null;
        ItemAnexoParteNormativaResponseDto last = null;
        for (var item : items) {
            if (item.elementType() == ARTIGO && numeroDe(item, numeracao) > 0) last = item;
            var childLast = ultimoArtigo(item.children(), numeracao);
            if (childLast != null) last = childLast;
        }
        return last;
    }
}
