// Regras da NPA (Norma Padrão de Ação) no frontend -- espelho das regras do backend (pacote
// domain.regras.comunicacaooficialpadronizada): HierarquiaDeNpa, NumeracaoDeNpa e CabecalhoDaNpa. Os cenários de teste são os mesmos de
// HierarquiaDeNpaTest, NumeracaoDeNpaTest e CabecalhoDaNpaTest, de propósito, para as duas implementações não
// divergirem em silêncio (ver docs/dominio.md, "NPA").

// ─── Hierarquia ────────────────────────────────────────────────────────────────
// Capítulo na raiz; seção sob capítulo; subseção sob seção; parágrafo sob capítulo, seção ou subseção; alínea só sob
// parágrafo. Sem artigo, inciso, parágrafo único nem subalínea. pai === null é a raiz da parte normativa.

const ROTULO_DO_TIPO = {
  capitulo: 'Capítulo',
  secao_normativa: 'Seção',
  subsecao_normativa: 'Subseção',
  paragrafo: 'Parágrafo',
  alinea: 'Alínea',
}

const FILHOS_PERMITIDOS = {
  capitulo: ['secao_normativa', 'paragrafo'],
  secao_normativa: ['subsecao_normativa', 'paragrafo'],
  subsecao_normativa: ['paragrafo'],
  paragrafo: ['alinea'],
  alinea: [],
}

export function filhosPermitidos(pai) {
  const tipos = pai == null ? ['capitulo'] : (FILHOS_PERMITIDOS[pai] ?? [])
  return tipos.map(tipo => ({ tipo, label: ROTULO_DO_TIPO[tipo] }))
}

export function permite(pai, filho) {
  return filhosPermitidos(pai).some(o => o.tipo === filho)
}

// Capítulo, seção e subseção têm título; parágrafo e alínea têm o texto (conteúdo).
export const TIPOS_DE_AGRUPAMENTO = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])

// ─── Numeração ─────────────────────────────────────────────────────────────────
// Algarismo arábico pelo CAMINHO do elemento: capítulo 1, seção 1.1, subseção 1.1.1, parágrafo 1.1.1.1 (direto sob
// capítulo, seção ou subseção), alínea a). Seção, subseção e parágrafo do MESMO pai dividem uma única sequência.
// Todo elemento é numerado; sem emenda, então nada é congelado nem recebe sufixo de letra.
//
// Grava em cada elemento `numero` (a posição) e `_caminho` (o rótulo: "1.2", "a)"): é o `_caminho` que faz
// formatLabel/bodyLabel (utils/numbering.js) rotularem como NPA.

export function letraDaAlinea(posicao) {
  let letra = ''
  for (let n = posicao; n > 0; n = Math.floor((n - 1) / 26)) {
    letra = String.fromCharCode(97 + ((n - 1) % 26)) + letra
  }
  return letra
}

export function renumerar(elementos) {
  let capitulo = 0
  for (const el of elementos ?? []) {
    if (el.tipo !== 'capitulo') continue
    capitulo++
    el.numero = capitulo
    el._caminho = String(capitulo)
    numerarFilhos(el, el._caminho)
  }
}

function numerarFilhos(pai, caminhoDoPai) {
  let posicao = 0
  let alinea = 0
  for (const filho of pai.filhos ?? []) {
    if (filho.tipo === 'secao_normativa' || filho.tipo === 'subsecao_normativa' || filho.tipo === 'paragrafo') {
      posicao++
      filho.numero = posicao
      filho._caminho = `${caminhoDoPai}.${posicao}`
      numerarFilhos(filho, filho._caminho)
    } else if (filho.tipo === 'alinea') {
      alinea++
      filho.numero = alinea
      filho._caminho = `${letraDaAlinea(alinea)})`
    }
  }
}

// Rótulo na árvore lateral: "1 — DISPOSIÇÕES PRELIMINARES", "1.1 — Finalidade", "1.1.1", "a)".
export function rotulo(el) {
  const caminho = el._caminho ?? ''
  if (el.tipo === 'capitulo') return caminho + (el.titulo ? ' — ' + el.titulo.toUpperCase() : '')
  if (el.tipo === 'secao_normativa' || el.tipo === 'subsecao_normativa') return caminho + (el.titulo ? ' — ' + el.titulo : '')
  return caminho
}

// Rótulo inline no corpo do documento (número + dois espaços não separáveis).
const NBSP2 = '\xA0\xA0'
export function rotuloDoCorpo(el) {
  const caminho = el._caminho ?? ''
  return caminho ? caminho + NBSP2 : ''
}

// ─── Cabeçalho e fecho ─────────────────────────────────────────────────────────
// Os mesmos textos de CabecalhoDaNpa (backend) -- PDF, HTML e prévia mostram exatamente o mesmo.

const COMANDO = 'COMANDO DA AERONÁUTICA'
const DISTRIBUICAO = 'OSTENSIVA'
const SEM_ANEXOS = 'NÃO HÁ'
const DATA_EM_BRANCO = '__ ___ ____'
const ELABORADO_POR = 'Elaborado por'
const APROVADO_POR = 'Aprovado por'
const ELABORADOR_EM_BRANCO = '[Nome completo, posto e função]'
const APROVADOR_EM_BRANCO = '[POSTO] FULANO DE TAL'
const MESES_ABREVIADOS = ['JAN', 'FEV', 'MAR', 'ABR', 'MAI', 'JUN', 'JUL', 'AGO', 'SET', 'OUT', 'NOV', 'DEZ']
const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro',
  'novembro', 'dezembro']

// Só a parte da data (yyyy-mm-dd), sem fuso: evita o dia "recuar" ao converter um instante para local.
function partesDaData(valor) {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(valor ?? ''))
  return m ? { ano: Number(m[1]), mes: Number(m[2]), dia: Number(m[3]) } : null
}

// Formato militar da data: "08 NOV 2026".
export function dataMilitar(valor) {
  const d = partesDaData(valor)
  return d ? `${String(d.dia).padStart(2, '0')} ${MESES_ABREVIADOS[d.mes - 1]} ${d.ano}` : null
}

export function dataPorExtenso(valor) {
  const d = partesDaData(valor)
  return d ? `${d.dia} de ${MESES[d.mes - 1]} de ${d.ano}` : null
}

export function letraDoAnexo(ordem) {
  let letra = ''
  for (let n = ordem; n > 0; n = Math.floor((n - 1) / 26)) {
    letra = String.fromCharCode(65 + ((n - 1) % 26)) + letra
  }
  return letra
}

// Uma linha por anexo: "A - X;", "B - Y; e", "C - Z." (um só: "A - X."). Sem anexos: "NÃO HÁ".
export function listaDeAnexos(anexos) {
  if (!anexos?.length) return [SEM_ANEXOS]
  const ordenados = [...anexos].sort((a, b) => a.ordem - b.ordem)
  return ordenados.map((a, i) => {
    const fim = i === ordenados.length - 1 ? '.' : (i === ordenados.length - 2 ? '; e' : ';')
    return `${letraDoAnexo(a.ordem)} - ${a.titulo}${fim}`
  })
}

// As duas linhas da célula EFETIVAÇÃO: "BIO 15" e "02 ABR 2026" (só depois de publicada; senão, em branco). O número
// vem da referência que o backend grava ("Boletim Interno Ostensivo nº 15, de ...") e a data, da data do Boletim.
function efetivacao(documento, publicada) {
  if (!publicada) return ['BIO __', DATA_EM_BRANCO]
  const numero = /nº\s*(\d+)/.exec(documento.bca_referencia ?? '')?.[1] ?? '__'
  return [`BIO ${numero}`, dataMilitar(documento.data_bca_referencia) ?? DATA_EM_BRANCO]
}

// Elaborado por (todos os autores), os blocos escritos pelo autor e Aprovado por (quem aprovou) -- nessa ordem. Os dois
// das pontas saem do documento (campos.elaboradoPor / campos.aprovadoPor); sem ninguém ainda, a linha de orientação
// ou a máscara, para a NPA em elaboração não parecer já aprovada por alguém.
export function assinaturas(campos) {
  const comValor = (linhas, orientacao) => (linhas?.length ? linhas : [orientacao])
  return comDoisPontos([
    { rotulo: ELABORADO_POR, linhas: comValor(campos.elaboradoPor, ELABORADOR_EM_BRANCO) },
    ...(campos.assinaturas ?? []),
    { rotulo: APROVADO_POR, linhas: comValor(campos.aprovadoPor, APROVADOR_EM_BRANCO) },
  ])
}

// "Elaborado por" e "Aprovado por" (com ou sem dois-pontos, em qualquer caixa) são dos blocos automáticos: não se escrevem.
export function rotuloReservado(rotulo) {
  const r = (rotulo ?? '').trim().replace(/:$/, '').trim().toLowerCase()
  return r === ELABORADO_POR.toLowerCase() || r === APROVADO_POR.toLowerCase()
}

// O rótulo do bloco de assinatura leva dois-pontos ("Elaborado por:"), como no modelo, mesmo que o autor não os tenha digitado.
export function comDoisPontos(assinaturas) {
  return (assinaturas ?? []).map(a => {
    const rotulo = (a.rotulo ?? '').trimEnd()
    return rotulo.endsWith(':') ? a : { ...a, rotulo: rotulo + ':' }
  })
}

// documento: o documento do frontend (backendParaFrontend); campos: { setorEmissor, local, assinaturas,
// boletimDaRevogacao }; anexos: [{ ordem, titulo }].
export function cabecalho(documento, campos, anexos) {
  const publicada = (documento.situacao_bca ?? 'NAO_PUBLICADO') !== 'NAO_PUBLICADO'
  const referencia = documento.bca_referencia?.trim()
    ? documento.bca_referencia.trim()
    : 'Boletim Interno Ostensivo nº __, de __ de ______ de ____'
  const aprovacao = documento.data_aprovacao
  const revogacao = campos.boletimDaRevogacao?.trim()
  return {
    linhasDeCima: [COMANDO, (documento.om_nome ?? '').toUpperCase(), campos.setorEmissor],
    identificacao: documento.codigo_documento,
    emissao: dataMilitar(aprovacao) ?? DATA_EM_BRANCO,
    efetivacao: efetivacao(documento, publicada),
    distribuicao: DISTRIBUICAO,
    assunto: documento.titulo,
    anexos: listaDeAnexos(anexos),
    localEData: `${campos.local}, ${dataPorExtenso(aprovacao) ?? '___ de __________ de ____'}`,
    assinaturas: assinaturas(campos),
    publicadaNo: publicada ? `(Publicada no ${referencia})` : null,
    revogadaNo: revogacao ? `(Revogada pelo ${revogacao})` : null,
  }
}
