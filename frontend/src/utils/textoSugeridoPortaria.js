import { formatReferenciaLabel, formatLabel, toRoman } from '@/utils/numbering.js'

// Gera um rascunho de texto para a portaria de (re)publicação de um documento
// ALTERADO, seguindo as regras de transcrição do Art. 22 da NSCA 5-3. Usado
// tanto em ComparisonPage.vue (tela de comparação de versões) quanto em
// DocumentViewerPage.vue (tela de visualização) -- extraído pra cá em vez de
// duplicado porque é lógica de negócio substancial, não um helper trivial.
//
// Regras cobertas:
// - I  — transcrição entre aspas + "(NR)" para artigo/dispositivo incluído ou alterado.
// - II — a palavra "revogado" nunca aparece dentro de texto entre aspas (só na cláusula própria).
// - V  — número/letra de dispositivo revogado nunca é reaproveitado (a numeração em si vem de
//        numbering.js/renumberElementsEmAlteracao, este módulo só consome).
// - §1º — sufixo de letra para dispositivo inserido (idem).
// - VI-a — cabeçalho com espécie/número/data da portaria de edição original, seguido de
//          "passa a vigorar com as seguintes alterações".
// - VI-c-1 — linha pontilhada precedida do artigo, quando só um dispositivo interno mudou.
//   NÃO implementa VI-c-2 (duas linhas pontilhadas quando caput E o dispositivo seguinte são
//   ambos preservados) -- simplificação assumida, documentada na UI.
//
// Texto gerado é um rascunho para revisão humana, não um texto jurídico final.

const SECAO_LABELS = {
  PARTE_PRELIMINAR: 'Parte Preliminar',
  PARTE_NORMATIVA: 'Parte Normativa',
  PARTE_FINAL: 'Parte Final',
}
const SECAO_TIPO_FRONTEND = {
  PARTE_PRELIMINAR: 'parte_preliminar',
  PARTE_NORMATIVA: 'parte_normativa',
}

// Ver comentário equivalente que existia em ComparisonPage.vue: a referência
// isolada de um dispositivo (ex.: "Inciso I") não o identifica fora do
// contexto do artigo/agrupamento -- por isso a referência inclui a cadeia de
// ancestrais relevante, cada família filtrando só os ancestrais do próprio
// tipo.
const TIPOS_REPARTICAO_ARTIGO = new Set(['artigo', 'paragrafo', 'paragrafo_unico', 'inciso', 'alinea', 'sub_alinea'])
const TIPOS_AGRUPAMENTO = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])

const MESES_EXTENSO = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro']

function dataPorExtenso(isoStr) {
  if (!isoStr) return null
  const [y, m, d] = isoStr.split('-')
  return `${parseInt(d, 10)} de ${MESES_EXTENSO[parseInt(m, 10) - 1]} de ${y}`
}

// Texto puro (sem HTML) do conteúdo TipTap -- para transcrição entre aspas.
// Mesmo padrão de EditorSidebar.vue/DiffViewer.vue/pdfExport.js (duplicado,
// não extraído -- já é assim nos outros lugares).
function extractText(conteudo) {
  if (!conteudo) return ''
  try {
    const visit = (node) => {
      if (!node) return ''
      if (node.text) return node.text
      if (node.content) return node.content.map(visit).join('')
      return ''
    }
    return visit(JSON.parse(conteudo)).trim()
  } catch {
    return ''
  }
}

// Resolve o elemento vivo na árvore atual do documento a partir de
// secao+elementoId, junto da cadeia de ancestrais -- o histórico de emenda
// não guarda tipo/número do elemento, só o conteúdo antes/depois.
function findElementoComAncestrais(documento, secaoBackend, elementoId) {
  const tipoFrontend = SECAO_TIPO_FRONTEND[secaoBackend]
  const secao = documento?.secoes?.find(s => s.tipo === tipoFrontend)
  if (!secao) return null

  function dfs(elementos, ancestrais) {
    for (const el of elementos) {
      if (String(el.id) === String(elementoId)) return { elemento: el, ancestrais }
      if (el.filhos?.length) {
        const achado = dfs(el.filhos, [...ancestrais, el])
        if (achado) return achado
      }
    }
    return null
  }

  return dfs(secao.elementos ?? [], [])
}

function referenciaPartes(documento, item) {
  const achado = findElementoComAncestrais(documento, item.secao, item.elementoId)
  if (!achado) {
    return {
      ancestrais: [],
      atual: (SECAO_LABELS[item.secao] ?? item.secao) + ' — ' + (item.tituloNovo ?? item.tituloAnterior ?? `#${item.elementoId}`),
    }
  }
  const { elemento, ancestrais } = achado
  let familia = null
  if (TIPOS_REPARTICAO_ARTIGO.has(elemento.tipo)) familia = TIPOS_REPARTICAO_ARTIGO
  else if (TIPOS_AGRUPAMENTO.has(elemento.tipo)) familia = TIPOS_AGRUPAMENTO
  const relevantes = familia ? ancestrais.filter(a => familia.has(a.tipo)) : []
  return {
    ancestrais: relevantes.map(formatReferenciaLabel),
    atual: formatReferenciaLabel(elemento),
  }
}

function referenciaCompleta(documento, item) {
  const { ancestrais, atual } = referenciaPartes(documento, item)
  return ancestrais.length ? `${atual} do ${ancestrais[ancestrais.length - 1]}` : atual
}

// Prefixo do dispositivo dentro de um bloco de artigo parcialmente alterado --
// mesma convenção usada na renderização oficial do corpo do documento (ver
// DocumentoFoCorpoBuilder.java): "§ 2º", "I -", "a)", "1.", "Parágrafo único.".
function prefixoDispositivo(elemento) {
  if (elemento.tipo === 'inciso') return formatLabel(elemento) + ' -'
  return formatLabel(elemento)
}

// Agrupa os itens (exceto REVOGAR) em:
// - diretos: o próprio artigo mudou por inteiro (ou não foi possível localizar
//   um artigo ancestral -- fallback pra transcrição isolada, ex.: parte
//   preliminar/final).
// - porArtigoParcial: dispositivos internos agrupados pelo artigo ancestral,
//   pra montar um único bloco com linha pontilhada por artigo (Art. 22, VI-c-1).
function classificarItens(documento, itens) {
  const diretos = []
  const porArtigoParcial = new Map()
  for (const item of itens) {
    const achado = findElementoComAncestrais(documento, item.secao, item.elementoId)
    if (!achado || achado.elemento.tipo === 'artigo') {
      diretos.push(item)
      continue
    }
    const artigoAncestral = [...achado.ancestrais].reverse().find(a => a.tipo === 'artigo')
    if (!artigoAncestral) {
      diretos.push(item)
      continue
    }
    const label = formatLabel(artigoAncestral)
    if (!porArtigoParcial.has(label)) porArtigoParcial.set(label, [])
    porArtigoParcial.get(label).push(item)
  }
  return { diretos, porArtigoParcial }
}

function blocoArtigoParcial(documento, artigoLabel, itens) {
  const pontilhado = artigoLabel + '  ' + '.'.repeat(60)
  const linhas = itens.map(item => {
    const achado = findElementoComAncestrais(documento, item.secao, item.elementoId)
    const prefixo = achado ? prefixoDispositivo(achado.elemento) : ''
    return `${prefixo}  ${extractText(item.textoNovo)}`.trim()
  })
  return `"${pontilhado}\n${linhas.join('\n')}." (NR)`
}

// itensCicloPendente: itens do mapa de alteração já filtrados pro ciclo
// pendente (cicloReferencia == null) -- quem chama decide isso, este módulo
// só formata.
export function gerarTextoSugeridoPortaria({ documento, itensCicloPendente, portarias, docLabel }) {
  const itens = itensCicloPendente ?? []
  if (!itens.length) return 'Nenhuma alteração pendente neste ciclo.'

  const edicao = (portarias ?? []).find(p => p.tipo === 'EDICAO')
  const numero = [documento?.numero_basico, documento?.numero_secundario].filter(Boolean).join('-')
  const dataExt = edicao ? dataPorExtenso(edicao.dataPortaria) : null
  const cabecalho = dataExt
    ? `${documento?.especie} nº ${numero}, de ${dataExt}, passa a vigorar com as seguintes alterações:`
    : `${docLabel} passa a vigorar com as seguintes alterações:`

  const alterarIncluir = itens.filter(i => i.acao !== 'REVOGAR')
  const revogados = itens.filter(i => i.acao === 'REVOGAR')
  const { diretos, porArtigoParcial } = classificarItens(documento, alterarIncluir)

  const blocos = [
    ...diretos.map(item => `"${referenciaCompleta(documento, item)}  ${extractText(item.textoNovo)}" (NR)`),
    ...Array.from(porArtigoParcial.entries()).map(([label, its]) => blocoArtigoParcial(documento, label, its)),
  ]

  const partes = [cabecalho, ...blocos]

  if (revogados.length) {
    const linhasRevogados = revogados.map((item, i) => `${toRoman(i + 1)} - ${referenciaCompleta(documento, item)};`)
    partes.push(`Ficam revogados os seguintes dispositivos:\n${linhasRevogados.join('\n')}`)
  }

  return partes.join('\n\n')
}
