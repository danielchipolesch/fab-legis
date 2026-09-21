import { describe, it, expect } from 'vitest'
import {
  filhosPermitidos, permite, renumerar, rotulo, rotuloDoCorpo, letraDaAlinea, letraDoAnexo, listaDeAnexos,
  cabecalho, comDoisPontos, dataMilitar, dataPorExtenso, assinaturas, rotuloReservado,
} from './npa.js'

// Espelho de HierarquiaDeNpaTest, NumeracaoDeNpaTest e CabecalhoDaNpaTest (backend): os mesmos cenários, de
// propósito, para as duas implementações não divergirem em silêncio.

const no = (tipo, ...filhos) => ({ tipo, filhos })
const cap = (...f) => no('capitulo', ...f)
const sec = (...f) => no('secao_normativa', ...f)
const sub = (...f) => no('subsecao_normativa', ...f)
const par = (...f) => no('paragrafo', ...f)
const ali = () => no('alinea')
const caminho = (el) => el._caminho

describe('hierarquia da NPA', () => {
  it('na raiz só cabe capítulo', () => {
    expect(filhosPermitidos(null).map(o => o.tipo)).toEqual(['capitulo'])
    expect(permite(null, 'capitulo')).toBe(true)
    expect(permite(null, 'paragrafo')).toBe(false)
    expect(permite(null, 'secao_normativa')).toBe(false)
  })

  it('sob o capítulo cabem seção e parágrafo', () => {
    expect(filhosPermitidos('capitulo').map(o => o.tipo)).toEqual(['secao_normativa', 'paragrafo'])
    expect(permite('capitulo', 'subsecao_normativa')).toBe(false)
    expect(permite('capitulo', 'alinea')).toBe(false)
  })

  it('sob a seção cabem subseção e parágrafo; sob a subseção só parágrafo', () => {
    expect(filhosPermitidos('secao_normativa').map(o => o.tipo)).toEqual(['subsecao_normativa', 'paragrafo'])
    expect(permite('secao_normativa', 'secao_normativa')).toBe(false)
    expect(filhosPermitidos('subsecao_normativa').map(o => o.tipo)).toEqual(['paragrafo'])
  })

  it('a alínea só existe sob um parágrafo e não tem filhos', () => {
    expect(permite('paragrafo', 'alinea')).toBe(true)
    for (const pai of [null, 'capitulo', 'secao_normativa', 'subsecao_normativa']) {
      expect(permite(pai, 'alinea')).toBe(false)
    }
    expect(filhosPermitidos('alinea')).toEqual([])
  })

  it('tipos do ato normativo não existem na NPA', () => {
    for (const pai of ['capitulo', 'secao_normativa', 'subsecao_normativa', 'paragrafo']) {
      for (const tipo of ['artigo', 'inciso', 'paragrafo_unico', 'sub_alinea']) expect(permite(pai, tipo)).toBe(false)
    }
  })
})

describe('numeração da NPA', () => {
  it('capítulos em arábico e em sequência', () => {
    const [c1, c2, c3] = [cap(), cap(), cap()]
    renumerar([c1, c2, c3])
    expect([c1, c2, c3].map(caminho)).toEqual(['1', '2', '3'])
  })

  it('seção, subseção e parágrafo seguem o caminho do pai', () => {
    const p = par(); const ss = sub(p); const s = sec(ss); const c = cap(s)
    renumerar([c])
    expect([c, s, ss, p].map(caminho)).toEqual(['1', '1.1', '1.1.1', '1.1.1.1'])
  })

  it('o parágrafo pode ficar direto sob o capítulo', () => {
    const p = par()
    renumerar([cap(), cap(), cap(p)])
    expect(caminho(p)).toBe('3.1')
  })

  it('o parágrafo pode ficar direto sob uma seção', () => {
    const p = par(); const s = sec(p)
    renumerar([cap(), cap(sec(), s)])
    expect(caminho(s)).toBe('2.2')
    expect(caminho(p)).toBe('2.2.1')
  })

  it('seção e parágrafo do mesmo pai dividem uma única sequência', () => {
    const s1 = sec(); const p = par(); const s2 = sec()
    renumerar([cap(s1, p, s2)])
    expect([s1, p, s2].map(caminho)).toEqual(['1.1', '1.2', '1.3'])
  })

  it('subseção e parágrafo de uma seção dividem uma única sequência', () => {
    const p1 = par(); const ss = sub(); const p2 = par()
    renumerar([cap(sec(p1, ss, p2))])
    expect([p1, ss, p2].map(caminho)).toEqual(['1.1.1', '1.1.2', '1.1.3'])
  })

  it('as alíneas de um parágrafo são letradas pela posição', () => {
    const [a, b, c] = [ali(), ali(), ali()]
    renumerar([cap(sec(par(a, b, c)))])
    expect([a, b, c].map(caminho)).toEqual(['a)', 'b)', 'c)'])
  })

  it('as alíneas não consomem a numeração dos irmãos do parágrafo', () => {
    const p1 = par(ali(), ali()); const p2 = par(ali())
    renumerar([cap(sec(p1, p2))])
    expect(caminho(p1)).toBe('1.1.1')
    expect(caminho(p2)).toBe('1.1.2')
    expect(caminho(p2.filhos[0])).toBe('a)')
  })

  it('a numeração de um capítulo não afeta a do outro', () => {
    const s1 = sec(); const s2 = sec()
    renumerar([cap(sec(), s1), cap(s2)])
    expect(caminho(s1)).toBe('1.2')
    expect(caminho(s2)).toBe('2.1')
  })

  it('numero é a posição e não há sufixo de letra de emenda', () => {
    const s = sec(); const p = par()
    const c = cap(s, p)
    p.incluidoPorEmenda = true
    renumerar([c])
    expect(p.numero).toBe(2)
    expect(caminho(p)).toBe('1.2')
    expect(p._emendaLetra).toBeUndefined()
  })

  it('a letra da alínea continua depois do z', () => {
    expect([1, 26, 27, 28].map(letraDaAlinea)).toEqual(['a', 'z', 'aa', 'ab'])
  })

  it('renumerar de novo depois de mover recalcula tudo', () => {
    const s1 = sec(); const p = par()
    const c = cap(s1, p)
    renumerar([c])
    c.filhos = [p, s1]
    renumerar([c])
    expect(caminho(p)).toBe('1.1')
    expect(caminho(s1)).toBe('1.2')
  })
})

describe('rótulos', () => {
  it('capítulo em maiúsculas, seção com o título, parágrafo e alínea só o número', () => {
    const c = { ...cap(), titulo: 'Disposições Finais' }
    const s = { ...sec(), titulo: 'Finalidade' }
    const p = par(); const a = ali()
    c.filhos = [s]; s.filhos = [p]; p.filhos = [a]
    renumerar([c])
    expect(rotulo(c)).toBe('1 — DISPOSIÇÕES FINAIS')
    expect(rotulo(s)).toBe('1.1 — Finalidade')
    expect(rotulo(p)).toBe('1.1.1')
    expect(rotulo(a)).toBe('a)')
  })

  it('o rótulo do corpo leva dois espaços não separáveis', () => {
    const p = par()
    renumerar([cap(p)])
    expect(rotuloDoCorpo(p)).toBe('1.1\xA0\xA0')
  })
})

describe('lista de anexos', () => {
  const anexo = (ordem, titulo) => ({ ordem, titulo })

  it('sem anexos: NÃO HÁ', () => {
    expect(listaDeAnexos([])).toEqual(['NÃO HÁ'])
    expect(listaDeAnexos(null)).toEqual(['NÃO HÁ'])
  })

  it('segue o formato do layout', () => {
    // Uma linha por anexo: ";" entre eles, "; e" no penúltimo e "." no último.
    expect(listaDeAnexos([anexo(1, 'Organograma')])).toEqual(['A - Organograma.'])
    expect(listaDeAnexos([anexo(1, 'Organograma'), anexo(2, 'Fluxograma')])).toEqual(['A - Organograma; e', 'B - Fluxograma.'])
    expect(listaDeAnexos([anexo(1, 'X'), anexo(2, 'Y'), anexo(3, 'Z')])).toEqual(['A - X;', 'B - Y; e', 'C - Z.'])
  })

  it('usa a ordem, mesmo que venham desordenados', () => {
    expect(listaDeAnexos([anexo(2, 'Y'), anexo(1, 'X')])).toEqual(['A - X; e', 'B - Y.'])
  })

  it('letra do anexo', () => {
    expect([1, 2, 26, 27, 28].map(letraDoAnexo)).toEqual(['A', 'B', 'Z', 'AA', 'AB'])
  })
})

describe('cabeçalho e fecho', () => {
  const campos = { setorEmissor: 'DIVISÃO DE SUPORTE', local: 'Brasília', assinaturas: [] }
  const doc = (extra = {}) => ({
    situacao_bca: 'NAO_PUBLICADO', codigo_documento: 'NPA-AGO-01', titulo: 'Funcionamento', om_nome: 'Grupo de Apoio', ...extra,
  })

  it('as três linhas de cima são Comando, OM e setor emissor', () => {
    expect(cabecalho(doc(), campos, []).linhasDeCima).toEqual(['COMANDO DA AERONÁUTICA', 'GRUPO DE APOIO', 'DIVISÃO DE SUPORTE'])
  })

  it('distribuição sempre ostensiva; assunto é o título; identificação livre', () => {
    const c = cabecalho(doc(), campos, [])
    expect(c.distribuicao).toBe('OSTENSIVA')
    expect(c.assunto).toBe('Funcionamento')
    expect(c.identificacao).toBe('NPA-AGO-01')
  })

  it('a emissão é a data da aprovação ou em branco', () => {
    expect(cabecalho(doc(), campos, []).emissao).toBe('__ ___ ____')
    expect(cabecalho(doc({ data_aprovacao: '2026-03-05T10:00:00' }), campos, []).emissao).toBe('05 MAR 2026')
  })

  it('o fecho usa o local e a data da aprovação por extenso', () => {
    expect(cabecalho(doc(), campos, []).localEData).toBe('Brasília, ___ de __________ de ____')
    expect(cabecalho(doc({ data_aprovacao: '2026-12-01T10:00:00' }), campos, []).localEData).toBe('Brasília, 1 de dezembro de 2026')
  })

  it('a efetivação e a linha da publicação só existem depois de publicada', () => {
    const antes = cabecalho(doc(), campos, [])
    expect(antes.efetivacao).toEqual(['BIO __', '__ ___ ____'])
    expect(antes.publicadaNo).toBeNull()

    const ref = 'Boletim Interno Ostensivo nº 15, de 2 de abril de 2026'
    const depois = cabecalho(doc({ situacao_bca: 'PUBLICADO', bca_referencia: ` ${ref} `, data_bca_referencia: '2026-04-02T00:00:00' }), campos, [])
    expect(depois.efetivacao).toEqual(['BIO 15', '02 ABR 2026'])
    expect(depois.publicadaNo).toBe(`(Publicada no ${ref})`)
  })

  it('publicada sem referência mostra os espaços em branco', () => {
    expect(cabecalho(doc({ situacao_bca: 'PUBLICADO' }), campos, []).efetivacao).toEqual(['BIO __', '__ ___ ____'])
  })

  it('a NPA revogada continua mostrando a publicação e ganha a linha da revogação', () => {
    const ref = 'Boletim Interno Ostensivo nº 15, de 2 de abril de 2026'
    const c = cabecalho(doc({ situacao_bca: 'REVOGADO', bca_referencia: ref }),
      { ...campos, boletimDaRevogacao: 'Boletim Interno Ostensivo nº 20, de 3 de maio de 2026' }, [])
    expect(c.publicadaNo).toContain('nº 15')
    expect(c.revogadaNo).toBe('(Revogada pelo Boletim Interno Ostensivo nº 20, de 3 de maio de 2026)')
  })

  it('o rótulo de assinatura leva dois-pontos, sem duplicar', () => {
    const r = comDoisPontos([{ rotulo: 'Visto', linhas: ['A'] }, { rotulo: 'Aprovo:', linhas: ['B'] }, { rotulo: 'Elaborado por  ', linhas: ['C'] }])
    expect(r.map(a => a.rotulo)).toEqual(['Visto:', 'Aprovo:', 'Elaborado por:'])
    expect(r[0].linhas).toEqual(['A'])
    expect(comDoisPontos(null)).toEqual([])
  })

  it('Elaborado por, os blocos escritos e Aprovado por, nessa ordem, com dois-pontos', () => {
    const c = cabecalho(doc(), { ...campos, assinaturas: [{ rotulo: 'Visto', linhas: ['A'] }, { rotulo: 'Proposto por:', linhas: ['B'] }] }, [])
    expect(c.assinaturas.map(a => a.rotulo)).toEqual(['Elaborado por:', 'Visto:', 'Proposto por:', 'Aprovado por:'])
    expect(c.assinaturas[1].linhas).toEqual(['A'])
  })

  it('Elaborado por traz todos os autores e Aprovado por, quem aprovou', () => {
    const a = assinaturas({
      ...campos,
      elaboradoPor: ['Cel FULANO DE TAL', 'Maj BELTRANO', 'Cap CICRANO'],
      aprovadoPor: ['Brig SILVA'],
    })
    expect(a[0].linhas).toEqual(['Cel FULANO DE TAL', 'Maj BELTRANO', 'Cap CICRANO'])
    expect(a[1].rotulo).toBe('Aprovado por:')
    expect(a[1].linhas).toEqual(['Brig SILVA'])
  })

  it('enquanto ninguém aprovou, Aprovado por leva a máscara', () => {
    const a = assinaturas({ ...campos, elaboradoPor: ['Cel FULANO'], aprovadoPor: [] })
    expect(a[1].linhas).toEqual(['[POSTO] FULANO DE TAL'])
  })

  it('os rótulos dos blocos automáticos são reservados', () => {
    expect(rotuloReservado('Elaborado por')).toBe(true)
    expect(rotuloReservado('  aprovado POR: ')).toBe(true)
    expect(rotuloReservado('Visto')).toBe(false)
    expect(rotuloReservado('Aprovo')).toBe(false)
  })

  it('datas sem fuso: o dia não recua', () => {
    expect(dataMilitar('2026-03-01T00:00:00')).toBe('01 MAR 2026')
    expect(dataMilitar('2026-12-31')).toBe('31 DEZ 2026')
    expect(dataPorExtenso('2026-03-01')).toBe('1 de março de 2026')
    expect(dataMilitar(null)).toBeNull()
  })
})
