<template>
  <q-page class="painel q-pa-xl">

    <q-breadcrumbs active-color="primary" class="q-mb-md" style="font-size:13px">
      <template #separator>
        <q-icon name="mdi-chevron-right" size="16px" color="primary" />
      </template>
      <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
      <q-breadcrumbs-el label="Área de Trabalho" :to="{ name: 'home' }" />
      <q-breadcrumbs-el label="Painel Gerencial" />
    </q-breadcrumbs>

    <div class="q-mb-lg">
      <h1 class="text-h5 text-weight-bold text-primary q-my-none">Painel Gerencial</h1>
      <p class="text-body2 text-grey-7 q-mb-none" data-testid="escopo-do-painel">
        <template v-if="painel">
          Indicadores da gestão normativa — <strong>{{ painel.escopo.omSigla ?? 'todas as OMs' }}</strong>, de
          {{ formatarData(painel.escopo.dataInicio) }} a {{ formatarData(painel.escopo.dataFim) }}.
        </template>
        <template v-else>Indicadores da gestão normativa.</template>
        <span v-if="painel && !painel.escopo.podeFiltrarPorOm"> Você vê só a sua OM.</span>
      </p>
    </div>

    <!-- Filtros: o Painel Gerencial Geral escolhe a OM; o Painel Gerencial da OM não tem esse campo (o servidor fixa a própria OM). -->
    <q-card flat class="cartao q-mb-lg">
      <q-card-section class="row q-col-gutter-md items-center">
        <div class="col-6 col-md-2">
          <q-input v-model="filtros.dataInicio" type="date" label="De" outlined dense hide-bottom-space data-testid="filtro-inicio" />
        </div>
        <div class="col-6 col-md-2">
          <q-input v-model="filtros.dataFim" type="date" label="Até" outlined dense hide-bottom-space data-testid="filtro-fim" />
        </div>
        <div class="col-12 col-md-3">
          <q-select v-model="filtros.especieId" :options="especies" option-value="id" option-label="sigla" emit-value map-options
                    label="Espécie" outlined dense clearable hide-bottom-space data-testid="filtro-especie" />
        </div>
        <div v-if="painel?.escopo.podeFiltrarPorOm" class="col-12 col-md-3">
          <q-select v-model="filtros.omId" :options="omsFiltradas" option-value="id" option-label="sigla" emit-value map-options
                    use-input input-debounce="0" label="OM (todas, se vazio)" outlined dense clearable hide-bottom-space
                    data-testid="filtro-om" @filter="filtrarOms" />
        </div>
        <div class="col-auto">
          <q-btn flat color="primary" icon="mdi-restore" label="Últimos 12 meses" @click="restaurarPeriodo" />
        </div>
      </q-card-section>
      <q-linear-progress v-if="carregando" indeterminate />
    </q-card>

    <q-banner v-if="erro" rounded class="bg-red-1 text-negative q-mb-lg">{{ erro }}</q-banner>

    <template v-if="painel">
      <!-- Resumo: o que importa primeiro. Os quatro primeiros contam o período; os dois últimos, a situação de agora. -->
      <div class="row q-col-gutter-md q-mb-lg" data-testid="resumo">
        <div v-for="k in cartoes" :key="k.chave" class="col-6 col-sm-4 col-md-2">
          <q-card flat class="kpi" :data-testid="`kpi-${k.chave}`" :style="{ '--cor': k.cor }">
            <q-card-section class="q-pa-md">
              <div class="row items-center no-wrap q-mb-sm">
                <q-avatar size="34px" class="kpi-icone" :icon="k.icone" />
                <div class="kpi-rotulo q-ml-sm">{{ k.rotulo }}</div>
              </div>
              <div class="kpi-valor">{{ k.valor }}</div>
              <div class="kpi-nota">{{ k.nota }}</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <div class="row q-col-gutter-lg q-mb-lg">
        <div class="col-12 col-md-8">
          <q-card flat class="cartao full-height">
            <q-card-section>
              <div class="titulo">Documentos por Mês</div>
              <div class="sub">Criados e publicados em cada mês do período</div>
              <div v-if="!painel.porMes.length" class="vazio">Nenhum mês no período.</div>
              <GraficoEcharts v-else :opcao="opcoes.porMes" arquivo="documentos-por-mes" descricao="Documentos criados e publicados por mês" data-testid="grafico-por-mes" />
            </q-card-section>
          </q-card>
        </div>

        <div class="col-12 col-md-4">
          <q-card flat class="cartao full-height">
            <q-card-section>
              <div class="titulo">Criados por Espécie</div>
              <div class="sub">No período</div>
              <div v-if="!painel.porEspecie.length" class="vazio">Nenhum documento criado no período.</div>
              <GraficoEcharts v-else :opcao="opcoes.porEspecie" arquivo="criados-por-especie" descricao="Documentos criados por espécie" />
            </q-card-section>
          </q-card>
        </div>
      </div>

      <div class="row q-col-gutter-lg q-mb-lg">
        <div class="col-12 col-md-5">
          <q-card flat class="cartao full-height">
            <q-card-section>
              <div class="titulo">Tempo em Cada Etapa</div>
              <div class="sub">Mediana e "90% até", em dias, dos documentos criados no período que cumpriram a etapa</div>
              <GraficoEcharts :opcao="opcoes.tempos" height="220px" arquivo="tempo-por-etapa" descricao="Tempo em cada etapa" />
              <q-markup-table flat dense class="q-mt-sm" data-testid="tempos">
                <thead><tr><th class="text-left">Etapa</th><th class="text-right">Mediana</th><th class="text-right">90% Até</th><th class="text-right">Docs</th></tr></thead>
                <tbody>
                  <tr v-for="t in linhasDeTempo" :key="t.rotulo">
                    <td>{{ t.rotulo }}</td>
                    <td class="text-right">{{ formatarDias(t.dados.mediana) }}</td>
                    <td class="text-right">{{ formatarDias(t.dados.p90) }}</td>
                    <td class="text-right">{{ t.dados.n }}</td>
                  </tr>
                </tbody>
              </q-markup-table>
              <div class="text-caption text-grey-6 q-mt-sm">Elaboração: da criação ao envio para aprovação. Aprovação: do envio à aprovação. Publicação: da aprovação à publicação.</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-12 col-md-3">
          <q-card flat class="cartao full-height">
            <q-card-section>
              <div class="titulo">Esperando Agora</div>
              <div class="sub">Fila de cada etapa</div>
              <div class="fila-medidor">
                <GraficoEcharts :opcao="opcoes.filaAprovacao" height="140px" arquivo="fila-aprovacao" descricao="Documentos aguardando aprovação" />
                <div class="legenda-fila"><strong data-testid="fila-aprovacao">{{ painel.filas.aguardandoAprovacao }}</strong> aguardando aprovação</div>
              </div>
              <div class="fila-medidor">
                <GraficoEcharts :opcao="opcoes.filaPublicacao" height="140px" arquivo="fila-publicacao" descricao="Documentos aguardando publicação" />
                <div class="legenda-fila"><strong data-testid="fila-publicacao">{{ painel.filas.aguardandoPublicacao }}</strong> aguardando publicação</div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-12 col-md-4">
          <q-card flat class="cartao full-height">
            <q-card-section>
              <div class="titulo">Situação Atual</div>
              <div class="sub">Todos os documentos, hoje</div>
              <div v-if="!painel.situacaoAtual.length" class="vazio">Nenhum documento.</div>
              <GraficoEcharts v-else :opcao="opcoes.situacao" :height="alturaDaSituacao" arquivo="situacao-atual" descricao="Documentos por situação atual" />
            </q-card-section>
          </q-card>
        </div>
      </div>

      <q-card flat class="cartao q-mb-lg">
        <q-card-section>
          <div class="titulo">Parados Há Mais Tempo</div>
          <div class="sub">Documentos esperando aprovação ou publicação, os mais antigos primeiro</div>
          <q-markup-table flat dense data-testid="gargalos">
            <thead><tr><th class="text-left">Documento</th><th class="text-left">OM</th><th class="text-left">Etapa</th><th class="text-right">Parado há</th></tr></thead>
            <tbody>
              <tr v-if="!painel.gargalos.length"><td colspan="4" class="text-grey-6">Nada esperando.</td></tr>
              <tr v-for="g in painel.gargalos" :key="g.documentoId">
                <td><router-link :to="{ name: 'documento-visualizar', params: { id: g.documentoId } }">{{ g.identificacao }}</router-link></td>
                <td>{{ g.omSigla }}</td>
                <td>{{ rotuloDaEtapa(g.etapa) }}</td>
                <td class="text-right">{{ formatarDias(g.dias) }}</td>
              </tr>
            </tbody>
          </q-markup-table>
        </q-card-section>
      </q-card>

      <!-- Por OM: só faz sentido na visão geral (várias OMs). O Painel Gerencial da OM vê uma OM só, então não há comparação a fazer. -->
      <q-card v-if="painel.escopo.podeFiltrarPorOm && !painel.escopo.omId" flat class="cartao">
        <q-card-section>
          <div class="titulo">Por OM</div>
          <div class="sub">As OMs com mais documentos criados no período</div>
          <div v-if="!painel.porOm.length" class="vazio">Nenhuma OM com movimento.</div>
          <template v-else>
            <GraficoEcharts :opcao="opcoes.porOm" :height="alturaPorOm" arquivo="documentos-por-om" descricao="Documentos por OM" />
            <q-markup-table flat dense class="q-mt-md" data-testid="por-om">
              <thead><tr><th class="text-left">OM</th><th class="text-right">Criados</th><th class="text-right">Publicados</th><th class="text-right">Em Tramitação</th></tr></thead>
              <tbody>
                <tr v-for="o in painel.porOm" :key="o.omId">
                  <td>{{ o.sigla }}</td><td class="text-right">{{ o.criados }}</td><td class="text-right">{{ o.publicados }}</td><td class="text-right">{{ o.emTramitacao }}</td>
                </tr>
              </tbody>
            </q-markup-table>
          </template>
        </q-card-section>
      </q-card>
    </template>
  </q-page>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import GraficoEcharts from '@/components/painel/GraficoEcharts.vue'
import { getPainelGerencial } from '@/api/painelGerencial.js'
import { listOrganizacoesMilitares } from '@/api/usuarios.js'
import { listEspeciesNormativas, normalizeEspecie } from '@/api/referencias.js'
import { situacaoLocalMeta } from '@/utils/statusDocumento.js'
import { filtrarOpcoesDeOm } from '@/utils/habilitacao.js'
import { rotuloDaEtapa, formatarDias, periodoPadrao } from '@/utils/painelGerencial.js'
import {
  CORES, opcaoPorMes, opcaoPorEspecie, opcaoSituacaoAtual, opcaoTempos, opcaoPorOm, opcaoFila,
} from '@/utils/painelGerencialGraficos.js'

const painel = ref(null)
const carregando = ref(false)
const erro = ref('')
const especies = ref([])
const oms = ref([])
const omsFiltradas = ref([])
const filtros = reactive({ ...periodoPadrao(), especieId: null, omId: null })

const formatarData = (aaaaMmDd) => (aaaaMmDd ? aaaaMmDd.split('-').reverse().join('/') : '')

const cartoes = computed(() => {
  const t = painel.value?.totais
  if (!t) return []
  return [
    { chave: 'criados', rotulo: 'Criados', valor: t.criados, nota: 'no período', icone: 'mdi-file-plus-outline', cor: '#0B3D91' },
    { chave: 'publicados', rotulo: 'Publicados', valor: t.publicados, nota: 'no período', icone: 'mdi-publish', cor: '#2E9E6B' },
    { chave: 'revogados', rotulo: 'Revogados', valor: t.revogados, nota: 'no período', icone: 'mdi-file-remove-outline', cor: '#6B7A90' },
    { chave: 'devolvidos', rotulo: 'Devolvidos', valor: t.devolvidos, nota: 'voltaram à minuta, no período', icone: 'mdi-undo-variant', cor: '#D9534F' },
    { chave: 'em-tramitacao', rotulo: 'Em Tramitação', valor: t.emTramitacao, nota: 'agora', icone: 'mdi-timer-sand', cor: '#E0A030' },
    { chave: 'vigentes', rotulo: 'Vigentes', valor: t.vigentes, nota: 'publicados em vigor, agora', icone: 'mdi-check-decagram-outline', cor: '#7A4FBF' },
  ]
})

const linhasDeTempo = computed(() => {
  const t = painel.value?.tempos
  return t ? [
    { rotulo: 'Elaboração', dados: t.elaboracao },
    { rotulo: 'Aprovação', dados: t.aprovacao },
    { rotulo: 'Publicação', dados: t.publicacao },
  ] : []
})

// Os dois medidores de fila usam a mesma escala (a maior das duas filas), para as duas poderem ser comparadas.
const maiorFila = computed(() => Math.max(painel.value?.filas.aguardandoAprovacao ?? 0, painel.value?.filas.aguardandoPublicacao ?? 0))

const opcoes = computed(() => {
  const p = painel.value
  if (!p) return {}
  return {
    porMes: opcaoPorMes(p.porMes),
    porEspecie: opcaoPorEspecie(p.porEspecie),
    situacao: opcaoSituacaoAtual(p.situacaoAtual, s => situacaoLocalMeta(s).label),
    tempos: opcaoTempos(p.tempos),
    porOm: opcaoPorOm(p.porOm),
    filaAprovacao: opcaoFila(p.filas.aguardandoAprovacao, maiorFila.value, CORES.criados),
    filaPublicacao: opcaoFila(p.filas.aguardandoPublicacao, maiorFila.value, CORES.p90),
  }
})

const alturaDaSituacao = computed(() => `${Math.max(220, (painel.value?.situacaoAtual.length ?? 0) * 34 + 24)}px`)
const alturaPorOm = computed(() => `${Math.max(240, (painel.value?.porOm.length ?? 0) * 46 + 60)}px`)

function filtrarOms(valor, update) {
  update(() => { omsFiltradas.value = filtrarOpcoesDeOm(oms.value, valor) })
}

async function carregar() {
  carregando.value = true
  erro.value = ''
  try {
    painel.value = await getPainelGerencial(filtros)
  } catch (e) {
    erro.value = `Não foi possível carregar os indicadores: ${e?.message ?? 'erro desconhecido'}`
  } finally {
    carregando.value = false
  }
}

function restaurarPeriodo() {
  Object.assign(filtros, periodoPadrao())
}

// Recarrega a cada mudança de filtro (datas incompletas esperam).
watch(filtros, () => { if (filtros.dataInicio && filtros.dataFim) carregar() })

onMounted(async () => {
  await carregar()
  try {
    especies.value = (await listEspeciesNormativas()).map(normalizeEspecie).sort((a, b) => a.sigla.localeCompare(b.sigla))
    if (painel.value?.escopo.podeFiltrarPorOm) {
      oms.value = (await listOrganizacoesMilitares()).sort((a, b) => a.sigla.localeCompare(b.sigla))
      omsFiltradas.value = oms.value
    }
  } catch {
    // sem as listas, só os filtros de espécie e de OM ficam vazios; os indicadores já estão na tela
  }
})
</script>

<style scoped>
.painel { background: #F4F6FA; }
.cartao { background: #fff; border-radius: 14px; box-shadow: 0 1px 2px rgba(20, 30, 60, .06), 0 4px 16px rgba(20, 30, 60, .05); }
.titulo { font-size: 16px; font-weight: 600; color: #1A2E5A; }
.sub { font-size: 12.5px; color: #6B7A90; margin-bottom: 12px; }
.vazio { color: #8A94A6; padding: 32px 0; text-align: center; }

.kpi { height: 100%; background: #fff; border-radius: 14px; border-top: 4px solid var(--cor); box-shadow: 0 1px 2px rgba(20, 30, 60, .06), 0 4px 16px rgba(20, 30, 60, .05); }
.kpi-icone { background: color-mix(in srgb, var(--cor) 14%, #fff); color: var(--cor); }
.kpi-rotulo { font-size: 13px; font-weight: 500; color: #4A566B; }
.kpi-valor { font-size: 34px; line-height: 1.1; font-weight: 700; color: var(--cor); font-variant-numeric: tabular-nums; }
.kpi-nota { font-size: 12px; color: #8A94A6; margin-top: 2px; }

.fila-medidor { text-align: center; margin-top: 4px; }
.legenda-fila { font-size: 13px; color: #4A566B; margin-top: -18px; }
.legenda-fila strong { color: #1A2E5A; }
</style>
