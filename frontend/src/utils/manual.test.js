import { describe, it, expect } from 'vitest'
import { slugDeTitulo, slugDaPagina, secoesDe, montarPaginas, nomesDosPerfis, ehParaMim, buscarNoManual } from './manual.js'
import { renderizarManual } from './manualMarkdown.js'
import { PAPEIS } from './papeis.js'

const arquivos = {
  '/src/manual/02-segunda.md': '<!-- perfis: ELB APR_OM -->\n# Segunda página\n\nIntrodução da segunda.\n\n## Aprovar um documento\n\nO **Aprovador da OM** decide.\n\n## Publicação\n\nSó no CENDOC.',
  '/src/manual/01-primeira.md': '# Primeira página\n\nTexto de abertura sobre acessibilidade.\n\n## Entrar no sistema\n\nDigite o CPF.',
}

describe('manual: páginas e seções', () => {
  it('o id de um título não tem acento nem pontuação', () => {
    expect(slugDeTitulo('Área de Trabalho')).toBe('area-de-trabalho')
    expect(slugDeTitulo('Aprovar (Aprovador da OM)')).toBe('aprovar-aprovador-da-om')
    expect(slugDeTitulo('NPA — Norma Padrão de Ação')).toBe('npa-norma-padrao-de-acao')
  })

  it('o endereço da página é o nome do arquivo sem o número de ordem', () => {
    expect(slugDaPagina('/src/manual/03-elaborar-um-documento.md')).toBe('elaborar-um-documento')
  })

  it('as páginas saem na ordem do número do arquivo, com título, perfis e seções', () => {
    const paginas = montarPaginas(arquivos)
    expect(paginas.map(p => p.slug)).toEqual(['primeira', 'segunda'])
    expect(paginas[1].titulo).toBe('Segunda página')
    expect(paginas[1].perfis).toEqual(['ELB', 'APR_OM'])
    expect(paginas[0].perfis).toEqual([])
    expect(paginas[1].secoes.map(s => s.id)).toEqual(['aprovar-um-documento', 'publicacao'])
    expect(paginas[1].markdown).not.toContain('perfis:')
    expect(paginas[1].markdown).not.toMatch(/^# /m)
  })

  it('"##" dentro de um bloco de código não é uma seção', () => {
    expect(secoesDe('intro\n\n```\n## não é título\n```\n\n## Título real').filter(s => s.titulo).map(s => s.titulo)).toEqual(['Título real'])
  })

  it('os perfis da página viram nomes por extenso; código desconhecido é ignorado', () => {
    expect(nomesDosPerfis(['ELB', 'APR_OM', 'XXX'])).toEqual(['Elaborador', 'Aprovador da OM'])
  })

  it('a página é para quem tem algum dos perfis dela; sem perfis, não destaca ninguém', () => {
    const [primeira, segunda] = montarPaginas(arquivos)
    expect(ehParaMim(segunda, ['ELB'])).toBe(true)
    expect(ehParaMim(segunda, ['PUB_BCA'])).toBe(false)
    expect(ehParaMim(primeira, ['ELB'])).toBe(false)
  })
})

describe('manual: busca', () => {
  const paginas = montarPaginas(arquivos)

  it('acha sem distinguir maiúsculas nem acentos, e aponta a seção', () => {
    const r = buscarNoManual(paginas, 'APROVADOR da om')
    expect(r).toHaveLength(1)
    expect(r[0]).toMatchObject({ pagina: 'segunda', secaoId: 'aprovar-um-documento' })
    expect(r[0].trecho).toContain('Aprovador da OM')
    expect(buscarNoManual(paginas, 'publicacao')[0].secaoId).toBe('publicacao')
  })

  it('todas as palavras precisam estar na mesma seção', () => {
    expect(buscarNoManual(paginas, 'cpf cendoc')).toEqual([])
    expect(buscarNoManual(paginas, 'cpf')).toHaveLength(1)
  })

  it('termo vazio ou curto demais não busca', () => {
    expect(buscarNoManual(paginas, '')).toEqual([])
    expect(buscarNoManual(paginas, 'a')).toEqual([])
  })

  it('o título que casa vem antes do que só cita a palavra no texto', () => {
    const r = buscarNoManual(montarPaginas({
      '/src/manual/01-a.md': '# A\n\n## Outra\n\nfalamos de revogação aqui.',
      '/src/manual/02-b.md': '# B\n\n## Revogação\n\nTexto.',
    }), 'revogacao')
    expect(r.map(x => x.pagina)).toEqual(['b', 'a'])
  })
})

describe('manual: Markdown', () => {
  it('cada título ganha id, e o link para outra página vira link interno do manual', () => {
    const html = renderizarManual('## Área de Trabalho\n\nVeja [a NPA](npa#cabecalho-e-assinaturas) e [o site](https://exemplo.mil.br) e [aqui](#area-de-trabalho).')
    expect(html).toContain('<h2 id="area-de-trabalho">')
    expect(html).toContain('href="/manual/npa#cabecalho-e-assinaturas" data-interno')
    expect(html).toContain('href="https://exemplo.mil.br" target="_blank" rel="noopener"')
    expect(html).toContain('href="#area-de-trabalho"')
  })

  it('HTML cru no texto não passa', () => {
    expect(renderizarManual('<script>alert(1)</script>')).not.toContain('<script>')
  })

  it('tabela vem num contêiner que rola', () => {
    expect(renderizarManual('| a | b |\n|---|---|\n| 1 | 2 |')).toContain('<div class="tabela-rolavel"><table>')
  })
})

describe('manual: o conteúdo de verdade', () => {
  const brutos = import.meta.glob('../manual/*.md', { query: '?raw', import: 'default', eager: true })
  const paginas = montarPaginas(brutos)

  it('tem páginas, cada uma com título e conteúdo', () => {
    expect(paginas.length).toBeGreaterThanOrEqual(8)
    for (const p of paginas) {
      expect(p.titulo, p.slug).toBeTruthy()
      expect(p.markdown.length, p.slug).toBeGreaterThan(200)
    }
  })

  it('os perfis citados existem', () => {
    for (const p of paginas) for (const codigo of p.perfis) expect(PAPEIS[codigo], `${p.slug}: ${codigo}`).toBeTruthy()
  })

  it('todo link entre páginas aponta para uma página e uma seção que existem', () => {
    for (const p of paginas) {
      for (const [, alvo] of p.markdown.matchAll(/\]\(([^)\s]+)\)/g)) {
        if (/^https?:/.test(alvo)) continue
        const [slug, ancora] = alvo.split('#')
        if (!slug) {
          expect(p.secoes.some(s => s.id === ancora), `${p.slug}: #${ancora}`).toBe(true)
          continue
        }
        const destino = paginas.find(x => x.slug === slug)
        expect(destino, `${p.slug} -> ${alvo}`).toBeTruthy()
        if (ancora) expect(destino.secoes.some(s => s.id === ancora), `${p.slug} -> ${alvo}`).toBe(true)
      }
    }
  })
})

describe('manual: a ajuda de cada tela', () => {
  // Cada rota (e cada módulo) diz em `manual` a página (e a seção) do manual que a explica: 'pagina' ou 'pagina#secao'.
  const fontes = import.meta.glob(['../router/index.js', '../perfis/index.js'], { query: '?raw', import: 'default', eager: true })
  const brutos = import.meta.glob('../manual/*.md', { query: '?raw', import: 'default', eager: true })
  const paginas = montarPaginas(brutos)
  const referencias = Object.values(fontes).flatMap(texto => [...texto.matchAll(/manual: '([^']+)'/g)].map(m => m[1]))

  it('toda tela com ajuda aponta para uma página e uma seção que existem', () => {
    expect(referencias.length).toBeGreaterThanOrEqual(10)
    for (const ref of referencias) {
      const [slug, secao] = ref.split('#')
      const pagina = paginas.find(p => p.slug === slug)
      expect(pagina, ref).toBeTruthy()
      if (secao) expect(pagina.secoes.some(s => s.id === secao), ref).toBe(true)
    }
  })
})
