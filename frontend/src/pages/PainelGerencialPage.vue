<template>
  <q-page class="q-pa-xl">

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

    <!-- Filtros: o Gestor Geral escolhe a OM; o Gestor da OM não tem esse campo (o servidor fixa a própria OM). -->
    <q-card flat bordered class="q-mb-lg">
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
        <div v-for="k in cartoes" :key="k.chave" class="col-6 col-md-2">
          <q-card flat bordered class="kpi" :data-testid="`kpi-${k.chave}`">
            <q-card-section class="q-pa-md">
              <div class="text-caption text-grey-7">{{ k.rotulo }}</div>
              <div class="text-h4 text-weight-bold text-primary">{{ k.valor }}</div>
              <div class="text-caption text-grey-6">{{ k.nota }}</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <div class="row q-col-gutter-lg q-mb-lg">
        <div class="col-12 col-md-7">
          <q-card flat bordered class="full-height">
            <q-card-section>
              <div class="text-subtitle1 text-weight-medium">Documentos por mês</div>
              <div class="text-caption text-grey-7 q-mb-sm">Criados e publicados em cada mês do período</div>
              <div v-if="!painel.porMes.length" class="text-grey-6">Nenhum mês no período.</div>
              <div v-for="m in painel.porMes" :key="m.mes" class="linha-de-barra" data-testid="barra-do-mes">
                <span class="rotulo">{{ rotuloDoMes(m.mes) }}</span>
                <span class="barras">
                  <span class="barra criados" :style="{ width: larguraDaBarra(m.criados, maiorDosMeses) + '%' }" />
                  <span class="barra publicados" :style="{ width: larguraDaBarra(m.publicados, maiorDosMeses) + '%' }" />
                </span>
                <span class="valor">{{ m.criados }} / {{ m.publicados }}</span>
              </div>
              <div class="legenda text-caption text-grey-7 q-mt-sm">
                <span class="chip criados" /> criados &nbsp; <span class="chip publicados" /> publicados
              </div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-12 col-md-5">
          <q-card flat bordered class="full-height">
            <q-card-section>
              <div class="text-subtitle1 text-weight-medium">Criados por espécie</div>
              <div class="text-caption text-grey-7 q-mb-sm">No período</div>
              <div v-if="!painel.porEspecie.length" class="text-grey-6">Nenhum documento criado no período.</div>
              <div v-for="e in painel.porEspecie" :key="e.sigla" class="linha-de-barra">
                <span class="rotulo">{{ e.sigla }}</span>
                <span class="barras"><span class="barra criados" :style="{ width: larguraDaBarra(e.criados, maiorDasEspecies) + '%' }" /></span>
                <span class="valor">{{ e.criados }}</span>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <div class="row q-col-gutter-lg q-mb-lg">
        <div class="col-12 col-md-5">
          <q-card flat bordered class="full-height">
            <q-card-section>
              <div class="text-subtitle1 text-weight-medium">Tempo em cada etapa</div>
              <div class="text-caption text-grey-7 q-mb-sm">Dos documentos criados no período que cumpriram a etapa (mediana e 90% dos casos)</div>
              <q-markup-table flat dense data-testid="tempos">
                <thead><tr><th class="text-left">Etapa</th><th class="text-right">Mediana</th><th class="text-right">90% até</th><th class="text-right">Docs</th></tr></thead>
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
          <q-card flat bordered class="full-height">
            <q-card-section>
              <div class="text-subtitle1 text-weight-medium">Esperando agora</div>
              <div class="text-caption text-grey-7 q-mb-sm">Fila de cada etapa</div>
              <div class="fila"><span class="text-h5 text-weight-bold text-primary" data-testid="fila-aprovacao">{{ painel.filas.aguardandoAprovacao }}</span> aguardando aprovação</div>
              <div class="fila"><span class="text-h5 text-weight-bold text-primary" data-testid="fila-publicacao">{{ painel.filas.aguardandoPublicacao }}</span> aguardando publicação</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-12 col-md-4">
          <q-card flat bordered class="full-height">
            <q-card-section>
              <div class="text-subtitle1 text-weight-medium">Situação atual</div>
              <div class="text-caption text-grey-7 q-mb-sm">Todos os documentos, hoje</div>
              <div v-for="s in painel.situacaoAtual" :key="s.situacao" class="linha-de-barra">
                <span class="rotulo larga">{{ situacaoLocalMeta(s.situacao).label }}</span>
                <span class="barras"><span class="barra criados" :style="{ width: larguraDaBarra(s.quantidade, maiorDasSituacoes) + '%' }" /></span>
                <span class="valor">{{ s.quantidade }}</span>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <q-card flat bordered class="q-mb-lg">
        <q-card-section>
          <div class="text-subtitle1 text-weight-medium">Parados há mais tempo</div>
          <div class="text-caption text-grey-7 q-mb-sm">Documentos esperando aprovação ou publicação, os mais antigos primeiro</div>
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

      <!-- Por OM: só faz sentido na visão geral (várias OMs). O Gestor da OM vê uma OM só, então não há comparação a fazer. -->
      <q-card v-if="painel.escopo.podeFiltrarPorOm && !painel.escopo.omId" flat bordered>
        <q-card-section>
          <div class="text-subtitle1 text-weight-medium">Por OM</div>
          <div class="text-caption text-grey-7 q-mb-sm">As OMs com mais documentos criados no período</div>
          <q-markup-table flat dense data-testid="por-om">
            <thead><tr><th class="text-left">OM</th><th class="text-right">Criados</th><th class="text-right">Publicados</th><th class="text-right">Em tramitação</th></tr></thead>
            <tbody>
              <tr v-if="!painel.porOm.length"><td colspan="4" class="text-grey-6">Nenhuma OM com movimento.</td></tr>
              <tr v-for="o in painel.porOm" :key="o.omId">
                <td>{{ o.sigla }}</td><td class="text-right">{{ o.criados }}</td><td class="text-right">{{ o.publicados }}</td><td class="text-right">{{ o.emTramitacao }}</td>
              </tr>
            </tbody>
          </q-markup-table>
        </q-card-section>
      </q-card>
    </template>
  </q-page>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { getPainelGerencial } from '@/api/painelGerencial.js'
import { listOrganizacoesMilitares } from '@/api/usuarios.js'
import { listEspeciesNormativas, normalizeEspecie } from '@/api/referencias.js'
import { situacaoLocalMeta } from '@/utils/statusDocumento.js'
import { filtrarOpcoesDeOm } from '@/utils/habilitacao.js'
import {
  larguraDaBarra, maiorValor, rotuloDaEtapa, formatarDias, rotuloDoMes, periodoPadrao,
} from '@/utils/painelGerencial.js'

const painel = ref(null)
const carregando = ref(false)
const erro = ref('')
const especies = ref([])
const oms = ref([])
const omsFiltradas = ref([])
const filtros = reactive({ ...periodoPadrao(), especieId: null, omId: null })

const formatarData = (aaaaMmDd) => (aaaaMmDd ? aaaaMmDd.split('-').reverse().join('/') : '')

const maiorDosMeses = computed(() => Math.max(maiorValor(painel.value?.porMes, 'criados'), maiorValor(painel.value?.porMes, 'publicados')))
const maiorDasEspecies = computed(() => maiorValor(painel.value?.porEspecie, 'criados'))
const maiorDasSituacoes = computed(() => maiorValor(painel.value?.situacaoAtual, 'quantidade'))

const cartoes = computed(() => {
  const t = painel.value?.totais
  if (!t) return []
  return [
    { chave: 'criados', rotulo: 'Criados', valor: t.criados, nota: 'no período' },
    { chave: 'publicados', rotulo: 'Publicados', valor: t.publicados, nota: 'no período' },
    { chave: 'revogados', rotulo: 'Revogados', valor: t.revogados, nota: 'no período' },
    { chave: 'devolvidos', rotulo: 'Devolvidos', valor: t.devolvidos, nota: 'voltaram à minuta, no período' },
    { chave: 'em-tramitacao', rotulo: 'Em tramitação', valor: t.emTramitacao, nota: 'agora' },
    { chave: 'vigentes', rotulo: 'Vigentes', valor: t.vigentes, nota: 'publicados em vigor, agora' },
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
.kpi { height: 100%; }
.linha-de-barra { display: flex; align-items: center; gap: 8px; margin: 4px 0; font-size: 13px; }
.linha-de-barra .rotulo { width: 64px; flex: none; color: var(--q-grey-8, #444); }
.linha-de-barra .rotulo.larga { width: 130px; }
.linha-de-barra .barras { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.linha-de-barra .barra { display: block; height: 8px; border-radius: 4px; }
.linha-de-barra .valor { width: 64px; flex: none; text-align: right; font-variant-numeric: tabular-nums; }
.barra.criados, .chip.criados { background: var(--q-primary); }
.barra.publicados, .chip.publicados { background: #2e9e6b; }
.chip { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 4px; }
.fila { margin: 8px 0; }
.fila .text-h5 { margin-right: 6px; }
</style>
