import { describe, it, expect } from 'vitest'
import {
  toRoman, toLetter, formatLabel, formatReferenciaLabel, bodyLabel, elementIcon,
  renumberElements, renumberElementsEmAlteracao, clausulaRenumeracao, itensRenumeracaoUnico,
  promoteType, demoteType, canDemoteSubtree, findById, removeById,
} from './numbering.js'

// Regras de numeração do Decreto nº 12.002/2024 art. 9º e da LC 95/1998 (vedação
// de renumeração) -- ver docs/dominio.md. Este módulo espelha NumeracaoService.java
// (backend): os cenários de "emenda" abaixo são os mesmos de NumeracaoServiceTest,
// de propósito, para as duas implementações não divergirem em silêncio.

const NBSP = ' '

// Fixtures ─────────────────────────────────────────────────────────────────────

const no = (tipo, filhos = [], extra = {}) => ({ tipo, filhos, ...extra })
const cap = (...filhos) => no('capitulo', filhos)
const sec = (...filhos) => no('secao_normativa', filhos)
const art = (...filhos) => no('artigo', filhos)
const incluido = (tipo, filhos = [], extra = {}) =>
  no(tipo, filhos, { incluidoPorEmenda: true, emendaStatus: 'INCLUIDO', ...extra })
const rotulo = (el) => formatLabel(el)

// ─── Formatação ────────────────────────────────────────────────────────────────

describe('toRoman', () => {
  it.each([
    [1, 'I'], [4, 'IV'], [9, 'IX'], [14, 'XIV'], [40, 'XL'], [90, 'XC'], [400, 'CD'], [1994, 'MCMXCIV'],
  ])('%i em romano é %s', (n, esperado) => {
    expect(toRoman(n)).toBe(esperado)
  })

  it('zero é vazio', () => {
    expect(toRoman(0)).toBe('')
  })
})

describe('toLetter', () => {
  it('1 é a, 3 é c', () => {
    expect(toLetter(1)).toBe('a')
    expect(toLetter(3)).toBe('c')
  })
})

describe('formatLabel', () => {
  it.each([
    [1, 'Art. 1º'], [9, 'Art. 9º'], [10, 'Art. 10.'], [12, 'Art. 12.'], [1024, 'Art. 1.024.'],
  ])('artigo %i: ordinal até o 9º e cardinal com ponto a partir do 10', (numero, esperado) => {
    expect(formatLabel({ tipo: 'artigo', numero })).toBe(esperado)
  })

  it.each([
    [5, 'A', 'Art. 5º-A'], [9, 'B', 'Art. 9º-B'], [10, 'A', 'Art. 10-A.'], [12, 'C', 'Art. 12-C.'], [1024, 'B', 'Art. 1.024-B.'],
  ])('artigo %i com letra %s: o ponto do cardinal migra para depois da letra', (numero, letra, esperado) => {
    expect(formatLabel({ tipo: 'artigo', numero, _emendaLetra: letra })).toBe(esperado)
  })

  it('capítulo em romano, título em maiúsculas', () => {
    expect(formatLabel({ tipo: 'capitulo', numero: 4, titulo: 'Disposições Finais' })).toBe('CAPÍTULO IV — DISPOSIÇÕES FINAIS')
    expect(formatLabel({ tipo: 'capitulo', numero: 1 })).toBe('CAPÍTULO I')
    expect(formatLabel({ tipo: 'capitulo', numero: 1, _emendaLetra: 'A' })).toBe('CAPÍTULO I-A')
  })

  it('seção e subseção em romano, título como digitado', () => {
    expect(formatLabel({ tipo: 'secao_normativa', numero: 2, titulo: 'Âmbito' })).toBe('Seção II — Âmbito')
    expect(formatLabel({ tipo: 'secao_normativa', numero: 1, _emendaLetra: 'B' })).toBe('Seção I-B')
    expect(formatLabel({ tipo: 'subsecao_normativa', numero: 3 })).toBe('Subseção III')
  })

  it('parágrafo, parágrafo único, inciso, alínea e subalínea', () => {
    expect(formatLabel({ tipo: 'paragrafo', numero: 1 })).toBe('§ 1º')
    expect(formatLabel({ tipo: 'paragrafo', numero: 10 })).toBe('§ 10.')
    expect(formatLabel({ tipo: 'paragrafo', numero: 2, _emendaLetra: 'A' })).toBe('§ 2º-A')
    expect(formatLabel({ tipo: 'paragrafo_unico' })).toBe('Parágrafo único')
    expect(formatLabel({ tipo: 'inciso', numero: 7 })).toBe('VII')
    expect(formatLabel({ tipo: 'alinea', numero: 3 })).toBe('c)')
    expect(formatLabel({ tipo: 'sub_alinea', numero: 2 })).toBe('2.')
  })

  it('elementos da parte preliminar e final têm rótulo fixo', () => {
    expect(formatLabel({ tipo: 'epigrafe' })).toBe('Epígrafe')
    expect(formatLabel({ tipo: 'ementa' })).toBe('Ementa')
    expect(formatLabel({ tipo: 'preambulo' })).toBe('Preâmbulo')
    expect(formatLabel({ tipo: 'clausula_vigencia' })).toBe('Cláusula de Vigência')
    expect(formatLabel({ tipo: 'assinatura' })).toBe('Assinatura')
  })

  it('tipo desconhecido devolve o próprio tipo', () => {
    expect(formatLabel({ tipo: 'inexistente' })).toBe('inexistente')
  })
})

describe('formatReferenciaLabel', () => {
  it('incisos, alíneas e subalíneas ganham o nome por extenso (só no quadro de justificativas)', () => {
    expect(formatReferenciaLabel({ tipo: 'inciso', numero: 3 })).toBe('Inciso III')
    expect(formatReferenciaLabel({ tipo: 'alinea', numero: 3 })).toBe('Alínea c)')
    expect(formatReferenciaLabel({ tipo: 'sub_alinea', numero: 2 })).toBe('Subalínea 2.')
  })

  it('os demais tipos mantêm o rótulo normal', () => {
    expect(formatReferenciaLabel({ tipo: 'artigo', numero: 1 })).toBe('Art. 1º')
  })
})

describe('bodyLabel (rótulo inline do corpo, com separador do Decreto 12.002/2024)', () => {
  it('artigo e parágrafo: dois espaços não separáveis', () => {
    expect(bodyLabel({ tipo: 'artigo', numero: 1 })).toBe(`Art. 1º${NBSP}${NBSP}`)
    expect(bodyLabel({ tipo: 'paragrafo', numero: 2 })).toBe(`§ 2º${NBSP}${NBSP}`)
    expect(bodyLabel({ tipo: 'paragrafo_unico' })).toBe(`Parágrafo único.${NBSP}${NBSP}`)
  })

  it('inciso, alínea e subalínea: um espaço não separável', () => {
    expect(bodyLabel({ tipo: 'inciso', numero: 4 })).toBe(`IV${NBSP}-${NBSP}`)
    expect(bodyLabel({ tipo: 'alinea', numero: 3 })).toBe(`c)${NBSP}`)
    expect(bodyLabel({ tipo: 'sub_alinea', numero: 2 })).toBe(`2.${NBSP}`)
  })

  it('artigo com letra usa o sufixo de emenda', () => {
    expect(bodyLabel({ tipo: 'artigo', numero: 5, _emendaLetra: 'A' })).toBe(`Art. 5º-A${NBSP}${NBSP}`)
  })

  it('tipos sem rótulo de corpo devolvem vazio', () => {
    expect(bodyLabel({ tipo: 'capitulo', numero: 1 })).toBe('')
  })
})

describe('elementIcon', () => {
  it('cada tipo tem um ícone e o desconhecido tem um padrão', () => {
    expect(elementIcon('capitulo')).toBe('mdi-folder-outline')
    expect(elementIcon('artigo')).toBe('mdi-format-list-numbered')
    expect(elementIcon('inexistente')).toBe('mdi-file-document-outline')
  })
})

// ─── Numeração sem emenda ──────────────────────────────────────────────────────

describe('renumberElements', () => {
  it('capítulos são numerados em sequência', () => {
    const els = [cap(), cap(), cap()]
    renumberElements(els)
    expect(els.map(e => e.numero)).toEqual([1, 2, 3])
  })

  it('seções reiniciam em cada capítulo', () => {
    const els = [cap(sec(), sec()), cap(sec(), sec())]
    renumberElements(els)
    expect(els[0].filhos.map(e => e.numero)).toEqual([1, 2])
    expect(els[1].filhos.map(e => e.numero)).toEqual([1, 2])
  })

  it('artigos têm numeração contínua pelo documento inteiro', () => {
    const a1 = art(), a2 = art(), a3 = art(), a4 = art()
    renumberElements([cap(sec(a1, a2)), cap(a3, sec(a4))])
    expect([a1, a2, a3, a4].map(a => a.numero)).toEqual([1, 2, 3, 4])
  })

  // Estrutura que todo documento novo recebe (CapitulosPadronizadosService, no backend).
  it('estrutura padrão da NSCA 5-3', () => {
    const artigos = Array.from({ length: 6 }, () => art())
    const c1 = cap(sec(artigos[0]), sec(artigos[1]))
    const c2 = cap(artigos[2])
    const c3 = cap(artigos[3])
    const c4 = cap(sec(artigos[4]), sec(artigos[5]))
    renumberElements([c1, c2, c3, c4])

    expect([c1, c2, c3, c4].map(rotulo)).toEqual(['CAPÍTULO I', 'CAPÍTULO II', 'CAPÍTULO III', 'CAPÍTULO IV'])
    expect([...c1.filhos, ...c4.filhos].map(rotulo)).toEqual(['Seção I', 'Seção II', 'Seção I', 'Seção II'])
    expect(artigos.map(rotulo)).toEqual(['Art. 1º', 'Art. 2º', 'Art. 3º', 'Art. 4º', 'Art. 5º', 'Art. 6º'])
  })

  it('artigo de número dez em diante usa cardinal com ponto', () => {
    const artigos = Array.from({ length: 12 }, () => art())
    renumberElements([cap(...artigos)])
    expect(rotulo(artigos[8])).toBe('Art. 9º')
    expect(rotulo(artigos[9])).toBe('Art. 10.')
    expect(rotulo(artigos[11])).toBe('Art. 12.')
  })

  it('incisos, alíneas e subalíneas reiniciam em cada pai', () => {
    const sub1 = no('sub_alinea'), sub2 = no('sub_alinea')
    const al1 = no('alinea', [sub1, sub2]), al2 = no('alinea')
    const inc1 = no('inciso', [al1, al2]), inc2 = no('inciso')
    const outroInc = no('inciso')
    const a1 = art(inc1, inc2), a2 = art(outroInc)
    renumberElements([a1, a2])

    expect([inc1, inc2].map(e => e.numero)).toEqual([1, 2])
    expect([al1, al2].map(e => e.numero)).toEqual([1, 2])
    expect([sub1, sub2].map(e => e.numero)).toEqual([1, 2])
    expect(outroInc.numero).toBe(1)
  })

  it('parágrafo único permanece único quando é o único parágrafo do artigo', () => {
    const p = no('paragrafo_unico')
    renumberElements([art(p)])
    expect(p.tipo).toBe('paragrafo_unico')
    expect(p.numero).toBeNull()
  })

  it('com mais de um parágrafo, todos viram parágrafos numerados', () => {
    const p1 = no('paragrafo_unico'), p2 = no('paragrafo')
    renumberElements([art(p1, p2)])
    expect(p1.tipo).toBe('paragrafo')
    expect([p1.numero, p2.numero]).toEqual([1, 2])
  })

  it('um parágrafo comum sozinho continua numerado (não vira único)', () => {
    const p = no('paragrafo')
    renumberElements([art(p)])
    expect(p.tipo).toBe('paragrafo')
    expect(p.numero).toBe(1)
  })

  it('lista vazia ou ausente não quebra', () => {
    expect(() => renumberElements([])).not.toThrow()
    expect(() => renumberElements(undefined)).not.toThrow()
  })

  it('elementos fora da parte normativa são ignorados', () => {
    const e = no('epigrafe')
    renumberElements([e])
    expect(e.numero).toBeUndefined()
  })
})

// ─── Numeração com emenda (sufixo de letra, LC 95/1998) ────────────────────────

describe('renumberElementsEmAlteracao', () => {
  it('sem elementos incluídos numera igual ao fluxo normal', () => {
    const a1 = art(), a2 = art(), s = sec(a1), c = cap(s, sec(a2))
    renumberElementsEmAlteracao([c])
    expect([c.numero, s.numero, a1.numero, a2.numero]).toEqual([1, 1, 1, 2])
    expect(a1._emendaLetra).toBeNull()
  })

  it('artigo incluído entre dois ativos recebe letra sem consumir numeração', () => {
    const a1 = art(), a2 = incluido('artigo'), a3 = art()
    renumberElementsEmAlteracao([cap(a1, a2, a3)])

    expect(rotulo(a1)).toBe('Art. 1º')
    expect(rotulo(a2)).toBe('Art. 1º-A')
    expect(a2.numero).toBe(1)
    expect(a2._emendaLetra).toBe('A')
    // O artigo seguinte não é deslocado: é a garantia central da vedação de renumeração.
    expect(rotulo(a3)).toBe('Art. 2º')
  })

  it('vários artigos incluídos seguidos recebem letras sucessivas', () => {
    const a2 = incluido('artigo'), a3 = incluido('artigo')
    const a4 = art()
    renumberElementsEmAlteracao([cap(art(), a2, a3, a4)])

    expect(rotulo(a2)).toBe('Art. 1º-A')
    expect(rotulo(a3)).toBe('Art. 1º-B')
    expect(rotulo(a4)).toBe('Art. 2º')
  })

  it('artigo incluído ao final da sequência recebe numeração normal', () => {
    const a2 = incluido('artigo')
    renumberElementsEmAlteracao([cap(art(), a2)])

    expect(rotulo(a2)).toBe('Art. 2º')
    expect(a2._emendaLetra).toBeNull()
  })

  it('a numeração de artigo é global: o "final" vale para o documento todo, não para o capítulo', () => {
    // Incluído é o último do 1º capítulo, mas há artigo ativo no 2º -> ainda recebe letra.
    const incl = incluido('artigo')
    const seguinte = art()
    renumberElementsEmAlteracao([cap(art(), incl), cap(seguinte)])

    expect(rotulo(incl)).toBe('Art. 1º-A')
    expect(rotulo(seguinte)).toBe('Art. 2º')
  })

  it('letra de artigo incluído a partir do 10º tem o ponto depois da letra', () => {
    const antes = Array.from({ length: 10 }, () => art())
    const incl = incluido('artigo')
    const depois = art()
    renumberElementsEmAlteracao([cap(...antes, incl, depois)])

    expect(rotulo(incl)).toBe('Art. 10-A.')
    expect(rotulo(depois)).toBe('Art. 11.')
  })

  // A marca é permanente (incluidoPorEmenda), não o status ao vivo: alterar ou
  // revogar o artigo depois não pode fazê-lo perder a letra nem deslocar os demais.
  it.each(['ALTERADO', 'REVOGADO'])('artigo incluído mantém a letra depois de %s', (status) => {
    const incl = no('artigo', [], { incluidoPorEmenda: true, emendaStatus: status })
    const seguinte = art()
    renumberElementsEmAlteracao([cap(art(), incl, seguinte)])

    expect(rotulo(incl)).toBe('Art. 1º-A')
    expect(rotulo(seguinte)).toBe('Art. 2º')
  })

  it('capítulo incluído entre dois ativos recebe letra; ao final, numeração normal', () => {
    const c2 = incluido('capitulo')
    renumberElementsEmAlteracao([cap(), c2, cap()])
    expect(rotulo(c2)).toBe('CAPÍTULO I-A')

    const cFinal = incluido('capitulo')
    renumberElementsEmAlteracao([cap(), cFinal])
    expect(rotulo(cFinal)).toBe('CAPÍTULO II')
  })

  it('seção incluída recebe letra local ao capítulo', () => {
    const s2 = incluido('secao_normativa')
    const s3 = sec()
    renumberElementsEmAlteracao([cap(sec(), s2, s3)])

    expect(rotulo(s2)).toBe('Seção I-A')
    expect(rotulo(s3)).toBe('Seção II')
  })

  it('subseção incluída recebe letra local à seção', () => {
    const sub2 = incluido('subsecao_normativa')
    renumberElementsEmAlteracao([cap(sec(no('subsecao_normativa'), sub2, no('subsecao_normativa')))])

    expect(rotulo(sub2)).toBe('Subseção I-A')
  })

  it('parágrafo incluído entre dois em vigor recebe letra (§ 2º-A) sem deslocar os seguintes', () => {
    const p1 = no('paragrafo'), p2 = incluido('paragrafo'), p3 = no('paragrafo')
    renumberElementsEmAlteracao([art(p1, p2, p3)])

    expect(rotulo(p1)).toBe('§ 1º')
    expect(rotulo(p2)).toBe('§ 1º-A')
    expect(rotulo(p3)).toBe('§ 2º')
  })

  it('parágrafos incluídos seguidos recebem letras sucessivas', () => {
    const p2 = incluido('paragrafo'), p3 = incluido('paragrafo'), p4 = no('paragrafo')
    renumberElementsEmAlteracao([art(no('paragrafo'), p2, p3, p4)])

    expect([p2, p3, p4].map(rotulo)).toEqual(['§ 1º-A', '§ 1º-B', '§ 2º'])
  })

  // Parágrafo em vigor NÃO pode ser renumerado: o revogado continua ocupando o número
  // que tinha, então o incluído antes dele recebe letra e o revogado não é empurrado.
  it('parágrafo incluído antes de um revogado recebe letra e o revogado mantém o número', () => {
    const incl = incluido('paragrafo')
    const revogado = no('paragrafo', [], { emendaStatus: 'REVOGADO' })
    renumberElementsEmAlteracao([art(no('paragrafo'), incl, revogado)])

    expect(rotulo(incl)).toBe('§ 1º-A')
    expect(rotulo(revogado)).toBe('§ 2º')
  })

  // A decisão usa a marca permanente (incluidoPorEmenda), não o status ao vivo: alterar
  // um parágrafo incluído depois não pode mudar o rótulo dos que vêm antes dele.
  it('alterar um parágrafo incluído não muda o rótulo dos anteriores', () => {
    const inclA = incluido('paragrafo')
    const inclB = incluido('paragrafo', [], { emendaStatus: 'ALTERADO' })
    renumberElementsEmAlteracao([art(no('paragrafo'), inclA, inclB)])

    expect([inclA, inclB].map(rotulo)).toEqual(['§ 2º', '§ 3º'])
  })

  it('parágrafo incluído ao final recebe numeração normal', () => {
    const p2 = incluido('paragrafo')
    renumberElementsEmAlteracao([art(no('paragrafo'), p2)])

    expect(rotulo(p2)).toBe('§ 2º')
  })

  it('unidades internas ao artigo são numeradas sempre em sequência', () => {
    const i1 = no('inciso'), i2 = no('inciso'), al = no('alinea'), sa = no('sub_alinea')
    renumberElementsEmAlteracao([art(i1, no('inciso', [al, no('alinea', [sa])]), i2)])

    expect([i1.numero, i2.numero]).toEqual([1, 3])
    expect(al.numero).toBe(1)
    expect(sa.numero).toBe(1)
  })

  it('parágrafo único permanece único em alteração', () => {
    const p = no('paragrafo_unico')
    renumberElementsEmAlteracao([art(p)])
    expect(p.tipo).toBe('paragrafo_unico')
    expect(p.numero).toBeNull()
  })

  // ── Elemento revogado mantém o número (LC 95/1998) ─────────────────────────────
  //
  // Artigo, capítulo, seção e subseção ORIGINAIS contam como "ativos" mesmo depois de
  // revogados (NumeracaoService.java, backend): um incluído antes de um revogado recebe
  // letra, e o revogado mantém o número que sempre teve -- revogar não abre vaga nem
  // renumera. Mesmos cenários de NumeracaoServiceTest.

  it('artigo incluído antes de um artigo revogado recebe letra e o revogado mantém o número', () => {
    const incl = incluido('artigo')
    const revogado = no('artigo', [], { emendaStatus: 'REVOGADO' })
    renumberElementsEmAlteracao([cap(art(), incl, revogado)])

    expect(rotulo(incl)).toBe('Art. 1º-A')
    expect(rotulo(revogado)).toBe('Art. 2º')
  })

  it('capítulo incluído antes de um capítulo revogado recebe letra e o revogado mantém o número', () => {
    const incl = incluido('capitulo')
    const revogado = no('capitulo', [], { emendaStatus: 'REVOGADO' })
    renumberElementsEmAlteracao([cap(), incl, revogado])

    expect(rotulo(incl)).toBe('CAPÍTULO I-A')
    expect(rotulo(revogado)).toBe('CAPÍTULO II')
  })
})

// ─── Hierarquia ────────────────────────────────────────────────────────────────

describe('promoteType / demoteType', () => {
  it('a hierarquia é artigo > parágrafo > inciso > alínea > subalínea', () => {
    expect(promoteType('sub_alinea')).toBe('alinea')
    expect(promoteType('alinea')).toBe('inciso')
    expect(promoteType('inciso')).toBe('paragrafo')
    expect(promoteType('paragrafo')).toBe('artigo')

    expect(demoteType('artigo')).toBe('paragrafo')
    expect(demoteType('paragrafo')).toBe('inciso')
    expect(demoteType('inciso')).toBe('alinea')
    expect(demoteType('alinea')).toBe('sub_alinea')
  })

  it('os extremos e tipos fora da hierarquia não mudam', () => {
    expect(promoteType('artigo')).toBe('artigo')
    expect(demoteType('sub_alinea')).toBe('sub_alinea')
    expect(promoteType('capitulo')).toBe('capitulo')
    expect(demoteType('capitulo')).toBe('capitulo')
  })

  it('parágrafo único ocupa o mesmo nível do parágrafo', () => {
    expect(promoteType('paragrafo_unico')).toBe('artigo')
    expect(demoteType('paragrafo_unico')).toBe('inciso')
  })
})

describe('canDemoteSubtree', () => {
  it('subalínea não pode ser rebaixada', () => {
    expect(canDemoteSubtree(no('sub_alinea'))).toBe(false)
  })

  it('só pode rebaixar se nenhum descendente estiver no nível mais baixo', () => {
    expect(canDemoteSubtree(no('alinea'))).toBe(true)
    expect(canDemoteSubtree(no('inciso', [no('alinea')]))).toBe(true)
    expect(canDemoteSubtree(no('inciso', [no('alinea', [no('sub_alinea')])]))).toBe(false)
    expect(canDemoteSubtree(no('alinea', [no('sub_alinea')]))).toBe(false)
  })
})

describe('findById / removeById', () => {
  const montar = () => [
    cap(sec(no('artigo', [no('inciso', [], { id: 'inc' })], { id: 'art' })), { id: 'sec2', tipo: 'secao_normativa', filhos: [] }),
  ].map(c => ({ ...c, id: 'cap' }))

  it('localiza um elemento em qualquer profundidade', () => {
    const els = montar()
    expect(findById(els, 'cap')).toBe(els[0])
    expect(findById(els, 'art').tipo).toBe('artigo')
    expect(findById(els, 'inc').tipo).toBe('inciso')
  })

  it('devolve null quando não encontra', () => {
    expect(findById(montar(), 'nao-existe')).toBeNull()
  })

  it('remove o elemento da árvore e o devolve', () => {
    const els = montar()
    const removido = removeById(els, 'inc')
    expect(removido.id).toBe('inc')
    expect(findById(els, 'inc')).toBeNull()
    expect(findById(els, 'art')).not.toBeNull()
  })

  it('remover algo inexistente devolve null e não altera a árvore', () => {
    const els = montar()
    expect(removeById(els, 'nao-existe')).toBeNull()
    expect(findById(els, 'inc')).not.toBeNull()
  })
})

// ─── Parágrafo único que vira "§ 1º" (único caso de renumeração de parágrafo) ───────
// Espelha NumeracaoService.unicoRenumerado (backend): mesmos cenários de NumeracaoServiceTest.

describe('parágrafo único renumerado', () => {
  const unico = (extra = {}) => no('paragrafo_unico', [], { id: 'u', emendaStatus: 'INALTERADO', ...extra })
  const novo = () => incluido('paragrafo', [], { id: 'n' })

  it('único em vigor que ganha um 2º parágrafo (documento publicado) é marcado como renumerado', () => {
    const u = unico(), n = novo()
    renumberElementsEmAlteracao([art(u, n)])
    expect(u._unicoRenumerado).toBe(true)
    expect(rotulo(u)).toBe('§ 1º')
    expect(rotulo(n)).toBe('§ 2º')
    expect(n._unicoRenumerado).toBe(false)
  })

  it('único sozinho nunca é renumerado', () => {
    const u = unico()
    renumberElementsEmAlteracao([art(u)])
    expect(u._unicoRenumerado).toBe(false)
    expect(rotulo(u)).toBe('Parágrafo único')
  })

  it('único incluído por emenda ainda pendente não estava em vigor: sem riscado', () => {
    const u = unico({ incluidoPorEmenda: true, emendaStatus: 'INCLUIDO', clausulaEmenda: null })
    renumberElementsEmAlteracao([art(u, novo())])
    expect(u._unicoRenumerado).toBe(false)
  })

  it('único incluído num ciclo anterior (já publicado) estava em vigor', () => {
    const u = unico({ incluidoPorEmenda: true, emendaStatus: 'INCLUIDO', clausulaEmenda: '(incluído pela Portaria X)' })
    renumberElementsEmAlteracao([art(u, novo())])
    expect(u._unicoRenumerado).toBe(true)
  })

  it('a marca sobrevive a uma nova numeração (o tipo já virou paragrafo)', () => {
    const u = unico(), n = novo()
    const arvore = [art(u, n)]
    renumberElementsEmAlteracao(arvore)
    renumberElementsEmAlteracao(arvore)
    expect(u.tipo).toBe('paragrafo')
    expect(u._unicoRenumerado).toBe(true)
  })

  it('em documento ainda não publicado (edição livre) nada é riscado', () => {
    const u = unico(), p2 = no('paragrafo', [], { id: 'p2' })
    renumberElements([art(u, p2)])
    expect(u._unicoRenumerado).toBe(false)
    expect(rotulo(u)).toBe('§ 1º')
    expect(rotulo(p2)).toBe('§ 2º')
  })

  it('com a cláusula já congelada (alteração publicada) o riscado é permanente', () => {
    const u = unico({ clausulaRenumeracao: '(redação dada pela Portaria X, publicada no BCA Y)' })
    const outro = incluido('paragrafo', [], { clausulaEmenda: '(incluído pela Portaria X)' })
    renumberElementsEmAlteracao([art(u, outro)])
    expect(u._unicoRenumerado).toBe(true)
  })

  it('a cláusula ao vivo usa o placeholder e a congelada prevalece', () => {
    expect(clausulaRenumeracao({})).toContain('redação dada pela Portaria DIRAD n° XYZ')
    expect(clausulaRenumeracao({ clausulaRenumeracao: '(redação dada ... 7)' })).toBe('(redação dada ... 7)')
  })

  it('itens sintéticos do quadro/portaria: só os pendentes (sem cláusula congelada)', () => {
    const pendente = unico({ id: 'a', conteudo: '{"x":1}' }), n1 = novo()
    const congelado = unico({ id: 'b', clausulaRenumeracao: '(redação dada ... 1)' })
    const outro = incluido('paragrafo', [], { clausulaEmenda: '(incluído ...)' })
    const arvore = [art(pendente, n1), art(congelado, outro)]
    renumberElementsEmAlteracao(arvore)
    const itens = itensRenumeracaoUnico({ secoes: [{ tipo: 'parte_normativa', elementos: arvore }] })
    expect(itens).toHaveLength(1)
    expect(itens[0]).toMatchObject({ elementoId: 'a', acao: 'ALTERAR', cicloReferencia: null, textoNovo: '{"x":1}' })
  })
})
