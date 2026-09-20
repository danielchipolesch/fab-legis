package br.com.danielchipolesch.domain.regimes;

// O REGIME NORMATIVO é o conjunto de regras a que uma espécie normativa obedece: como seus elementos
// são numerados, como o documento é criado, exportado e publicado. Cada espécie aponta para um regime
// (EspecieNormativa.regime) e todo o resto do sistema pergunta ao regime -- via RegimesNormativos --
// em vez de conhecer as espécies. Ver docs/arquitetura.md ("Regimes normativos").
//
// ATO_NORMATIVO: DCA, ICA, NSCA... -- produzidos pelas OM mas com âmbito que extrapola a OM; regidos
// pela LC 95/1998, pelo Decreto 12.002/2024 e pela NSCA 5-3 (artigo, parágrafo, inciso; emenda).
//
// Ainda por vir (ver docs/roadmap.md): NPA -- Norma Padrão de Ação, de uso interno da OM, com layout,
// gramática de elementos e numeração próprios e sem alteração (só publicação e revogação).
public enum RegimeNormativo {
    ATO_NORMATIVO
}
