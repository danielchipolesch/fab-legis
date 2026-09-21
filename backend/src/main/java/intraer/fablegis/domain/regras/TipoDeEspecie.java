package intraer.fablegis.domain.regras;

// O tipo da espécie normativa, como a NSCA 5-3 as divide: define qual conjunto de regras ela segue -- como seus
// elementos são numerados, como o documento é criado, exportado e publicado. Cada EspecieNormativa aponta para um
// TipoDeEspecie (EspecieNormativa.tipoDeEspecie) e todo o resto do sistema pergunta as regras a RegrasDasEspecies em vez
// de conhecer as espécies. Ver docs/arquitetura.md ("Regras por espécie normativa").
//
// CONVENCIONAL: as Espécies Convencionais (MCA, NSCA, ICA, ROCA, DCA...) -- produzidas pelas OM mas com âmbito que
// extrapola a OM; regidas pela LC 95/1998, pelo Decreto 12.002/2024 e pela NSCA 5-3 (artigo, parágrafo, inciso; emenda).
//
// COMUNICACAO_OFICIAL_PADRONIZADA: as Espécies de Comunicações Oficiais Padronizadas (NSCA 5-3, Capítulo VIII,
// Seção VIII), de uso interno da OM -- hoje a NPA (Norma Padrão de Ação), com layout, gramática de elementos e
// numeração próprios e sem alteração (só publicação e revogação). Ver docs/dominio.md.
public enum TipoDeEspecie {
    CONVENCIONAL,
    COMUNICACAO_OFICIAL_PADRONIZADA
}
