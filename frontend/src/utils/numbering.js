export function toRoman(n) {
  const vals = [1000,900,500,400,100,90,50,40,10,9,5,4,1]
  const syms = ['M','CM','D','CD','C','XC','L','XL','X','IX','V','IV','I']
  let result = ''
  for (let i = 0; i < vals.length; i++) {
    while (n >= vals[i]) { result += syms[i]; n -= vals[i] }
  }
  return result
}

export function toLetter(n) {
  return String.fromCharCode(96 + n)
}

// Separador de milhar (padrão brasileiro) — nunca aplicado a anos.
function comSeparadorMilhar(n) {
  return n.toLocaleString('pt-BR')
}

// Decreto 12.002/2024 Art. 9 deg I e VII: ordinal (grau) ate o 9, cardinal (.) a partir do 10.
function ordinalOrCardinal(n) {
  return n <= 9 ? `${n}` + '\xBA' : `${comSeparadorMilhar(n)}.`
}

// Sufixo de letra para elementos inseridos por emenda (Art. 7°-A, Art. 27-A., …)
// Quando há letra, o ponto do cardinal (a partir do 10°) migra para o final,
// depois da letra — nunca fica entre o número e o hífen.
function ordinalWithLetra(n, letra) {
  if (!letra) return ordinalOrCardinal(n)
  if (n <= 9) return `${n}\xBA-${letra}`
  return `${comSeparadorMilhar(n)}-${letra}.`
}

export function formatLabel(element) {
  const letra = element._emendaLetra ?? null
  switch (element.tipo) {
    case 'capitulo': {
      const t = element.titulo ? ' — ' + element.titulo.toUpperCase() : ''
      return 'CAP\xCDTULO ' + toRoman(element.numero ?? 0) + (letra ? '-' + letra : '') + t
    }
    case 'secao_normativa': {
      const t = element.titulo ? ' — ' + element.titulo : ''
      return 'Se\xE7\xE3o ' + toRoman(element.numero ?? 0) + (letra ? '-' + letra : '') + t
    }
    case 'subsecao_normativa': {
      const t = element.titulo ? ' — ' + element.titulo : ''
      return 'Subse\xE7\xE3o ' + toRoman(element.numero ?? 0) + (letra ? '-' + letra : '') + t
    }
    case 'artigo':          return 'Art. ' + ordinalWithLetra(element.numero ?? 0, letra)
    case 'paragrafo_unico': return 'Par\xE1grafo \xFAnico'
    case 'paragrafo':       return '\xA7 ' + ordinalWithLetra(element.numero ?? 0, letra)
    case 'inciso':          return toRoman(element.numero ?? 0)
    case 'alinea':          return toLetter(element.numero ?? 1) + ')'
    case 'sub_alinea':      return (element.numero ?? 1) + '.'
    case 'epigrafe':             return 'Ep\xEDgrafe'
    case 'ementa':               return 'Ementa'
    case 'preambulo':            return 'Pre\xE2mbulo'
    case 'fundamentacao':        return 'Fundamenta\xE7\xE3o'
    case 'clausula_revogatoria': return 'Cl\xE1usula Revogat\xF3ria'
    case 'clausula_vigencia':    return 'Cl\xE1usula de Vig\xEAncia'
    case 'fecho':                return 'Fecho'
    case 'assinatura':           return 'Assinatura'
    case 'referenda':            return 'Referenda'
    default: return element.tipo
  }
}

// Rótulo para a coluna "Referência" do quadro comparativo/PDF de justificativas
// (Mapa de Alteração) — não é texto de corpo normativo, então incisos, alíneas e
// subalíneas precisam do nome por extenso ("Inciso III", "Alínea c)") para o leitor
// identificar o elemento fora do contexto do artigo. No corpo do documento em si
// (formatLabel/bodyLabel) isso NUNCA aparece — a LC 95/1998 não usa esses rótulos.
export function formatReferenciaLabel(element) {
  switch (element.tipo) {
    case 'inciso':     return 'Inciso ' + formatLabel(element)
    case 'alinea':     return 'Alínea ' + formatLabel(element)
    case 'sub_alinea': return 'Subalínea ' + formatLabel(element)
    default:            return formatLabel(element)
  }
}

export function elementIcon(tipo) {
  const icons = {
    capitulo:           'mdi-folder-outline',
    secao_normativa:    'mdi-folder-open-outline',
    subsecao_normativa: 'mdi-folder-minus-outline',
    artigo:             'mdi-format-list-numbered',
    paragrafo_unico:    'mdi-format-paragraph',
    paragrafo:          'mdi-format-paragraph',
    inciso:             'mdi-format-indent-increase',
    alinea:             'mdi-circle-small',
    sub_alinea:         'mdi-minus',
    epigrafe:           'mdi-tag-outline',
    ementa:             'mdi-text-short',
    preambulo:          'mdi-text',
    fundamentacao:      'mdi-scale-balance',
    clausula_revogatoria: 'mdi-delete-outline',
    clausula_vigencia:  'mdi-calendar-check-outline',
    fecho:              'mdi-handshake-outline',
    assinatura:         'mdi-draw',
    referenda:          'mdi-check-decagram-outline',
  }
  return icons[tipo] ?? 'mdi-file-document-outline'
}

/**
 * Renumbers all elements recursively.
 * Artigos: numbered GLOBALLY across the entire parte_normativa.
 * All other counters reset per parent.
 */
export function renumberElements(elements, _ctx = null) {
  if (!elements?.length) return

  const ctx = _ctx ?? { artCount: 0 }

  let capCount = 0, secCount = 0, subSecCount = 0
  let incisoCount = 0, alineaCount = 0, subAlineaCount = 0
  const paragrafos = []

  for (const el of elements) {
    switch (el.tipo) {
      case 'capitulo':
        capCount++
        el.numero = capCount
        renumberElements(el.filhos, ctx)
        break
      case 'secao_normativa':
        secCount++
        el.numero = secCount
        renumberElements(el.filhos, ctx)
        break
      case 'subsecao_normativa':
        subSecCount++
        el.numero = subSecCount
        renumberElements(el.filhos, ctx)
        break
      case 'artigo':
        ctx.artCount++
        el.numero = ctx.artCount
        renumberElements(el.filhos, null)
        break
      case 'paragrafo':
      case 'paragrafo_unico':
        paragrafos.push(el)
        renumberElements(el.filhos, null)
        break
      case 'inciso':
        incisoCount++
        el.numero = incisoCount
        renumberElements(el.filhos, null)
        break
      case 'alinea':
        alineaCount++
        el.numero = alineaCount
        renumberElements(el.filhos, null)
        break
      case 'sub_alinea':
        subAlineaCount++
        el.numero = subAlineaCount
        renumberElements(el.filhos, null)
        break
    }
  }

  // Paragrafo unico permanece unico somente quando for o unico paragrafo do artigo
  // e ja tiver sido criado como tal. Caso contrario, todos viram paragrafos numerados.
  const unicoOnly = paragrafos.length === 1 && paragrafos[0].tipo === 'paragrafo_unico'
  if (unicoOnly) {
    paragrafos[0].numero = null
  } else if (paragrafos.length > 0) {
    let pNum = 0
    for (const p of paragrafos) { pNum++; p.tipo = 'paragrafo'; p.numero = pNum }
  }
}

const HIERARCHY = ['artigo', 'paragrafo', 'inciso', 'alinea', 'sub_alinea']

// paragrafo_unico ocupa o mesmo nível que paragrafo na hierarquia
function hierarchyBase(tipo) {
  return tipo === 'paragrafo_unico' ? 'paragrafo' : tipo
}

export function promoteType(tipo) {
  const idx = HIERARCHY.indexOf(hierarchyBase(tipo))
  if (idx <= 0) return tipo
  return HIERARCHY[idx - 1]
}

export function demoteType(tipo) {
  const idx = HIERARCHY.indexOf(hierarchyBase(tipo))
  if (idx < 0 || idx >= HIERARCHY.length - 1) return tipo
  return HIERARCHY[idx + 1]
}

/**
 * Renumeração para documentos em EM_ALTERACAO.
 *
 * Regras (LGCP):
 * - Artigos e unidades SUPERIORES ao artigo (capítulo, seção, subseção) NÃO podem
 *   ser renumerados. Elementos INALTERADO/ALTERADO/REVOGADO recebem números fixos.
 * - Elemento INCLUIDO inserido ENTRE dois elementos não-INCLUIDO do mesmo tipo:
 *   recebe _emendaLetra ('A', 'B', …) sem consumir a contagem.
 * - Elemento INCLUIDO inserido APÓS o último não-INCLUIDO do mesmo tipo (final da
 *   sequência): recebe numeração sequencial normal (sem letra-sufixo).
 * - Unidades INTERNAS ao artigo (parágrafo, inciso, alínea, sub-alínea) são livremente
 *   reordenáveis e sempre numeradas sequencialmente.
 */

// Coleta todos os artigos do documento em ordem DFS (leitura linear do documento).
// Usado para determinar globalmente se um INCLUIDO é "ao final da sequência".
function collectArtigosFlat(elements, result = []) {
  for (const el of elements) {
    if (el.tipo === 'artigo') result.push(el)
    if (el.filhos?.length) collectArtigosFlat(el.filhos, result)
  }
  return result
}

// Verifica se há algum artigo INALTERADO/ALTERADO DEPOIS de `el` na sequência global.
// REVOGADO é ignorado: não bloqueia a numeração sequencial de INCLUIDOs.
// A lista `flatArtigos` é construída uma única vez na chamada raiz e compartilhada.
function hasActiveArtigoAfterGlobal(el, flatArtigos) {
  const idx = flatArtigos.indexOf(el)
  for (let i = idx + 1; i < flatArtigos.length; i++) {
    const s = flatArtigos[i]
    if (s.emendaStatus !== 'INCLUIDO' && s.emendaStatus !== 'REVOGADO') return true
  }
  return false
}

// Verifica localmente (mesmo array de irmãos) se há elemento do mesmo tipo não-INCLUIDO
// e não-REVOGADO após o índice dado. Usado para capítulos, seções e subseções, cuja
// numeração é local ao pai (não cruzam capítulos).
function hasNonIncludedSameTypeAfter(elements, idx, tipo) {
  for (let i = idx + 1; i < elements.length; i++) {
    const s = elements[i]
    if (s.tipo === tipo && s.emendaStatus !== 'INCLUIDO' && s.emendaStatus !== 'REVOGADO') return true
  }
  return false
}

// Mesma checagem, mas para uma lista já homogênea (ex.: parágrafos de um mesmo
// artigo, coletados à parte) — não precisa filtrar por tipo.
function hasActiveAfterInList(list, idx) {
  for (let i = idx + 1; i < list.length; i++) {
    const s = list[i]
    if (s.emendaStatus !== 'INCLUIDO' && s.emendaStatus !== 'REVOGADO') return true
  }
  return false
}

export function renumberElementsEmAlteracao(elements, _ctx = null) {
  if (!elements?.length) return

  // Na chamada raiz (ctx nulo), pré-coleta TODOS os artigos do documento em ordem
  // DFS para que a verificação "ao final da sequência" seja global, não local.
  const ctx = _ctx ?? { artCount: 0, flatArtigos: collectArtigosFlat(elements) }

  let capCount = 0, secCount = 0, subSecCount = 0
  let capLetterIdx = 0, secLetterIdx = 0, subSecLetterIdx = 0, artLetterIdx = 0
  let incisoCount = 0, alineaCount = 0, subAlineaCount = 0
  const paragrafos = []

  for (let i = 0; i < elements.length; i++) {
    const el = elements[i]
    // Marca permanente, não o status ao vivo: um artigo incluído por emenda mantém seu
    // sufixo de letra mesmo depois de ser alterado ou revogado — só assim emendaStatus
    // fica livre para evoluir sem deslocar a numeração sequencial dos artigos seguintes
    // (vedado pela LC 95/1998). Espelha DocumentoFoBuilder.java's assignNumbering.
    const isIncluido = el.incluidoPorEmenda === true

    switch (el.tipo) {
      case 'capitulo': {
        // Capítulos: verificação local (são numerados dentro do próprio nível raiz)
        const atEnd = isIncluido && !hasNonIncludedSameTypeAfter(elements, i, 'capitulo')
        if (!isIncluido || atEnd) {
          capCount++
          el.numero       = capCount
          el._emendaLetra = null
          el._emendaBase  = null
          capLetterIdx    = 0
        } else {
          el.numero       = capCount
          el._emendaBase  = capCount
          el._emendaLetra = String.fromCharCode(65 + capLetterIdx++)
        }
        renumberElementsEmAlteracao(el.filhos, ctx)
        break
      }
      case 'secao_normativa': {
        // Seções: numeração local ao capítulo pai
        const atEnd = isIncluido && !hasNonIncludedSameTypeAfter(elements, i, 'secao_normativa')
        if (!isIncluido || atEnd) {
          secCount++
          el.numero       = secCount
          el._emendaLetra = null
          el._emendaBase  = null
          secLetterIdx    = 0
        } else {
          el.numero       = secCount
          el._emendaBase  = secCount
          el._emendaLetra = String.fromCharCode(65 + secLetterIdx++)
        }
        renumberElementsEmAlteracao(el.filhos, ctx)
        break
      }
      case 'subsecao_normativa': {
        // Subseções: numeração local à seção pai
        const atEnd = isIncluido && !hasNonIncludedSameTypeAfter(elements, i, 'subsecao_normativa')
        if (!isIncluido || atEnd) {
          subSecCount++
          el.numero       = subSecCount
          el._emendaLetra = null
          el._emendaBase  = null
          subSecLetterIdx = 0
        } else {
          el.numero       = subSecCount
          el._emendaBase  = subSecCount
          el._emendaLetra = String.fromCharCode(65 + subSecLetterIdx++)
        }
        renumberElementsEmAlteracao(el.filhos, ctx)
        break
      }
      case 'artigo': {
        // Artigos: numeração GLOBAL — verifica a lista flat de todo o documento.
        // INALTERADO/ALTERADO/REVOGADO: incrementa a contagem (mantendo a sequência original).
        // INCLUIDO entre dois ativos → letra-sufixo (não consome contagem).
        // INCLUIDO ao final de toda a sequência global → numeração normal.
        if (!isIncluido) {
          ctx.artCount++
          el.numero       = ctx.artCount
          el._emendaLetra = null
          el._emendaBase  = null
          artLetterIdx    = 0
        } else {
          const atEnd = !hasActiveArtigoAfterGlobal(el, ctx.flatArtigos)
          if (atEnd) {
            ctx.artCount++
            el.numero       = ctx.artCount
            el._emendaLetra = null
            el._emendaBase  = null
            artLetterIdx    = 0
          } else {
            el.numero       = ctx.artCount
            el._emendaBase  = ctx.artCount
            el._emendaLetra = String.fromCharCode(65 + artLetterIdx++)
          }
        }
        renumberElementsEmAlteracao(el.filhos, ctx)
        break
      }
      case 'paragrafo':
      case 'paragrafo_unico':
        paragrafos.push(el)
        renumberElementsEmAlteracao(el.filhos, ctx)
        break

      case 'inciso':
        incisoCount++
        el.numero       = incisoCount
        el._emendaLetra = null
        renumberElementsEmAlteracao(el.filhos, ctx)
        break

      case 'alinea':
        alineaCount++
        el.numero       = alineaCount
        el._emendaLetra = null
        renumberElementsEmAlteracao(el.filhos, ctx)
        break

      case 'sub_alinea':
        subAlineaCount++
        el.numero       = subAlineaCount
        el._emendaLetra = null
        renumberElementsEmAlteracao(el.filhos, ctx)
        break
    }
  }

  // Parágrafo INCLUIDO entre dois parágrafos já em vigor (não-INCLUIDO/não-REVOGADO):
  // sufixo de letra permanente (§ 2º-A), sem deslocar a numeração dos seguintes —
  // mesma regra já aplicada a capítulo/seção/subseção/artigo acima. Vedação expressa
  // do Decreto nº 12.002/2024, art. 14, IV (renumeração de parágrafo já em vigor).
  const unicoOnly = paragrafos.length === 1 && paragrafos[0].tipo === 'paragrafo_unico'
  if (unicoOnly) {
    paragrafos[0].numero       = null
    paragrafos[0]._emendaLetra = null
    paragrafos[0]._emendaBase  = null
  } else if (paragrafos.length > 0) {
    let pNum = 0, letterIdx = 0
    for (let i = 0; i < paragrafos.length; i++) {
      const p = paragrafos[i]
      p.tipo = 'paragrafo'
      const isIncluido = p.incluidoPorEmenda === true
      const atEnd = isIncluido && !hasActiveAfterInList(paragrafos, i)
      if (!isIncluido || atEnd) {
        pNum++
        p.numero       = pNum
        p._emendaLetra = null
        p._emendaBase  = null
        letterIdx       = 0
      } else {
        p.numero       = pNum
        p._emendaBase  = pNum
        p._emendaLetra = String.fromCharCode(65 + letterIdx++)
      }
    }
  }
}

/**
 * Retorna false se o elemento ou qualquer descendente já está no nível mais baixo
 * (sub_alinea), tornando impossível rebaixar toda a subárvore.
 */
export function canDemoteSubtree(element) {
  if (demoteType(element.tipo) === element.tipo) return false
  return (element.filhos ?? []).every(child => canDemoteSubtree(child))
}

export function findById(elements, id) {
  for (const el of elements) {
    if (el.id === id) return el
    if (el.filhos?.length) {
      const found = findById(el.filhos, id)
      if (found) return found
    }
  }
  return null
}

export function removeById(elements, id) {
  for (let i = 0; i < elements.length; i++) {
    if (elements[i].id === id) return elements.splice(i, 1)[0]
    if (elements[i].filhos?.length) {
      const removed = removeById(elements[i].filhos, id)
      if (removed) return removed
    }
  }
  return null
}

// Separadores per Decreto 12.002/2024 Art. 9deg (NBSP = \xA0 para nao colapsar em HTML)
const S2 = '\xA0\xA0' // dois espacos — Art./paragrafo (incisos II, VI, VIII)
const S1 = '\xA0'     // um espaco — alinea/item (incisos XII, XIV)

/**
 * Rotulo inline para o corpo do documento, incluindo o separador correto.
 * O template renderiza {{ item.label }} diretamente sem adicionar espacos.
 *
 * Formato por tipo (Decreto 12.002/2024 Art. 9deg):
 *   Art.   -> "Art. 1deg  texto"  (inciso I + II)
 *   Par.   -> "Paragrafo unico.  texto" (inciso VI) | "§ 1deg  texto" (inciso VII + VIII)
 *   Inciso -> "I - texto"   (inciso X: espaco + hifen + espaco)
 *   Alinea -> "a) texto"    (inciso XII: letra + parentese + espaco)
 *   Item   -> "1. texto"    (inciso XIV: arabe + ponto + espaco)
 */
export function bodyLabel(element) {
  const n    = element.numero ?? 0
  const letra = element._emendaLetra ?? null
  switch (element.tipo) {
    case 'artigo':          return 'Art. ' + ordinalWithLetra(n, letra) + S2
    case 'paragrafo_unico': return 'Par\xE1grafo \xFAnico.' + S2
    case 'paragrafo':       return '\xA7 ' + ordinalWithLetra(n, letra) + S2
    case 'inciso':          return toRoman(n) + S1 + '-' + S1
    case 'alinea':          return toLetter(n) + ')' + S1
    case 'sub_alinea':      return n + '.' + S1
    default:                return ''
  }
}
