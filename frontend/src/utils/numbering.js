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

// Decreto 12.002/2024 Art. 9 deg I e VII: ordinal (grau) ate o 9, cardinal (.) a partir do 10.
function ordinalOrCardinal(n) {
  return n <= 9 ? `${n}` + '\xBA' : `${n}.`
}

export function formatLabel(element) {
  switch (element.tipo) {
    case 'capitulo': {
      const t = element.titulo ? ' — ' + element.titulo.toUpperCase() : ''
      return 'CAP\xCDTULO ' + toRoman(element.numero ?? 0) + t
    }
    case 'secao_normativa': {
      const t = element.titulo ? ' — ' + element.titulo : ''
      return 'Se\xE7\xE3o ' + toRoman(element.numero ?? 0) + t
    }
    case 'subsecao_normativa': {
      const t = element.titulo ? ' — ' + element.titulo : ''
      return 'Subse\xE7\xE3o ' + toRoman(element.numero ?? 0) + t
    }
    case 'artigo':          return 'Art. ' + ordinalOrCardinal(element.numero ?? 0)
    case 'paragrafo_unico': return 'Par\xE1grafo \xFAnico'
    case 'paragrafo':       return '\xA7 ' + ordinalOrCardinal(element.numero ?? 0)
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

  // Paragrafo unico <-> paragrafo numerado conversion
  if (paragrafos.length > 1) {
    let pNum = 0
    for (const p of paragrafos) { pNum++; p.tipo = 'paragrafo'; p.numero = pNum }
  } else if (paragrafos.length === 1) {
    paragrafos[0].tipo = 'paragrafo_unico'
    paragrafos[0].numero = null
  }
}

const HIERARCHY = ['artigo', 'paragrafo', 'inciso', 'alinea', 'sub_alinea']

export function promoteType(tipo) {
  const idx = HIERARCHY.indexOf(tipo)
  if (idx <= 0) return tipo
  return HIERARCHY[idx - 1]
}

export function demoteType(tipo) {
  const idx = HIERARCHY.indexOf(tipo)
  if (idx < 0 || idx >= HIERARCHY.length - 1) return tipo
  return HIERARCHY[idx + 1]
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
  const n = element.numero ?? 0
  switch (element.tipo) {
    case 'artigo':          return 'Art. ' + ordinalOrCardinal(n) + S2
    case 'paragrafo_unico': return 'Par\xE1grafo \xFAnico.' + S2
    case 'paragrafo':       return '\xA7 ' + ordinalOrCardinal(n) + S2
    case 'inciso':          return toRoman(n) + S1 + '-' + S1
    case 'alinea':          return toLetter(n) + ')' + S1
    case 'sub_alinea':      return n + '.' + S1
    default:                return ''
  }
}
