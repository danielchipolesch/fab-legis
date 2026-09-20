package br.com.danielchipolesch.domain.regras;

// Qual conjunto de regras uma espécie normativa segue: como seus elementos são numerados, como o documento é
// criado, exportado e publicado. Cada EspecieNormativa aponta para um TipoDeRegras (EspecieNormativa.tipoDeRegras)
// e todo o resto do sistema pergunta as regras a RegrasDasEspecies em vez de conhecer as espécies. Ver
// docs/arquitetura.md ("Regras por espécie normativa").
//
// ATO_NORMATIVO: DCA, ICA, NSCA... -- produzidos pelas OM mas com âmbito que extrapola a OM; regidos pela
// LC 95/1998, pelo Decreto 12.002/2024 e pela NSCA 5-3 (artigo, parágrafo, inciso; emenda).
//
// NPA: Norma Padrão de Ação, de uso interno da OM, com layout, gramática de elementos e numeração próprios e sem
// alteração (só publicação e revogação) -- ver docs/dominio.md.
public enum TipoDeRegras {
    ATO_NORMATIVO,
    NPA
}
