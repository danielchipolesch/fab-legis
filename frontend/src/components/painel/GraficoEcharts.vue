<template>
  <div ref="raiz" class="grafico" :style="{ height }" role="img" :aria-label="descricao" />
</template>

<script setup>
// Desenha uma opção do ECharts e a mantém ajustada ao tamanho do cartão. Só os módulos usados são importados (o bundle não leva a biblioteca inteira).
import { ref, watch, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart, GaugeChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent, ToolboxComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, LineChart, PieChart, GaugeChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, ToolboxComponent, CanvasRenderer])

const props = defineProps({
  opcao: { type: Object, required: true },
  height: { type: String, default: '280px' },
  descricao: { type: String, default: 'Gráfico' },
  // Nome do arquivo ao baixar a imagem.
  arquivo: { type: String, default: 'grafico' },
})

// Botão de download da imagem (PNG) em cada gráfico.
const comDownload = (opcao) => ({
  ...opcao,
  toolbox: {
    right: 0, top: -4, itemSize: 15,
    feature: { saveAsImage: { title: 'Baixar imagem', name: props.arquivo, pixelRatio: 2, backgroundColor: '#fff' } },
  },
})

const raiz = ref(null)
const grafico = shallowRef(null)
let observador = null

onMounted(() => {
  grafico.value = echarts.init(raiz.value)
  grafico.value.setOption(comDownload(props.opcao))
  if (typeof ResizeObserver !== 'undefined') {
    observador = new ResizeObserver(() => grafico.value?.resize())
    observador.observe(raiz.value)
  }
})

watch(() => props.opcao, (opcao) => grafico.value?.setOption(comDownload(opcao), true), { deep: true })

onBeforeUnmount(() => {
  observador?.disconnect()
  grafico.value?.dispose()
})
</script>

<style scoped>
.grafico { width: 100%; }
</style>
