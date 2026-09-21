<template>
  <!-- flow-root: as margens do bloco ficam dentro dele, para a altura medida (paginação da prévia) incluí-las. -->
  <div class="bloco" :id="comId ? bloco.idAttr : undefined">
    <div v-if="bloco.tipo === 'elemento'" class="npa-el" :class="`npa-${bloco.el.tipo}`">
      <template v-if="bloco.el.tipo === 'capitulo'">
        <span class="num">{{ bloco.el._caminho }}</span>&nbsp;&nbsp;{{ (bloco.el.titulo || '').toUpperCase() }}
      </template>
      <template v-else-if="bloco.el.tipo === 'secao_normativa' || bloco.el.tipo === 'subsecao_normativa'">
        <span class="num">{{ bloco.el._caminho }}</span>&nbsp;&nbsp;<u>{{ (bloco.el.titulo || '').toUpperCase() }}</u>
      </template>
      <template v-else>
        <span class="num">{{ bloco.el._caminho }}</span>&nbsp;&nbsp;<span class="texto" v-html="bloco.html" />
      </template>
    </div>
    <div v-else-if="bloco.tipo === 'fecho'" class="fecho">{{ bloco.texto }}</div>
    <div v-else-if="bloco.tipo === 'assinatura'" class="assinatura">
      <div>{{ bloco.assinatura.rotulo }}</div>
      <div class="linhas"><div v-for="(linha, j) in bloco.assinatura.linhas" :key="j">{{ linha }}</div></div>
    </div>
    <div v-else class="publicada">{{ bloco.texto }}</div>
  </div>
</template>

<script setup>
// Um pedaço do corpo da NPA (um elemento numerado, o fecho, uma assinatura ou a linha de publicação/revogação), como o
// NpaPreview o distribui nas folhas. comId: só a cópia visível leva o id que a rolagem procura (a cópia usada para medir, não).
defineProps({
  bloco: { type: Object, required: true },
  comId: { type: Boolean, default: false },
})
</script>

<style scoped>
.bloco { display: flow-root; }

.npa-el { padding: 2px 0; text-align: left; }
.npa-capitulo { font-weight: 700; margin-top: 14px; }
.npa-secao_normativa, .npa-subsecao_normativa { margin-top: 8px; }
/* Como no modelo: parágrafo com a primeira linha recuada (1,25 cm); alínea a 2,5 cm com a letra pendurada. */
.npa-paragrafo { text-indent: 47px; }
.npa-alinea { margin-left: 117px; text-indent: -23px; }
.num { font-weight: 700; }
.npa-alinea .num { font-weight: 400; }
.texto :deep(p) { display: inline; margin: 0; }

.fecho { text-align: right; margin-top: 26px; }
.assinatura { text-align: left; margin-top: 26px; }
.assinatura .linhas { text-align: center; margin-top: 30px; }
.publicada { text-align: center; font-size: 12px; margin-top: 24px; }
</style>
