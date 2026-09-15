// Fonte única de rótulo/ícone/cor por DocumentoStatusEnum -- antes duplicado
// (com divergência real) em StatusBadge.vue, HomePage.vue (STATUS_CFG),
// DocumentoViewerPage.vue e BuscaPage.vue (STATUS_META), cada um mantido à
// mão. Cada situação tem exatamente UMA família de cor, sem repetir --
// MINUTA/EM_REVISAO, EM_PUBLICACAO/PUBLICADO e ANALISE_REVOGACAO/
// EM_REVOGACAO/REVOGADO chegaram a compartilhar cor entre si, o que fazia
// badges de situações diferentes parecerem a mesma coisa à primeira vista.
//
// `bg`/`fg` (par claro/escuro da mesma família) alimenta chips "tonal"
// (StatusBadge.vue, filtros da HomePage); `color` (nome sólido do Quasar, sem
// sufixo de tom) alimenta badges sólidos/ícones (DocumentoViewerPage.vue,
// BuscaPage.vue) -- duas linguagens visuais diferentes, mesma família de cor
// por situação nas duas.
export const STATUS_META = {
  RASCUNHO:          { label: 'Rascunho',              icon: 'mdi-pencil-outline',         bg: 'grey-3',        fg: 'grey-9',        color: 'grey'        },
  MINUTA:            { label: 'Minuta',                icon: 'mdi-file-edit-outline',      bg: 'orange-2',      fg: 'orange-10',     color: 'orange'      },
  EM_REVISAO:        { label: 'Em Revisão',             icon: 'mdi-account-search-outline', bg: 'amber-2',       fg: 'amber-10',      color: 'amber'       },
  APROVADO:          { label: 'Aprovado',               icon: 'mdi-check-circle-outline',   bg: 'green-2',       fg: 'green-10',      color: 'green'       },
  EM_PUBLICACAO:     { label: 'Em Publicação',          icon: 'mdi-timer-sand',             bg: 'blue-2',        fg: 'blue-10',       color: 'blue'        },
  PUBLICADO:         { label: 'Publicado',              icon: 'mdi-publish',                bg: 'indigo-2',      fg: 'indigo-10',     color: 'indigo'      },
  EM_ALTERACAO:      { label: 'Em Alteração',           icon: 'mdi-pencil-lock-outline',    bg: 'deep-orange-2', fg: 'deep-orange-10', color: 'deep-orange' },
  ALTERADO:          { label: 'Alterado',               icon: 'mdi-check-circle-outline',   bg: 'teal-2',        fg: 'teal-10',       color: 'teal'        },
  ANALISE_REVOGACAO: { label: 'Análise de Revogação',   icon: 'mdi-file-search-outline',    bg: 'brown-2',       fg: 'brown-10',      color: 'brown'       },
  EM_REVOGACAO:      { label: 'Em Revogação',           icon: 'mdi-timer-sand',             bg: 'deep-purple-2', fg: 'deep-purple-10', color: 'deep-purple' },
  CANCELADO:         { label: 'Cancelado',              icon: 'mdi-cancel',                 bg: 'red-2',         fg: 'red-10',        color: 'negative'    },
  REVOGADO:          { label: 'Revogado',               icon: 'mdi-file-remove-outline',    bg: 'blue-grey-2',   fg: 'blue-grey-10',  color: 'blue-grey'   },
}

export function statusMeta(status) {
  return STATUS_META[status] ?? { label: status, icon: 'mdi-help', bg: 'grey-3', fg: 'grey-9', color: 'grey' }
}
