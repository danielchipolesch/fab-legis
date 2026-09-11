<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" :persistent="enviando">
    <q-card style="min-width:420px;max-width:760px;width:100%">
      <q-card-section class="text-h6">{{ isRevogacao ? 'Revogar documento' : 'Publicar documento' }}?</q-card-section>
      <q-card-section class="q-pt-none">
        O documento
        <strong>{{ documento?.especie }} {{ documento?.numero_basico }}<template v-if="documento?.numero_secundario">-{{ documento.numero_secundario }}</template></strong>
        terá sua situação alterada para <strong>{{ isRevogacao ? 'REVOGADO' : 'PUBLICADO' }}</strong>.
      </q-card-section>
      <q-separator />
      <q-card-section class="q-pt-md q-pb-sm column q-gutter-y-md">
        <div class="text-caption text-grey-7">
          {{ isRevogacao
            ? 'Informe os dados da Portaria e do BCA que revogam este documento:'
            : 'Informe os dados da Portaria e do BCA que registram esta publicação:' }}
        </div>
        <div class="row q-col-gutter-md">
          <q-input
            v-model="form.orgaoPortaria"
            label="Órgão *"
            outlined dense class="col-3"
            placeholder="Ex: DIRAD"
            lazy-rules
            :rules="[v => !!v?.trim() || 'Informe o órgão']"
            :disable="enviando"
          />
          <q-input
            v-model="form.setorPortaria"
            label="Setor(es) *"
            outlined dense class="col-3"
            placeholder="Ex: PP6"
            lazy-rules
            :rules="[v => !!v?.trim() || 'Informe o setor']"
            :disable="enviando"
          />
          <q-input
            v-model="form.numeroPortaria"
            label="Número *"
            outlined dense class="col-2"
            placeholder="Ex: 1.731"
            lazy-rules
            :rules="[v => !!v?.trim() || 'Obrigatório']"
            :disable="enviando"
          />
          <q-input
            v-model="form.dataPortaria"
            type="date"
            label="Data *"
            outlined dense class="col-4"
            lazy-rules
            :rules="[
              v => !!v || 'Informe a data',
              v => !documento?.data_portaria_referencia || v >= documento.data_portaria_referencia
                || 'Anterior à alteração anterior',
            ]"
            :disable="enviando"
          />
        </div>
        <div class="row q-col-gutter-md">
          <q-input
            v-model="form.numeroBca"
            type="number" min="1" max="366"
            label="Número do BCA *"
            outlined dense class="col-4"
            lazy-rules
            :rules="[
              v => (v !== '' && v !== null && v !== undefined) || 'Informe o número',
              v => (v >= 1 && v <= 366) || 'Deve estar entre 1 e 366',
            ]"
            :disable="enviando"
          />
          <q-input
            v-model="form.dataBca"
            type="date"
            label="Data *"
            outlined dense class="col-4"
            lazy-rules
            :rules="[
              v => !!v || 'Informe a data',
              v => !documento?.data_bca_referencia || v >= documento.data_bca_referencia
                || 'Anterior à alteração anterior',
            ]"
            :disable="enviando"
          />
        </div>
        <template v-if="isRepublicacao">
          <q-separator />
          <div class="text-caption text-grey-7">Prévia da cláusula:</div>
          <div class="text-body2 text-italic">{{ previewClausula }}</div>
        </template>

        <!-- Parte preliminar do documento -- só existe de fato a partir da
             publicação, então é coletada aqui, não durante a edição. Não se
             aplica à revogação, que não republica o conteúdo do documento. -->
        <template v-if="!isRevogacao">
          <q-separator />
          <div class="text-caption text-grey-7">
            Parte preliminar do documento publicado:
          </div>
          <q-input
            v-model="form.epigrafe"
            label="Epígrafe *"
            outlined dense
            placeholder="Ex: Portaria DIRAD/PP6 n° 1.731, de 24 de agosto de 2026"
            lazy-rules
            :rules="[v => !!v?.trim() || 'Informe a epígrafe']"
            :disable="enviando"
          />
          <q-input
            v-model="form.ementa"
            type="textarea" autogrow
            label="Ementa *"
            outlined dense
            lazy-rules
            :rules="[v => !!v?.trim() || 'Informe a ementa']"
            :disable="enviando"
          />
          <q-input
            v-model="form.preambulo"
            type="textarea" autogrow
            label="Preâmbulo *"
            outlined dense
            lazy-rules
            :rules="[v => !!v?.trim() || 'Informe o preâmbulo']"
            :disable="enviando"
          />
          <q-input
            v-model="form.fecho"
            type="textarea" autogrow
            label="Fecho *"
            outlined dense
            lazy-rules
            :rules="[v => !!v?.trim() || 'Informe o fecho']"
            :disable="enviando"
          />
          <q-input
            v-model="form.assinatura"
            type="textarea" autogrow
            label="Assinatura *"
            outlined dense
            lazy-rules
            :rules="[v => !!v?.trim() || 'Informe a assinatura']"
            :disable="enviando"
          />
        </template>

        <q-separator />
        <div class="text-caption text-grey-7">
          PDF da portaria *
        </div>
        <q-uploader
          ref="uploaderRef"
          :url="uploadUrl"
          :headers="uploadHeaders"
          field-name="arquivo"
          label="Portaria (PDF)"
          accept="application/pdf"
          :multiple="false"
          :max-files="1"
          auto-upload
          :disable="enviando"
          flat bordered
          style="max-height:200px;width:100%"
          @uploading="uploadando = true"
          @uploaded="onPdfUploaded"
          @failed="onPdfFailed"
          @removed="form.portariaPdfUrl = ''"
        />
      </q-card-section>
      <q-card-actions align="right" class="q-pb-md q-px-md">
        <q-btn flat label="Cancelar" :disable="enviando" @click="fechar" />
        <q-btn
          unelevated color="primary" :label="isRevogacao ? 'Revogar' : 'Publicar'"
          :loading="enviando"
          :disable="erros.length > 0 || uploadando"
          @click="confirmar"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, reactive, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from '@/stores/auth.js'
import { jDoc, jPara, jText } from '@/stores/documentos.js'
import { BASE_URL } from '@/api/client.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  documento: { type: Object, default: null },
  // Republicação (a partir de ALTERADO/EM_REVOGACAO de um documento já publicado
  // antes) exige a mesma cláusula/tela de revogar -- distingue só a prévia da
  // cláusula e o rótulo, a via de disparo é sempre a mesma (ver PublicacaoPage.vue).
  isRevogacao: { type: Boolean, default: false },
  isRepublicacao: { type: Boolean, default: false },
  enviando: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'confirmar'])

const $q = useQuasar()
const auth = useAuthStore()

function formVazio() {
  return {
    orgaoPortaria: '', setorPortaria: '', numeroPortaria: '', dataPortaria: '',
    numeroBca: '', dataBca: '',
    epigrafe: '', ementa: '', preambulo: '', fecho: '', assinatura: '',
    portariaPdfUrl: '',
  }
}

const form = reactive(formVazio())
const uploaderRef = ref(null)
const uploadando = ref(false)

watch(() => props.modelValue, (aberto) => {
  if (aberto) Object.assign(form, formVazio())
})

const uploadUrl = computed(() => `${BASE_URL}/documentos/${props.documento?.id}/portaria-pdf`)
// q-uploader não passa pelo client.js (http.js), então não herda a injeção
// automática do Authorization -- precisa ser passado explicitamente aqui.
const uploadHeaders = computed(() => [{ name: 'Authorization', value: `Bearer ${auth.token}` }])

function onPdfUploaded(info) {
  uploadando.value = false
  try {
    const resposta = JSON.parse(info.xhr.responseText)
    form.portariaPdfUrl = resposta.url
  } catch {
    $q.notify({ type: 'negative', message: 'Erro ao processar a resposta do upload da portaria.' })
  }
}

function onPdfFailed() {
  uploadando.value = false
  form.portariaPdfUrl = ''
  $q.notify({ type: 'negative', message: 'Erro ao enviar o PDF da portaria.' })
}

const MESES_EXTENSO = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro']

function dataPorExtenso(isoStr) {
  if (!isoStr) return null
  const [y, m, d] = isoStr.split('-')
  return `${parseInt(d, 10)} de ${MESES_EXTENSO[parseInt(m, 10) - 1]} de ${y}`
}

// Prévia da cláusula "Portaria X/Y n° Z, de D, publica no BCA n° W, de D" — só faz
// sentido para republicação, já que é o único momento em que essa cláusula é gerada.
const previewClausula = computed(() => {
  const orgao = form.orgaoPortaria?.trim() || 'ÓRGÃO'
  const setor = form.setorPortaria?.trim() || 'SETOR'
  const numeroPortaria = form.numeroPortaria?.trim() || 'XYZ'
  const dataPortariaExt = dataPorExtenso(form.dataPortaria) || 'DD de MÊS de AAAA'
  const numeroBca = form.numeroBca !== '' && form.numeroBca != null ? form.numeroBca : 'ABC'
  const dataBcaExt = dataPorExtenso(form.dataBca) || 'DD de mês de AAAA'
  return `Portaria ${orgao}/${setor} n° ${numeroPortaria}, de ${dataPortariaExt}, `
    + `publica no BCA n° ${numeroBca}, de ${dataBcaExt}.`
})

// Espelha a validação exibida por campo (via :rules nos q-inputs) para saber se o
// formulário está completo e habilitar o botão Confirmar — não é mais renderizada
// como lista de erros, cada input mostra sua própria mensagem nativamente.
// Datas são strings ISO "YYYY-MM-DD" (tanto as do formulário quanto as vindas do
// backend), então comparação de string já basta para checar ordem cronológica.
const erros = computed(() => {
  const errs = []
  if (!form.orgaoPortaria?.trim()) errs.push('Informe o órgão da portaria.')
  if (!form.setorPortaria?.trim()) errs.push('Informe o setor da portaria.')
  if (!form.numeroPortaria?.trim()) errs.push('Informe o número da portaria.')
  if (!form.dataPortaria) errs.push('Informe a data da portaria.')

  const bcaNum = parseInt(form.numeroBca, 10)
  if (form.numeroBca === '' || isNaN(bcaNum)) {
    errs.push('Informe o número do BCA.')
  } else if (bcaNum < 1 || bcaNum > 366) {
    // O BCA é publicado apenas em dias úteis, então nunca passa de 366 (dias do ano).
    errs.push('O número do BCA deve estar entre 1 e 366.')
  }
  if (!form.dataBca) errs.push('Informe a data do BCA.')

  // A data de cada alteração não pode ser anterior à alteração anterior.
  if (form.dataPortaria && props.documento?.data_portaria_referencia
      && form.dataPortaria < props.documento.data_portaria_referencia) {
    errs.push('A data da portaria não pode ser anterior à da alteração anterior.')
  }
  if (form.dataBca && props.documento?.data_bca_referencia
      && form.dataBca < props.documento.data_bca_referencia) {
    errs.push('A data do BCA não pode ser anterior à da alteração anterior.')
  }

  // Revogar não republica o conteúdo do documento -- não exige a parte preliminar.
  if (!props.isRevogacao) {
    if (!form.epigrafe?.trim()) errs.push('Informe a epígrafe.')
    if (!form.ementa?.trim()) errs.push('Informe a ementa.')
    if (!form.preambulo?.trim()) errs.push('Informe o preâmbulo.')
    if (!form.fecho?.trim()) errs.push('Informe o fecho.')
    if (!form.assinatura?.trim()) errs.push('Informe a assinatura.')
  }
  if (!form.portariaPdfUrl) errs.push('Envie o PDF da portaria.')

  return errs
})

function fechar() {
  if (props.enviando) return
  emit('update:modelValue', false)
}

function confirmar() {
  if (erros.value.length) return
  emit('confirmar', {
    orgaoPortaria: form.orgaoPortaria.trim(),
    setorPortaria: form.setorPortaria.trim(),
    numeroPortaria: form.numeroPortaria.trim(),
    dataPortaria: form.dataPortaria,
    numeroBca: parseInt(form.numeroBca, 10),
    dataBca: form.dataBca,
    epigrafe: props.isRevogacao ? undefined : jDoc(jPara(jText(form.epigrafe.trim()))),
    ementa: props.isRevogacao ? undefined : jDoc(jPara(jText(form.ementa.trim()))),
    preambulo: props.isRevogacao ? undefined : jDoc(jPara(jText(form.preambulo.trim()))),
    fecho: props.isRevogacao ? undefined : jDoc(jPara(jText(form.fecho.trim()))),
    assinatura: props.isRevogacao ? undefined : jDoc(jPara(jText(form.assinatura.trim()))),
    portariaPdfUrl: form.portariaPdfUrl,
  })
}

defineExpose({
  resetUploader: () => uploaderRef.value?.reset(),
})
</script>
