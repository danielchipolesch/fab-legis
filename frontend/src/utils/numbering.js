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

export function formatLabel(element) {
  switch (element.tipo) {
    case 'capitulo': {
      const t = element.titulo ? ` — ${element.titulo.toUpperCase()}` : ''
      return `CAPÍTULO ${toRoman(element.numero ?? 0)}${t}`
    }
    case 'secao_normativa': {
      const t = element.titulo ? ` — ${element.titulo}` : ''
      return `Seção ${toRoman(element.numero ?? 0)}${t}`
    }
    case 'subsecao_normativa': {
      const t = element.titulo ? ` — ${element.titulo}` : ''
      return `Subseção ${toRoman(element.numero ?? 0)}${t}`
    }
    case 'artigo':          return `Art. ${element.numero}°`
    case 'paragrafo_unico': return 'Parágrafo único'
    case 'paragrafo':       return `§ ${element.numero}°`
    case 'inciso':          return toRoman(element.numero)
    case 'alinea':          return `${toLetter(element.numero)})`
    case 'sub_alinea':      return `${element.numero})`
    case 'epigrafe':              return 'Epígrafe'
    case 'ementa':                return 'Ementa'
    case 'preambulo':             return 'Preâmbulo'
    case 'fundamentacao':         return 'Fundamentação'
    case 'clausula_revogatoria':  return 'Cláusula Revogatória'
    case 'clausula_vigencia':     return 'Cláusula de Vigência'
    case 'fecho':                 return 'Fecho'
    case 'assinatura':            return 'Assinatura'
    case 'referenda':             return 'Referenda'
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
 * - Capítulos, seções, subseções: numbered per level (I, II, III…)
 * - Artigos: numbered GLOBALLY across the entire parte_normativa
 * - Parágrafos, incisos, alíneas, sub-alíneas: numbered per parent
 *
 * _ctx carries the global article counter across recursive calls.
 * Pass null (or omit) for a fresh top-level call.
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
        renumberElements(el.filhos, null) // reset for artigo-level children
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

  // Parágrafo único ↔ parágrafo numerado conversion
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

/** Returns the inline label used in the document body (preview). */
export function bodyLabel(element) {
  switch (element.tipo) {
    case 'artigo':          return `Art. ${element.numero}°.`
    case 'paragrafo_unico': return 'Parágrafo único.'
    case 'paragrafo':       return `§ ${element.numero}°`
    case 'inciso':          return `${toRoman(element.numero)} –`
    case 'alinea':          return `${toLetter(element.numero)})`
    case 'sub_alinea':      return `${element.numero}.`
    default:                return ''
  }
}
