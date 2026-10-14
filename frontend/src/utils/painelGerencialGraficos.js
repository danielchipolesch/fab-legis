// Opções (ECharts) dos gráficos do Painel Gerencial. Funções puras sobre o JSON do servidor, sem tocar no DOM, para ficarem testáveis;
// quem desenha é components/painel/GraficoEcharts.vue.
import { rotuloDoMes } from '@/utils/painelGerencial.js'

export const CORES = {
  criados: '#0B3D91',
  publicados: '#2E9E6B',
  mediana: '#0B3D91',
  p90: '#E0A030',
  eixo: '#8A94A6',
  grade: '#E6E9EF',
  texto: '#2B3445',
  // Série para fatias/barras categóricas (espécies, situações), sem repetir tons vizinhos.
  serie: ['#0B3D91', '#2E9E6B', '#E0A030', '#7A4FBF', '#D9534F', '#2A9DB8', '#8C6D46', '#6B7A90'],
}

const base = {
  textStyle: { fontFamily: 'inherit', color: CORES.texto },
  animationDuration: 500,
}

const dica = { backgroundColor: '#fff', borderColor: CORES.grade, textStyle: { color: CORES.texto }, extraCssText: 'box-shadow:0 4px 14px rgba(20,30,60,.12)' }

// Criados x publicados por mês: colunas + linha, com o eixo em "mmm/aaaa".
export function opcaoPorMes(porMes = []) {
  return {
    ...base,
    color: [CORES.criados, CORES.publicados],
    tooltip: { ...dica, trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, left: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10, textStyle: { color: CORES.eixo } },
    grid: { left: 8, right: 8, top: 36, bottom: 4, containLabel: true },
    xAxis: { type: 'category', data: porMes.map(m => rotuloDoMes(m.mes)), axisLine: { lineStyle: { color: CORES.grade } }, axisTick: { show: false }, axisLabel: { color: CORES.eixo } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: CORES.grade } }, axisLabel: { color: CORES.eixo } },
    series: [
      { name: 'Criados', type: 'bar', barMaxWidth: 22, itemStyle: { borderRadius: [4, 4, 0, 0] }, data: porMes.map(m => m.criados) },
      { name: 'Publicados', type: 'line', smooth: true, symbolSize: 7, lineStyle: { width: 3 }, areaStyle: { opacity: 0.12 }, data: porMes.map(m => m.publicados) },
    ],
  }
}

// Criados por espécie: rosca, com o total no centro.
export function opcaoPorEspecie(porEspecie = []) {
  const total = porEspecie.reduce((s, e) => s + (e.criados ?? 0), 0)
  return {
    ...base,
    color: CORES.serie,
    tooltip: { ...dica, trigger: 'item', formatter: '{b}: <b>{c}</b> ({d}%)' },
    legend: { bottom: 0, type: 'scroll', icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: CORES.eixo } },
    title: { text: String(total), subtext: total === 1 ? 'documento' : 'documentos', left: 'center', top: '34%', textStyle: { fontSize: 28, fontWeight: 700, color: CORES.texto }, subtextStyle: { color: CORES.eixo } },
    series: [{
      type: 'pie', radius: ['52%', '74%'], center: ['50%', '42%'], avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 }, label: { show: false },
      emphasis: { scaleSize: 6 },
      data: porEspecie.map(e => ({ name: e.sigla, value: e.criados })),
    }],
  }
}

// Situação atual: barras horizontais ordenadas (a maior em cima). `rotulo(situacao)` traduz o código.
export function opcaoSituacaoAtual(situacoes = [], rotulo = s => s) {
  const ordenadas = [...situacoes].sort((a, b) => a.quantidade - b.quantidade) // eixo categórico cresce de baixo para cima
  return {
    ...base,
    color: [CORES.criados],
    tooltip: { ...dica, trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 32, top: 4, bottom: 4, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: CORES.grade } }, axisLabel: { color: CORES.eixo } },
    yAxis: { type: 'category', data: ordenadas.map(s => rotulo(s.situacao)), axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: CORES.texto } },
    series: [{
      type: 'bar', barMaxWidth: 18, itemStyle: { borderRadius: [0, 6, 6, 0] },
      label: { show: true, position: 'right', color: CORES.texto },
      data: ordenadas.map(s => s.quantidade),
    }],
  }
}

// Tempo em cada etapa: mediana e "90% até", em dias.
export function opcaoTempos(tempos) {
  const etapas = [['Elaboração', tempos?.elaboracao], ['Aprovação', tempos?.aprovacao], ['Publicação', tempos?.publicacao]]
  return {
    ...base,
    color: [CORES.mediana, CORES.p90],
    tooltip: { ...dica, trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: v => (v == null ? '—' : `${v} dias`) },
    legend: { top: 0, left: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10, textStyle: { color: CORES.eixo } },
    grid: { left: 8, right: 8, top: 36, bottom: 4, containLabel: true },
    xAxis: { type: 'category', data: etapas.map(([nome]) => nome), axisTick: { show: false }, axisLine: { lineStyle: { color: CORES.grade } }, axisLabel: { color: CORES.texto } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: CORES.grade } }, axisLabel: { color: CORES.eixo } },
    series: [
      { name: 'Mediana', type: 'bar', barMaxWidth: 26, itemStyle: { borderRadius: [4, 4, 0, 0] }, data: etapas.map(([, d]) => d?.mediana ?? null) },
      { name: '90% até', type: 'bar', barMaxWidth: 26, itemStyle: { borderRadius: [4, 4, 0, 0] }, data: etapas.map(([, d]) => d?.p90 ?? null) },
    ],
  }
}

// Por OM: barras empilhadas (publicados + em tramitação) das OMs com mais movimento, mais os criados ao lado.
export function opcaoPorOm(porOm = []) {
  const oms = [...porOm].reverse() // a primeira da lista (mais criados) fica em cima
  return {
    ...base,
    color: [CORES.criados, CORES.publicados, CORES.p90],
    tooltip: { ...dica, trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, left: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10, textStyle: { color: CORES.eixo } },
    grid: { left: 8, right: 24, top: 36, bottom: 4, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: CORES.grade } }, axisLabel: { color: CORES.eixo } },
    yAxis: { type: 'category', data: oms.map(o => o.sigla), axisTick: { show: false }, axisLine: { show: false }, axisLabel: { color: CORES.texto } },
    series: [
      { name: 'Criados', type: 'bar', barMaxWidth: 14, itemStyle: { borderRadius: [0, 4, 4, 0] }, data: oms.map(o => o.criados) },
      { name: 'Publicados', type: 'bar', barMaxWidth: 14, itemStyle: { borderRadius: [0, 4, 4, 0] }, data: oms.map(o => o.publicados) },
      { name: 'Em tramitação', type: 'bar', barMaxWidth: 14, itemStyle: { borderRadius: [0, 4, 4, 0] }, data: oms.map(o => o.emTramitacao) },
    ],
  }
}

// Fila de uma etapa: medidor (0 até o maior entre as filas, mínimo 10) para o número de documentos esperando.
export function opcaoFila(valor = 0, maximo = 10, cor = CORES.criados) {
  const teto = Math.max(maximo, 10)
  return {
    ...base,
    series: [{
      type: 'gauge', startAngle: 200, endAngle: -20, min: 0, max: teto, radius: '100%', center: ['50%', '62%'],
      progress: { show: true, width: 14, roundCap: true, itemStyle: { color: cor } },
      axisLine: { lineStyle: { width: 14, color: [[1, CORES.grade]] }, roundCap: true },
      pointer: { show: false }, axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false },
      detail: { valueAnimation: true, fontSize: 30, fontWeight: 700, offsetCenter: [0, '-4%'], color: CORES.texto, formatter: '{value}' },
      data: [{ value: valor }],
    }],
  }
}

