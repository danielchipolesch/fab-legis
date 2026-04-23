/**
 * Exportação de PDF seguindo a formatação da NSCA 5-3.
 *
 * Para usar o brasão oficial coloque o arquivo na pasta public/
 * com o nome "brasao-fab.png" — o sistema tentará carregá-lo via fetch
 * automaticamente; caso não encontre, usa um placeholder gerado em canvas.
 */
import { bodyLabel, formatLabel } from './numbering.js'

// ─── Constantes ──────────────────────────────────────────────────────────────

const WATERMARK_CFG = {
  RASCUNHO: { text: 'RASCUNHO', color: 'red',   opacity: 0.14, angle: 45, fontSize: 90, bold: true },
  MINUTA:   { text: 'MINUTA',   color: 'red',   opacity: 0.14, angle: 45, fontSize: 90, bold: true },
  APROVADO: { text: 'APROVADO', color: 'green', opacity: 0.12, angle: 45, fontSize: 90, bold: true },
}

const ESPECIE_COMPLETA = {
  ICA:       'INSTRUÇÃO DO COMANDO DA AERONÁUTICA',
  NSCA:      'NORMA DO SISTEMA DO COMANDO DA AERONÁUTICA',
  Portaria:  'PORTARIA',
  Resolução: 'RESOLUÇÃO',
  Decreto:   'DECRETO',
  Aviso:     'AVISO',
  Mensagem:  'MENSAGEM',
}

// Margens A4 em pontos (1 cm ≈ 28.35 pt): esq 3cm, sup 3cm, dir 2.5cm, inf 3cm
const MARGINS = [85, 85, 71, 85]

// ─── Utilitários ─────────────────────────────────────────────────────────────

function stripHtml(html) {
  const div = document.createElement('div')
  div.innerHTML = html ?? ''
  return (div.textContent || '').trim()
}

function buildDocId(doc) {
  const parts = [doc.especie, doc.numero_basico]
  if (doc.numero_secundario) parts.push(doc.numero_secundario)
  return parts.filter(Boolean).join(' ')
}

function formatDateBR(iso) {
  if (!iso) return ''
  const [y, m, d] = iso.split('-')
  const meses = ['janeiro','fevereiro','março','abril','maio','junho',
                 'julho','agosto','setembro','outubro','novembro','dezembro']
  return `${parseInt(d)} de ${meses[parseInt(m) - 1]} de ${y}`
}

function getElementoByTipo(doc, tipo) {
  for (const sec of doc.secoes ?? []) {
    const el = (sec.elementos ?? []).find(e => e.tipo === tipo)
    if (el) return el
  }
  return null
}

// ─── Brasão (placeholder + opção de imagem real) ─────────────────────────────

function createBrasaoDataUrl() {
  const size = 280
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')
  const cx = size / 2, cy = size / 2, r = size / 2 - 4

  // Anel externo dourado
  ctx.beginPath()
  ctx.arc(cx, cy, r, 0, 2 * Math.PI)
  ctx.fillStyle = '#B8972E'
  ctx.fill()

  // Círculo interno azul
  ctx.beginPath()
  ctx.arc(cx, cy, r - 14, 0, 2 * Math.PI)
  ctx.fillStyle = '#1A2E5A'
  ctx.fill()

  // Estrela central (placeholder da águia)
  ctx.save()
  ctx.translate(cx, cy - 22)
  ctx.fillStyle = '#FFFFFF'
  ctx.font = `bold ${Math.round(size * 0.32)}px serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('✦', 0, 0)
  ctx.restore()

  // Texto inferior
  ctx.fillStyle = '#FFFFFF'
  ctx.font = `bold ${Math.round(size * 0.12)}px Arial`
  ctx.textAlign = 'center'
  ctx.fillText('FORÇA AÉREA', cx, cy + 42)
  ctx.fillText('BRASILEIRA', cx, cy + 64)

  // Borda interna dourada
  ctx.beginPath()
  ctx.arc(cx, cy, r - 14, 0, 2 * Math.PI)
  ctx.strokeStyle = '#B8972E'
  ctx.lineWidth = 3
  ctx.stroke()

  return canvas.toDataURL('image/png')
}

async function loadBrasao() {
  try {
    // Busca a imagem de /public/brasao-fab.png (não passa pelo módulo do Vite,
    // portanto não causa erro de build se o arquivo ainda não existir).
    const resp = await fetch('/brasao-fab.png')
    if (!resp.ok) throw new Error('not found')
    const blob = await resp.blob()
    return await new Promise((res, rej) => {
      const reader = new FileReader()
      reader.onload = () => res(reader.result)
      reader.onerror = rej
      reader.readAsDataURL(blob)
    })
  } catch {
    return createBrasaoDataUrl()
  }
}

// ─── Carregamento dinâmico do pdfmake ────────────────────────────────────────

async function getPdfMake() {
  const [pdfMakeModule, pdfFontsModule] = await Promise.all([
    import('pdfmake/build/pdfmake'),
    import('pdfmake/build/vfs_fonts'),
  ])
  // Normaliza import CJS/ESM: Vite pode entregar como .default ou direto
  const pdfMake  = pdfMakeModule.default  ?? pdfMakeModule
  const pdfFonts = pdfFontsModule.default ?? pdfFontsModule

  // vfs_fonts expõe o dicionário de fontes de formas diferentes conforme a versão
  pdfMake.vfs = pdfFonts?.pdfMake?.vfs ?? pdfFonts?.vfs ?? pdfFonts
  return pdfMake
}

// ─── Capa ────────────────────────────────────────────────────────────────────

function buildCapa(doc, brasao) {
  const docId        = buildDocId(doc)
  const especieNome  = ESPECIE_COMPLETA[doc.especie] ?? doc.especie?.toUpperCase() ?? ''
  const ementaEl     = getElementoByTipo(doc, 'ementa')
  const ementaTxt    = stripHtml(ementaEl?.conteudo ?? '')
  const data         = formatDateBR(doc.data_criacao)
  const org          = doc.organizacao ?? 'COMANDO DA AERONÁUTICA'

  return [
    // Brasão
    { image: brasao, width: 110, alignment: 'center', margin: [0, 20, 0, 12] },

    // Cabeçalho institucional
    { text: 'MINISTÉRIO DA DEFESA',   style: 'capaInstituicao' },
    { text: 'COMANDO DA AERONÁUTICA', style: 'capaInstituicao', margin: [0, 0, 0, 18] },

    // Linha separadora
    hrLine(),

    // Espécie normativa
    { text: especieNome, style: 'capaEspecie', margin: [0, 18, 0, 6] },

    // Identificador numérico
    { text: docId, style: 'capaNumero', margin: [0, 0, 0, 18] },

    // Linha separadora
    hrLine(),

    // Ementa
    ...(ementaTxt ? [{ text: ementaTxt, style: 'capaEmenta', margin: [30, 18, 30, 24] }] : []),

    // Aprovação
    { text: 'APROVAÇÃO:', style: 'capaAprovLabel', margin: [0, 0, 0, 4] },
    { text: org.replace(/^[A-Z]+ — /, '').toUpperCase(), style: 'capaAprovOrg' },

    // Data e local
    { text: `Brasília, ${data}`, style: 'capaData', margin: [0, 24, 0, 0] },

    // Rodapé da capa
    {
      text: `BRASIL — ${new Date().getFullYear()}`,
      style: 'capaRodape',
      absolutePosition: { x: 0, y: 760 },
    },

    { text: '', pageBreak: 'after' },
  ]
}

// ─── Portaria ────────────────────────────────────────────────────────────────

function buildPortaria(doc) {
  const docId       = buildDocId(doc)
  const especieNome = ESPECIE_COMPLETA[doc.especie] ?? doc.especie ?? ''
  const assunto     = doc.assunto_basico ?? ''
  const data        = formatDateBR(doc.data_criacao)
  const org         = (doc.organizacao ?? 'COMANDO DA AERONÁUTICA').replace(/^[A-Z]+ — /, '').toUpperCase()

  return [
    { text: 'PORTARIA', style: 'portariaTitulo', margin: [0, 30, 0, 8] },
    {
      text: [
        { text: 'O COMANDANTE DA AERONÁUTICA', bold: true },
        ', no uso das atribuições que lhe confere o art. 12 da Lei Complementar nº 97, de 9 de junho de 1999, e tendo em vista o que consta do processo nº ___/______-___/___,',
      ],
      style: 'portariaTexto',
      margin: [0, 0, 0, 14],
    },
    { text: 'RESOLVE:', style: 'portariaResolve', margin: [0, 0, 0, 14] },
    {
      text: `Aprovar a ${especieNome} – ${docId}, que dispõe sobre ${assunto}.`,
      style: 'portariaTexto',
      margin: [0, 0, 0, 50],
    },
    { text: `Brasília, ${data}`, style: 'portariaTexto', margin: [0, 0, 0, 50] },
    {
      columns: [
        {
          stack: [
            hrLine(),
            { text: org, alignment: 'center', fontSize: 11, bold: true },
            { text: 'Comandante da Aeronáutica', alignment: 'center', fontSize: 10 },
          ],
          width: 280,
          margin: [80, 0, 0, 0],
        },
      ],
    },
    { text: '', pageBreak: 'after' },
  ]
}

// ─── Renderização recursiva dos elementos normativos ─────────────────────────

const INDENT_PT = { artigo: 0, paragrafo_unico: 28, paragrafo: 28, inciso: 56, alinea: 84, sub_alinea: 112 }

function renderElemento(el) {
  const texto  = stripHtml(el.conteudo)
  const label  = bodyLabel(el)
  const indent = INDENT_PT[el.tipo] ?? 0
  const items  = []

  switch (el.tipo) {
    case 'artigo':
      items.push({
        text: [{ text: `${label} `, bold: true }, texto],
        margin: [indent, 6, 0, 2],
        tocItem:          true,
        tocStyle:         'tocArtigo',
        tocMargin:        [20, 0, 0, 0],
        tocNumberStyle:   { fontSize: 10 },
      })
      break

    case 'paragrafo_unico':
    case 'paragrafo':
      items.push({
        text: [{ text: `${label} `, bold: true }, texto],
        margin: [indent, 4, 0, 2],
      })
      break

    case 'inciso':
    case 'alinea':
    case 'sub_alinea':
      items.push({
        text: [{ text: `${label} `, bold: el.tipo === 'inciso' }, texto],
        margin: [indent, 2, 0, 2],
      })
      break

    default: {
      // Elementos fixos (epígrafe, ementa, preâmbulo, fecho, etc.)
      const labelTxt = formatLabel(el)
      if (texto) {
        items.push({
          stack: [
            ...(labelTxt ? [{ text: labelTxt, style: 'elementLabel' }] : []),
            { text: texto, style: 'elementBody', margin: [0, 2, 0, 0] },
          ],
          margin: [0, 8, 0, 4],
        })
      }
    }
  }

  // Filhos recursivos
  for (const filho of el.filhos ?? []) {
    items.push(...renderElemento(filho))
  }

  return items
}

// ─── Estilos ─────────────────────────────────────────────────────────────────

function buildStyles() {
  return {
    // Capa
    capaInstituicao: { fontSize: 13, bold: false, alignment: 'center', color: '#1A2E5A' },
    capaEspecie:     { fontSize: 13, bold: true,  alignment: 'center', color: '#1A2E5A', characterSpacing: 0.5 },
    capaNumero:      { fontSize: 24, bold: true,  alignment: 'center', color: '#1A2E5A' },
    capaEmenta:      { fontSize: 11, alignment: 'justify' },
    capaAprovLabel:  { fontSize: 10, bold: true,  alignment: 'left', color: '#444' },
    capaAprovOrg:    { fontSize: 10, alignment: 'left', color: '#444' },
    capaData:        { fontSize: 11, alignment: 'center', color: '#333' },
    capaRodape:      { fontSize: 12, bold: true,  alignment: 'center', color: '#1A2E5A' },

    // Portaria
    portariaTitulo:  { fontSize: 14, bold: true,  alignment: 'center', color: '#1A2E5A', decoration: 'underline' },
    portariaTexto:   { fontSize: 12, lineHeight: 1.5, alignment: 'justify' },
    portariaResolve: { fontSize: 13, bold: true,  alignment: 'center' },

    // Sumário
    sumarioTitulo:   { fontSize: 14, bold: true,  alignment: 'center', color: '#1A2E5A', margin: [0, 20, 0, 20] },
    tocSecao:        { fontSize: 11, bold: true,  color: '#1A2E5A' },
    tocArtigo:       { fontSize: 10, color: '#333' },

    // Corpo
    secaoTitulo:     { fontSize: 12, bold: true,  alignment: 'center', color: '#1A2E5A', decoration: 'underline' },
    elementLabel:    { fontSize: 10, bold: true,  color: '#1A2E5A', margin: [0, 0, 0, 2] },
    elementBody:     { fontSize: 12, alignment: 'justify', lineHeight: 1.5 },
  }
}

// ─── Auxiliar: linha horizontal ───────────────────────────────────────────────

function hrLine(color = '#1A2E5A', width = 460) {
  return {
    canvas: [{ type: 'line', x1: 0, y1: 0, x2: width, y2: 0, lineWidth: 1, lineColor: color }],
    margin: [0, 4, 0, 4],
  }
}

// ─── Função principal ────────────────────────────────────────────────────────

export async function exportToPdf(documento) {
  const [pdfMake, brasao] = await Promise.all([getPdfMake(), loadBrasao()])

  const docId  = buildDocId(documento)
  const status = documento.status
  const wm     = WATERMARK_CFG[status] ?? null

  // Número de páginas de pré-texto (capa + portaria + sumário)
  // Usado no footer para iniciar numeração árabe na primeira página do corpo.
  const PRE_PAGES = 3

  const content = [
    // ── Capa
    ...buildCapa(documento, brasao),

    // ── Portaria
    ...buildPortaria(documento),

    // ── Sumário (pdfmake preenche automaticamente os números de página)
    {
      toc: {
        title: { text: 'SUMÁRIO', style: 'sumarioTitulo' },
        textStyle:   { fontSize: 11 },
        numberStyle: { fontSize: 11 },
      },
    },
    { text: '', pageBreak: 'after' },

    // ── Seções
    ...buildCorpo(documento),
  ]

  const dd = {
    pageSize:    'A4',
    pageMargins: MARGINS,
    content,

    // Cabeçalho: aparece a partir da página do corpo (após pré-texto)
    header: (currentPage) => {
      if (currentPage <= PRE_PAGES) return null
      return {
        columns: [
          { text: '', width: '*' },
          {
            text: docId,
            fontSize: 9,
            bold: true,
            color: '#1A2E5A',
            alignment: 'right',
            margin: [0, 24, 60, 0],
          },
        ],
      }
    },

    // Rodapé: sem numeração na capa; romano nas páginas de pré-texto; árabe no corpo
    footer: (currentPage) => {
      if (currentPage === 1) return null // Capa sem rodapé

      let pageText
      if (currentPage === 2) {
        // Portaria
        pageText = ''
      } else if (currentPage === PRE_PAGES) {
        // Sumário: não numera ou usa romano
        pageText = ''
      } else {
        pageText = String(currentPage - PRE_PAGES)
      }

      return pageText
        ? { text: pageText, alignment: 'center', fontSize: 10, color: '#555', margin: [0, 10, 0, 0] }
        : null
    },

    ...(wm ? { watermark: wm } : {}),

    styles:       buildStyles(),
    defaultStyle: { font: 'Roboto', fontSize: 12, lineHeight: 1.5, alignment: 'justify' },
  }

  const filename = [documento.especie, documento.numero_basico, documento.numero_secundario]
    .filter(Boolean).join('-') + '.pdf'

  pdfMake.createPdf(dd).download(filename)
}

// ─── Corpo do documento (seções) ─────────────────────────────────────────────

function buildCorpo(doc) {
  const items = []

  for (const secao of doc.secoes ?? []) {
    items.push({
      text:    secao.titulo,
      style:   'secaoTitulo',
      tocItem: true,
      tocStyle:       'tocSecao',
      tocMargin:      [0, 4, 0, 2],
      tocNumberStyle: { fontSize: 11, bold: true },
      margin:  [0, 8, 0, 10],
    })

    for (const el of secao.elementos ?? []) {
      items.push(...renderElemento(el))
    }

    items.push({ text: '', margin: [0, 12] })
  }

  return items
}
