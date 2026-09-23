<template>
  <q-page class="q-pa-xl">
    <div class="q-mb-xl">
      <h1 class="text-h5 text-weight-bold text-primary q-my-none">Busca Textual</h1>
      <p class="text-body2 text-grey-7 q-mb-none">
        Procure por texto dentro dos artigos, parágrafos, incisos e demais dispositivos do acervo — não só por número ou assunto.
      </p>
    </div>

    <q-card flat bordered class="q-mb-lg">
      <q-card-section class="q-pa-md">
        <q-input
          v-model="store.termo"
          label="Buscar no conteúdo dos dispositivos"
          outlined
          autofocus
          clearable
          hide-bottom-space
          @keyup.enter="store.buscarAgora"
        >
          <template #prepend>
            <q-icon name="mdi-magnify" />
          </template>
        </q-input>
        <div class="text-caption text-grey-6 q-mt-sm">
          <q-icon name="mdi-information-outline" size="14px" class="q-mr-xs" />
          Use <code>"aspas"</code> para uma frase exata e <code>-palavra</code> para excluir um termo.
        </div>
      </q-card-section>
    </q-card>

    <div v-if="store.carregando" class="row justify-center q-pa-xl">
      <q-spinner color="primary" size="42px" />
    </div>

    <template v-else-if="store.termoBuscado">
      <div v-if="!store.resultados.length" class="text-center text-grey-6 q-pa-xl">
        <q-icon name="mdi-file-search-outline" size="48px" class="q-mb-sm" />
        <div>Nenhum dispositivo encontrado para "{{ store.termoBuscado }}".</div>
      </div>

      <template v-else>
        <div class="text-caption text-grey-7 q-mb-sm">
          {{ store.totalElements }} {{ store.totalElements === 1 ? 'resultado' : 'resultados' }} para "{{ store.termoBuscado }}"
        </div>

        <q-list separator bordered class="rounded-borders bg-white">
          <q-item
            v-for="(item, idx) in store.resultados"
            :key="`${item.documentoId}-${item.elementoId}-${idx}`"
            clickable
            :to="{ name: 'documento-visualizar', params: { id: item.documentoId }, query: { origem: 'busca' } }"
            class="q-py-md"
          >
            <q-item-section avatar top>
              <q-icon :name="situacaoBcaMeta(item.situacaoBca).icon"
                      :color="situacaoBcaMeta(item.situacaoBca).color" />
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-weight-medium">
                {{ item.codigoDocumento }}
                <span class="text-grey-7 text-weight-regular"> — {{ item.tituloDocumento }}</span>
              </q-item-label>
              <q-item-label caption class="q-mt-xs">
                <q-badge outline color="primary" class="q-mr-sm">{{ TIPO_LABEL[item.tipoItem] ?? item.tipoItem }}</q-badge>
                <q-badge :color="situacaoBcaMeta(item.situacaoBca).color">{{ situacaoBcaMeta(item.situacaoBca).label }}</q-badge>
                <q-badge v-if="temEtapaEmCurso(item.situacaoLocal)" outline :color="situacaoLocalMeta(item.situacaoLocal).color" class="q-ml-xs">{{ situacaoLocalMeta(item.situacaoLocal).label }}</q-badge>
              </q-item-label>
              <q-item-label caption class="q-mt-sm text-body2 text-grey-9" v-html="destacarTrecho(item.trecho)" />
            </q-item-section>
          </q-item>
        </q-list>

        <div class="row justify-center q-mt-lg" v-if="store.totalPages > 1">
          <q-pagination
            :model-value="store.pagina"
            :max="store.totalPages"
            :max-pages="7"
            boundary-numbers
            direction-links
            @update:model-value="store.irParaPagina"
          />
        </div>
      </template>
    </template>
  </q-page>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useBuscaStore } from '@/stores/busca.js'
import { termoDaQuery } from '@/utils/buscaTextual.js'
import { situacaoBcaMeta, situacaoLocalMeta, temEtapaEmCurso } from '@/utils/statusDocumento.js'

const store = useBuscaStore()
const route = useRoute()

// Vindo do botão "Busca no conteúdo" da homepage, o termo digitado lá chega na URL (`?q=`): já
// preenche o campo e executa a busca. Sem `?q=`, mantém a última busca guardada na store.
onMounted(() => {
  const termo = termoDaQuery(route.query)
  if (termo) {
    store.termo = termo
    store.buscarAgora()
  }
})

// Rótulo em português de cada ItemAnexoParteNormativaTipoEnum (backend) --
// cobre as 3 partes (preliminar/normativa/final), já que a busca cruza as três.
const TIPO_LABEL = {
  TITULO: 'Título', CAPITULO: 'Capítulo', SECAO: 'Seção', SUBSECAO: 'Subseção',
  ARTIGO: 'Artigo', PARAGRAFO_NUMERADO: 'Parágrafo', PARAGRAFO_UNICO: 'Parágrafo único',
  INCISO: 'Inciso', ALINEA: 'Alínea', ITEM: 'Item',
  EPIGRAFE: 'Epígrafe', EMENTA: 'Ementa', PREAMBULO: 'Preâmbulo',
  SECAO_NORMATIVA: 'Seção', SUBSECAO_NORMATIVA: 'Subseção', PARAGRAFO: 'Parágrafo', SUB_ALINEA: 'Sub-alínea',
  CLAUSULA_REVOGATORIA: 'Cláusula revogatória', CLAUSULA_VIGENCIA: 'Cláusula de vigência',
  FECHO: 'Fecho', ASSINATURA: 'Assinatura', REFERENDA: 'Referenda',
}

// ts_headline (backend) marca o trecho encontrado com sentinelas de controle
// (/), NUNCA com <mark> literal -- o texto ao redor não é
// escapado pelo Postgres, então tratá-lo como HTML confiável seria um XSS
// armazenado (um parágrafo real pode conter "<"/"&" digitados por um
// usuário). Escapa o texto inteiro primeiro, só DEPOIS troca as sentinelas
// pelas tags <mark> de verdade -- nessa ordem, o que sobra como HTML é só o
// que nós mesmos inserimos.
function destacarTrecho(trecho) {
  if (!trecho) return ''
  const escapado = trecho
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return escapado
    .replaceAll('', '<mark>')
    .replaceAll('', '</mark>')
}
</script>
