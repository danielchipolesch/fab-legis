import { regrasDe } from '@/regras/index.js'

// Apresentação dos indicadores do Painel Gerencial (os números vêm prontos do servidor, ver PainelGerencialService).

// Largura (0 a 100) de uma barra, em relação ao maior valor da série; barra vazia se não há nada a comparar.
export function percentual(valor, maximo) {
  if (!maximo || maximo <= 0 || !valor || valor <= 0) return 0
  return Math.min(100, Math.round((valor / maximo) * 100))
}

export const maiorValor = (serie, campo) => (serie ?? []).reduce((m, item) => Math.max(m, item?.[campo] ?? 0), 0)

export const ROTULO_DA_ETAPA = { APROVACAO: 'Aprovação', PUBLICACAO: 'Publicação' }

export const rotuloDaEtapa = (etapa) => ROTULO_DA_ETAPA[etapa] ?? etapa

// "6,5 dias" / "1 dia" / "—" (sem dado).
export function formatarDias(valor) {
  if (valor == null) return '—'
  const texto = Number(valor).toLocaleString('pt-BR', { maximumFractionDigits: 1 })
  return `${texto} ${Number(valor) === 1 ? 'dia' : 'dias'}`
}

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

// '2026-03' -> 'mar/2026'
export function rotuloDoMes(aaaaMm) {
  const [ano, mes] = String(aaaaMm ?? '').split('-')
  return MESES[Number(mes) - 1] ? `${MESES[Number(mes) - 1]}/${ano}` : String(aaaaMm ?? '')
}

const iso = (data) => `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`

// Os últimos 12 meses até hoje, o período com que a tela abre.
export function periodoPadrao(hoje = new Date()) {
  const inicio = new Date(hoje)
  inicio.setFullYear(inicio.getFullYear() - 1)
  return { dataInicio: iso(inicio), dataFim: iso(hoje) }
}

// Largura da barra: proporcional, mas com um mínimo visível (2%) para quem tem algum valor; sem valor, sem barra.
export function larguraDaBarra(valor, maximo) {
  if (!maximo || maximo <= 0 || !valor || valor <= 0) return 0
  return Math.max(2, percentual(valor, maximo))
}

// As espécies que o filtro do painel oferece. O Painel Gerencial Geral nunca conta as NPAs (de uso interno de cada OM): só as espécies
// que não são de uso interno. O da OM vê só as espécies da sua OM -- as que ela está habilitada a criar, mais a NPA (`criaveis`, de
// stores/auth.js) --, restritas ao módulo escolhido, se houver.
export function especiesDoFiltro(especies, criaveis, { geral, tipoDeEspecie } = {}) {
  const todas = especies ?? []
  if (geral) return todas.filter(e => !regrasDe(e.tipoDeEspecie).usoInternoDaOm)
  const siglas = new Set((criaveis ?? []).map(e => e.sigla))
  return todas.filter(e => siglas.has(e.sigla) && (!tipoDeEspecie || e.tipoDeEspecie === tipoDeEspecie))
}
