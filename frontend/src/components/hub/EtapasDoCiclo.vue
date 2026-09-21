<template>
  <!-- As etapas do ciclo do documento (utils/fluxoDocumento.js, etapasDoCiclo): as já cumpridas cheias, a atual em
       destaque, as que faltam só com contorno. Só mostra -- nada aqui é clicável. -->
  <div class="etapas row no-wrap items-start" data-testid="etapas-do-ciclo">
    <div v-for="(etapa, i) in ciclo.etapas" :key="etapa.chave" class="etapa col column items-center">
      <div class="linha-do-passo row items-center no-wrap full-width">
        <div class="fio col" :class="{ invisivel: i === 0, cumprido: i <= ciclo.atual }" />
        <div class="bolinha row items-center justify-center" :class="estado(i)" :data-estado="estado(i)">
          <q-icon v-if="estado(i) === 'cumprida'" name="mdi-check" size="16px" />
          <span v-else>{{ i + 1 }}</span>
        </div>
        <div class="fio col" :class="{ invisivel: i === ciclo.etapas.length - 1, cumprido: i < ciclo.atual }" />
      </div>
      <div class="rotulo text-center" :class="{ 'text-weight-bold text-primary': estado(i) === 'atual' }">{{ etapa.rotulo }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { etapasDoCiclo } from '@/utils/fluxoDocumento.js'

const props = defineProps({ doc: { type: Object, required: true } })

const ciclo = computed(() => etapasDoCiclo(props.doc))

function estado(i) {
  if (i < ciclo.value.atual) return 'cumprida'
  return i === ciclo.value.atual ? 'atual' : 'futura'
}
</script>

<style scoped>
.etapa { min-width: 0; }
.bolinha {
  width: 32px; height: 32px; border-radius: 50%; flex: none;
  font-size: 13px; font-weight: 700; box-sizing: border-box;
}
.bolinha.cumprida { background: #c5d3ee; color: #0b3d91; }
.bolinha.atual { width: 38px; height: 38px; background: #0b3d91; color: #fff; }
.bolinha.futura { background: #fff; color: #7a8599; border: 2px solid #c5cad6; }
.fio { height: 2px; background: #d6dae3; }
.fio.cumprido { background: #7f9bd1; }
.invisivel { visibility: hidden; }
.rotulo { margin-top: 6px; font-size: 12px; color: #4a5568; line-height: 1.2; padding: 0 2px; }
</style>
