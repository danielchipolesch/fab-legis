<template>
  <!-- Cabeçalho da NPA como no modelo do Anexo XII (mesma grade do PDF): as bordas de cima e dos lados são as da moldura. -->
  <table class="cabecalho">
    <colgroup><col style="width:24.5%"><col style="width:25%"><col style="width:26.5%"><col style="width:24%"></colgroup>
    <tbody>
      <tr style="height:72px">
        <td colspan="4" class="topo">
          <div v-for="(linha, i) in c.linhasDeCima" :key="i" class="rotulo">{{ linha }}</div>
        </td>
      </tr>
      <tr style="height:28px">
        <td rowspan="3" class="e" />
        <td colspan="2" class="e">DATAS</td>
        <td rowspan="2">DISTRIBUIÇÃO</td>
      </tr>
      <tr style="height:29px">
        <td class="e">EMISSÃO</td>
        <td class="e">EFETIVAÇÃO</td>
      </tr>
      <tr style="height:44px">
        <td rowspan="2" class="e">{{ c.emissao }}</td>
        <td rowspan="2" class="e"><div v-for="(linha, i) in c.efetivacao" :key="i">{{ linha }}</div></td>
        <td rowspan="2">{{ c.distribuicao }}</td>
      </tr>
      <tr style="height:37px">
        <td class="e">{{ c.identificacao }}</td>
      </tr>
      <tr style="height:47px">
        <td class="e">ASSUNTO</td>
        <td colspan="3" class="j">{{ c.assunto }}</td>
      </tr>
      <tr style="height:40px">
        <td class="e">ANEXOS</td>
        <td colspan="3" class="j"><div v-for="(linha, i) in c.anexos" :key="i">{{ linha }}</div></td>
      </tr>
    </tbody>
  </table>
</template>

<script setup>
// c: o resultado de cabecalho() de perfis/npa.js.
defineProps({ c: { type: Object, required: true } })
</script>

<style scoped>
table.cabecalho { width: 100%; border-collapse: collapse; table-layout: fixed; font-size: 14px; text-align: center; }
table.cabecalho td { border-bottom: 1px solid #000; padding: 3px 6px; vertical-align: middle; }
table.cabecalho td.e { border-right: 1px solid #000; }
table.cabecalho td.j { text-align: justify; }
table.cabecalho td.topo { vertical-align: bottom; }
.rotulo { font-weight: 700; }
</style>
