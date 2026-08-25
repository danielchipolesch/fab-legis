<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)">
    <q-card style="width:720px; max-width:95vw">
      <q-card-section class="row items-center q-pb-sm">
        <q-icon name="mdi-book-open-variant-outline" color="primary" size="24px" class="q-mr-sm" />
        <div class="text-h6 text-primary">Técnica Legislativa — LC 95/1998</div>
        <q-space />
        <q-btn icon="mdi-close" flat round dense v-close-popup />
      </q-card-section>
      <div class="text-caption text-grey-7 q-px-md q-pb-sm">
        Resumo das regras da Lei Complementar nº 95/1998 (alterada pela LC 107/2001), que
        dispõe sobre a elaboração, redação, alteração e consolidação das leis — aplicadas
        neste sistema, por analogia, às normas infralegais do COMAER (Portarias,
        Instruções, Normas de Sistema etc). Consulte o texto oficial para os casos não
        cobertos aqui.
      </div>
      <q-banner dense class="bg-orange-1 text-orange-10 q-mx-md q-mb-sm" rounded>
        <template #avatar>
          <q-icon name="mdi-alert-outline" color="orange-9" />
        </template>
        Este sistema não implementa todas as regras da LC 95/1998. O agrupamento de
        artigos vai só até <strong>Capítulo</strong> (Capítulo → Seção → Subseção) — Título,
        Livro e Parte, previstos na LC 95/1998, não são suportados.
      </q-banner>
      <q-separator />

      <q-card-section class="q-pa-none" style="max-height:65vh; overflow-y:auto">
        <q-list>

          <q-expansion-item default-opened icon="mdi-file-tree-outline" label="Estrutura da legislação (Art. 3º)" header-class="text-weight-bold">
            <q-card-section class="text-body2 q-pt-none">
              <p class="q-mb-sm">Toda legislação se estrutura em três partes:</p>
              <ul class="q-mb-none">
                <li><strong>Parte preliminar</strong> — epígrafe, ementa, preâmbulo, enunciado do objeto e indicação do âmbito de aplicação.</li>
                <li><strong>Parte normativa</strong> — as normas propriamente ditas, incluindo as de caráter transitório.</li>
                <li><strong>Parte final</strong> — cláusula de vigência e, quando couber, cláusula de revogação.</li>
              </ul>
            </q-card-section>
          </q-expansion-item>

          <q-expansion-item icon="mdi-format-list-numbered" label="O artigo, sua unidade básica (Art. 10, I)" header-class="text-weight-bold">
            <q-card-section class="text-body2 q-pt-none">
              <ul class="q-mb-none">
                <li>O artigo é a unidade básica para a articulação do texto, indicado pela abreviatura "Art.", numerado em ordinal até o nono ("Art. 9º") e em cardinal a partir do décimo ("Art. 10").</li>
                <li>Começa com letra maiúscula e termina com ponto — exceto quando desdobrado em incisos, caso em que termina com dois pontos.</li>
                <li>O desdobramento segue, nesta ordem: <strong>artigo</strong> → <strong>parágrafo</strong> (§, ou "Parágrafo único" quando único) → <strong>inciso</strong> (algarismos romanos) → <strong>alínea</strong> (letras minúsculas seguidas de parêntese) → <strong>item</strong> (algarismos arábicos).</li>
                <li>O parágrafo é usado para expressar aspectos complementares ao caput e exceções à regra que ele enuncia — nunca para introduzir assunto novo.</li>
              </ul>
            </q-card-section>
          </q-expansion-item>

          <q-expansion-item icon="mdi-folder-multiple-outline" label="Agrupamento de artigos (Art. 10, II e III)" header-class="text-weight-bold">
            <q-card-section class="text-body2 q-pt-none">
              <p class="q-mb-sm">A LC 95/1998 prevê, na ordem crescente de abrangência:</p>
              <p class="q-mb-none">
                Subseção → Seção → Capítulo →
                <span class="text-grey-6">Título → Livro → Parte</span>.
              </p>
              <p class="q-mt-sm q-mb-none">
                Este sistema implementa só até <strong>Capítulo</strong> — os níveis
                <span class="text-grey-6">Título, Livro e Parte</span> (em cinza acima)
                não existem aqui. Capítulos podem se desdobrar em Seções, e estas em
                Subseções — mas o agrupamento só se justifica quando há mais de uma
                unidade do nível seguinte (não há Seção única dentro de um Capítulo, nem
                Subseção única dentro de uma Seção).
              </p>
            </q-card-section>
          </q-expansion-item>

          <q-expansion-item icon="mdi-pencil-ruler-outline" label="Técnica de redação (Art. 11)" header-class="text-weight-bold">
            <q-card-section class="text-body2 q-pt-none">
              <p class="q-mb-xs text-weight-medium">Para a articulação e a clareza do texto:</p>
              <ul class="q-mb-sm">
                <li>Usar frases curtas e concisas, preferindo a ordem direta.</li>
                <li>Evitar duplo sentido e usar as palavras e expressões em seu sentido comum, salvo quando a norma versar sobre assunto técnico.</li>
              </ul>
              <p class="q-mb-xs text-weight-medium">Para a precisão:</p>
              <ul class="q-mb-sm">
                <li>Expressar a ideia, sempre que possível, em um único artigo, parágrafo, inciso ou alínea.</li>
                <li>Usar os mesmos termos ao se referir ao mesmo conceito, sem alternar sinônimos que possam gerar ambiguidade.</li>
              </ul>
              <p class="q-mb-xs text-weight-medium">Para a ordem lógica:</p>
              <ul class="q-mb-none">
                <li>Reunir sob agrupamento próprio as disposições referentes a um mesmo assunto.</li>
                <li>Restringir cada artigo a um único assunto.</li>
                <li>Apresentar as disposições gerais antes das específicas.</li>
              </ul>
            </q-card-section>
          </q-expansion-item>

          <q-expansion-item icon="mdi-file-edit-outline" label="Alteração de norma já em vigor (Art. 12)" header-class="text-weight-bold">
            <q-card-section class="text-body2 q-pt-none">
              <p class="q-mb-sm">A alteração de dispositivo já vigente se dá de três formas:</p>
              <ul class="q-mb-sm">
                <li>Reprodução integral do texto em nova redação, quando a alteração for considerável.</li>
                <li>Revogação parcial.</li>
                <li>Substituição, no próprio texto, do dispositivo alterado, ou acréscimo de dispositivo novo.</li>
              </ul>
              <p class="q-mb-none">
                Dispositivo novo inserido entre dois já existentes recebe sufixo de letra
                (ex.: um artigo novo entre o Art. 7º e o Art. 8º vira <strong>"Art. 7º-A"</strong>) —
                artigos vigentes nunca são renumerados. É assim que o Quadro Editor deste
                sistema numera elementos incluídos por emenda.
              </p>
            </q-card-section>
          </q-expansion-item>

          <q-expansion-item icon="mdi-calendar-check-outline" label="Vigência e disposições finais (Art. 8º e 9º)" header-class="text-weight-bold">
            <q-card-section class="text-body2 q-pt-none">
              <ul class="q-mb-none">
                <li>A legislação deve indicar expressamente a data em que entra em vigor.</li>
                <li>Normas de pequena repercussão podem entrar em vigor na data de sua publicação.</li>
                <li>A cláusula de revogação, quando houver, deve indicar expressamente as normas revogadas.</li>
                <li>Disposições de natureza transitória, se necessárias, integram a parte final, separadas das disposições permanentes.</li>
              </ul>
            </q-card-section>
          </q-expansion-item>

        </q-list>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
})
defineEmits(['update:modelValue'])
</script>
