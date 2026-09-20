package br.com.danielchipolesch.domain.regimes;

import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;

import java.util.List;
import java.util.Map;

// Calcula o rótulo de cada elemento da parte normativa a partir da árvore. A regra de numeração é do
// regime: num ato normativo, romano para capítulo/seção/subseção, ordinal/cardinal para artigo e sufixo de
// letra para o que é incluído por emenda (LC 95/1998); numa NPA, o caminho decimal (1, 1.1, 1.1.1.1).
// Quem só precisa do rótulo (a API de numeração, o salvamento da estrutura) depende desta interface, não
// da regra de nenhum regime.
public interface CalculadoraDeNumeracaoDosElementos {

    // Chave: id do elemento. Só elementos que recebem número entram no mapa.
    Map<Long, ElementoNumeracao> calcular(List<ItemAnexoParteNormativaResponseDto> normativos);
}
