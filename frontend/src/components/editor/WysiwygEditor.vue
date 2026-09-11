<template>
  <div class="wysiwyg-wrapper">
    <!-- Upload de imagem (input oculto) -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/png,image/jpeg,image/gif,image/webp"
      style="display:none"
      @change="onFileSelected"
    />

    <!-- Toolbar -->
    <div v-if="editor" class="wysiwyg-toolbar row items-center q-px-sm q-py-xs" style="flex-wrap:wrap;gap:4px">

      <q-btn-group outline>
        <q-btn :class="{ active: editor.isActive('bold') }" outline color="primary" @click="editor.chain().focus().toggleBold().run()" icon="mdi-format-bold" size="sm">
          <q-tooltip anchor="top middle" self="bottom middle">Negrito (Ctrl+B)</q-tooltip>
        </q-btn>
        <q-btn :class="{ active: editor.isActive('italic') }" outline color="primary" @click="editor.chain().focus().toggleItalic().run()" icon="mdi-format-italic" size="sm">
          <q-tooltip anchor="top middle" self="bottom middle">Itálico (Ctrl+I)</q-tooltip>
        </q-btn>
        <q-btn :class="{ active: editor.isActive('underline') }" outline color="primary" @click="editor.chain().focus().toggleUnderline().run()" icon="mdi-format-underline" size="sm">
          <q-tooltip anchor="top middle" self="bottom middle">Sublinhado (Ctrl+U)</q-tooltip>
        </q-btn>
      </q-btn-group>

      <q-separator vertical class="q-mx-xs" style="height:24px" />

      <q-btn-group outline>
        <q-btn :class="{ active: editor.isActive({ textAlign: 'left' }) }" outline color="primary" @click="editor.chain().focus().setTextAlign('left').run()" icon="mdi-format-align-left" size="sm" />
        <q-btn :class="{ active: editor.isActive({ textAlign: 'center' }) }" outline color="primary" @click="editor.chain().focus().setTextAlign('center').run()" icon="mdi-format-align-center" size="sm" />
        <q-btn :class="{ active: editor.isActive({ textAlign: 'right' }) }" outline color="primary" @click="editor.chain().focus().setTextAlign('right').run()" icon="mdi-format-align-right" size="sm" />
        <q-btn :class="{ active: editor.isActive({ textAlign: 'justify' }) }" outline color="primary" @click="editor.chain().focus().setTextAlign('justify').run()" icon="mdi-format-align-justify" size="sm" />
      </q-btn-group>

      <q-separator vertical class="q-mx-xs" style="height:24px" />

      <!-- Cor do texto -->
      <q-btn-group outline>
        <q-btn
          outline color="primary" size="sm"
          :class="{ active: editor.isActive('textStyle', { color: '#000000' }) }"
          @click="editor.chain().focus().setColor('#000000').run()"
        >
          <q-icon name="mdi-format-color-text" size="16px" style="color:#000" />
          <q-tooltip anchor="top middle" self="bottom middle">Texto preto</q-tooltip>
        </q-btn>
        <q-btn
          outline color="primary" size="sm"
          :class="{ active: editor.isActive('textStyle', { color: '#CC0000' }) }"
          @click="editor.chain().focus().setColor('#CC0000').run()"
        >
          <q-icon name="mdi-format-color-text" size="16px" style="color:#CC0000" />
          <q-tooltip anchor="top middle" self="bottom middle">Texto vermelho</q-tooltip>
        </q-btn>
        <q-btn
          outline color="primary" size="sm"
          :class="{ active: editor.isActive('highlight', { color: '#FFD600' }) }"
          @click="editor.chain().focus().toggleHighlight({ color: '#FFD600' }).run()"
        >
          <q-icon name="mdi-marker" size="16px" style="color:#FFD600; -webkit-text-stroke: 0.5px #999" />
          <q-tooltip anchor="top middle" self="bottom middle">Marca-texto amarelo</q-tooltip>
        </q-btn>
      </q-btn-group>

      <q-separator vertical class="q-mx-xs" style="height:24px" />

      <q-btn-group outline>
        <q-btn outline color="primary" @click="editor.chain().focus().undo().run()" :disable="!editor.can().undo()" icon="mdi-undo" size="sm">
          <q-tooltip anchor="top middle" self="bottom middle">Desfazer</q-tooltip>
        </q-btn>
        <q-btn outline color="primary" @click="editor.chain().focus().redo().run()" :disable="!editor.can().redo()" icon="mdi-redo" size="sm">
          <q-tooltip anchor="top middle" self="bottom middle">Refazer</q-tooltip>
        </q-btn>
      </q-btn-group>

      <q-separator vertical class="q-mx-xs" style="height:24px" />

      <!-- Table insertion -->
      <q-btn
        outline
        size="sm"
        color="primary"
        @click="editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()"
      >
        <q-icon left name="mdi-table-plus" />
        Tabela
      </q-btn>

      <!-- Edição de estrutura da tabela: só aparece com o cursor dentro de uma
           tabela -- os comandos (addRowAfter, deleteTable etc.) são nativos do
           @tiptap/extension-table, a UI é a única parte que faltava. Também
           checa !readonly explicitamente (os outros botões da toolbar não
           checam, um gap pré-existente fora do escopo desta mudança) porque
           comandos do editor rodam mesmo com a view não-editável -- `editable`
           só bloqueia digitação/clique nativos no ProseMirror, não uma
           chamada direta a editor.commands.*, então sem isto alguém em modo
           leitura conseguiria excluir linha/coluna/tabela pela toolbar. -->
      <template v-if="editor.isActive('table') && !props.readonly">
        <q-btn-group outline>
          <q-btn outline color="primary" size="sm" icon="mdi-table-row-plus-before" @click="editor.chain().focus().addRowBefore().run()" :disable="!editor.can().addRowBefore()">
            <q-tooltip anchor="top middle" self="bottom middle">Inserir linha acima</q-tooltip>
          </q-btn>
          <q-btn outline color="primary" size="sm" icon="mdi-table-row-plus-after" @click="editor.chain().focus().addRowAfter().run()" :disable="!editor.can().addRowAfter()">
            <q-tooltip anchor="top middle" self="bottom middle">Inserir linha abaixo</q-tooltip>
          </q-btn>
          <q-btn outline color="negative" size="sm" icon="mdi-table-row-remove" @click="editor.chain().focus().deleteRow().run()" :disable="!editor.can().deleteRow()">
            <q-tooltip anchor="top middle" self="bottom middle">Excluir linha</q-tooltip>
          </q-btn>
        </q-btn-group>

        <q-btn-group outline>
          <q-btn outline color="primary" size="sm" icon="mdi-table-column-plus-before" @click="editor.chain().focus().addColumnBefore().run()" :disable="!editor.can().addColumnBefore()">
            <q-tooltip anchor="top middle" self="bottom middle">Inserir coluna à esquerda</q-tooltip>
          </q-btn>
          <q-btn outline color="primary" size="sm" icon="mdi-table-column-plus-after" @click="editor.chain().focus().addColumnAfter().run()" :disable="!editor.can().addColumnAfter()">
            <q-tooltip anchor="top middle" self="bottom middle">Inserir coluna à direita</q-tooltip>
          </q-btn>
          <q-btn outline color="negative" size="sm" icon="mdi-table-column-remove" @click="editor.chain().focus().deleteColumn().run()" :disable="!editor.can().deleteColumn()">
            <q-tooltip anchor="top middle" self="bottom middle">Excluir coluna</q-tooltip>
          </q-btn>
        </q-btn-group>

        <q-btn outline color="negative" size="sm" icon="mdi-table-remove" @click="editor.chain().focus().deleteTable().run()">
          <q-tooltip anchor="top middle" self="bottom middle">Excluir tabela</q-tooltip>
        </q-btn>
      </template>

      <q-separator vertical class="q-mx-xs" style="height:24px" />

      <!-- Image insertion -->
      <q-btn
        outline
        size="sm"
        color="primary"
        :loading="uploadando"
        @click="fileInputRef?.click()"
      >
        <q-icon left name="mdi-image-plus" />
        Imagem
        <q-tooltip anchor="top middle" self="bottom middle">Inserir imagem (PNG, JPEG, GIF, WebP)</q-tooltip>
      </q-btn>
    </div>

    <!-- Editor content area -->
    <div class="wysiwyg-content">
      <editor-content :editor="editor" class="tiptap-editor" />
    </div>
  </div>
</template>

<script>
// Módulo (fora do <script setup>, que é escopo por instância): sobrevive à troca
// de instância no remount local->colaborativo (ver :key em DocumentoEditorPage.vue).
// onBeforeUnmount do editor local marca aqui se estava focado; o próximo editor
// (colaborativo, mesmo elemento) consome a flag pra devolver o foco -- sem isso o
// usuário perde o cursor no meio da digitação e as teclas seguintes vão para
// lugar nenhum (document.activeElement volta a ser <body>).
let focoPendente = false
</script>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCursor from '@tiptap/extension-collaboration-cursor'
import { HocuspocusProvider } from '@hocuspocus/provider'
import { editorExtensions, editorExtensionsColaborativas } from '@/editor/extensions.js'
import { useAuthStore } from '@/stores/auth.js'
import { useQuasar } from 'quasar'
import { primeMinioUrlCache } from '@/utils/minioUrls.js'

// Throttle simples (leading+trailing): a primeira chamada roda na hora, chamadas
// subsequentes dentro da janela viram uma única execução ao final dela -- garante
// que o ÚLTIMO estado do editor sempre chega, mesmo que a digitação pare no meio
// da janela.
function throttle(fn, ms) {
  let ultimaExecucao = 0
  let timer = null
  let argsPendentes = null
  function disparar() {
    ultimaExecucao = Date.now()
    timer = null
    fn(...argsPendentes)
  }
  const throttled = (...args) => {
    argsPendentes = args
    const decorrido = Date.now() - ultimaExecucao
    if (decorrido >= ms) {
      if (timer) { clearTimeout(timer); timer = null }
      disparar()
    } else if (!timer) {
      timer = setTimeout(disparar, ms - decorrido)
    }
  }
  throttled.cancel = () => { if (timer) { clearTimeout(timer); timer = null } }
  return throttled
}

const props = defineProps({
  modelValue: { type: String, default: '' },
  readonly:   { type: Boolean, default: false },
  // Presentes juntos => edição colaborativa ao vivo (Yjs/Hocuspocus) para este
  // elemento; ausentes (elemento recém-criado, ainda sem id persistido) => modo local
  // antigo, via modelValue/update:modelValue. A decisão é tomada uma vez, na criação
  // do componente -- DocumentoEditorPage.vue força um remount (:key) exatamente na
  // transição local->colaborativo, quando o elemento ganha o primeiro id real. Ver
  // Fase 4 do plano de colaboração em tempo real.
  documentoId: { type: [String, Number], default: null },
  elementoId:  { type: [String, Number], default: null },
})

// sync-status: só emitido no modo colaborativo -- reflete se o Y.Doc deste elemento
// tem alterações locais ainda não confirmadas pelo servidor (`saving`), já
// confirmadas (`synced`) ou se a conexão caiu (`offline`). DocumentoEditorPage.vue usa
// isso pra alimentar o indicador "Salvo"/"Salvando" no topo, que sem isso nunca mudava
// pra elementos já colaborativos (o autosave antigo, debounce+PATCH /secoes, não
// dispara mais pra conteúdo -- ver Fase 6 do plano de colaboração em tempo real).
// content-live: só emitido no modo colaborativo -- JSON do editor a cada mudança
// (própria ou de outra pessoa, throttled), pra alimentar a prévia (DocumentoPreview)
// em tempo real. update:modelValue continua reservado ao modo local antigo (onde
// modelValue É a fonte de verdade); no colaborativo essa fonte é o Y.Doc, então
// content-live é só um espelho pra exibição, nunca volta a escrever no Y.Doc.
const emit = defineEmits(['update:modelValue', 'sync-status', 'content-live'])

const $q = useQuasar()
const authStore = useAuthStore()
const fileInputRef = ref(null)
const uploadando = ref(false)

function parseContent(val) {
  if (!val) return null
  try { return JSON.parse(val) } catch { return null }
}

// Mesmo cálculo em qualquer navegador para o mesmo usuário -- cor estável do cursor
// de colaboração, sem precisar de um cadastro de cores por usuário.
function corDoUsuario(usuarioId) {
  const paleta = ['#0B3D91', '#B3261E', '#1B7A43', '#8E4EC6', '#C77700', '#0E7C86']
  const indice = Math.abs(Number(usuarioId) || 0) % paleta.length
  return paleta[indice]
}

// Mesmo formato do nome no topbar (ver AppTopBar.vue) -- "CP CHIPOLESCH" em vez do
// nome completo, pra caber ao lado do cursor sem tomar a tela toda.
function rotuloDoUsuario(usuario) {
  if (!usuario) return 'Anônimo'
  if (usuario.postoGraduacaoBigrama && usuario.nomeGuerra) {
    return `${usuario.postoGraduacaoBigrama} ${usuario.nomeGuerra}`
  }
  return usuario.nome ?? 'Anônimo'
}

const colaborativo = !!(props.documentoId && props.elementoId)

let provider = null
let editor
let emitirContentLive = null

if (colaborativo) {
  const collabUrl = import.meta.env.VITE_COLLAB_URL ?? 'ws://127.0.0.1:1234'
  provider = new HocuspocusProvider({
    url: collabUrl,
    name: `documento:${props.documentoId}:elemento:${props.elementoId}`,
    // Função, não string: em caso de reconexão (ex.: o WebSocket cai e o provider
    // tenta de novo sozinho), pega o token MAIS RECENTE da store -- importante porque
    // o access token expira em 15min e é renovado via refresh em client.js.
    token: () => authStore.token,
    onStatus: ({ status }) => {
      // 'connected' | 'connecting' | 'disconnected' (WebSocketStatus do provider) --
      // só cobre o estado da CONEXÃO; salvo/salvando vem das mensagens stateless
      // abaixo, que refletem quando o servidor realmente persistiu no Postgres,
      // não o ACK (quase instantâneo) do próprio WebSocket.
      //
      // 'connecting' NÃO é tratado como offline: como cada elemento é sua própria
      // sala (novo HocuspocusProvider a cada troca -- ver comentário de `colaborativo`
      // acima), toda abertura de elemento passa por 'connecting' antes de 'connected',
      // mesmo com a rede perfeita e o documento já salvo. Tratar isso como offline
      // fazia "Sem conexão" piscar a cada clique, mascarando o caso real (a conexão
      // caiu DEPOIS de já ter conectado uma vez). Só 'disconnected' é offline de
      // verdade.
      if (status === 'connected') emit('sync-status', 'synced')
      else if (status === 'disconnected') emit('sync-status', 'offline')
    },
  })
  // Reconcilia a transição local->colaborativo: entre o snapshot que gerou o
  // primeiro id persistido (ver DocumentoEditorPage.vue) e este remount, o
  // usuário pode ter continuado digitando no editor local antigo -- esse texto
  // só existe em modelValue/editorStore (atualizado a cada tecla via
  // onContentUpdate), nunca chegou ao Y.Doc. Sem isto, o primeiro sync carrega
  // o snapshot mais antigo do Postgres e descarta essas teclas silenciosamente.
  // Roda só uma vez (no primeiro sync) para não sobrescrever edições reais de
  // outra pessoa em reconexões futuras -- e nunca se alguém (outra pessoa já
  // na sala) alterou o Y.Doc antes do reconcile (usuarioEditouAntesDoSync,
  // setado no onUpdate abaixo): sobrescrever o Y.Doc por cima de uma edição
  // concorrente intercala as duas transações e embaralha o texto. O editor
  // nasce `editable: false` (ver useEditor abaixo) exatamente para que a
  // digitação do PRÓPRIO usuário nunca dispare esse onUpdate antes daqui --
  // sem isso, era o caso comum (não o raro): o usuário emenda a digitação do
  // editor local direto no novo editor colaborativo, ainda dentro da janela
  // de conexão da sala, e o reconcile via de que "já tem edição" e desistia,
  // perdendo tudo que foi digitado antes do remount.
  let reconciliadoInicial = false
  let usuarioEditouAntesDoSync = false
  provider.on('synced', () => {
    if (reconciliadoInicial) return
    reconciliadoInicial = true
    if (!usuarioEditouAntesDoSync && editor.value) {
      const localParsed = parseContent(props.modelValue)
      if (localParsed) {
        const atual = JSON.stringify(editor.value.getJSON())
        if (atual !== JSON.stringify(localParsed)) {
          editor.value.commands.setContent(localParsed, false)
        }
      }
    }
    // Libera a edição (travada até aqui) e só agora devolve o foco -- fazer
    // isso a cada 'synced' (inclusive reconexões futuras) roubaria o cursor de
    // quem já está digitando havia tempo; reconciliadoInicial acima garante
    // que só acontece nesta primeira vez.
    editor.value?.setEditable(!props.readonly)
    if (deveDevolverFoco) editor.value?.commands.focus('end')
  })
  // 'saving' | 'saved' | 'error' -- emitido pelo collab/server.js (avisarStatus em
  // onChange/onStoreDocument). unsyncedChanges (contador de updates locais ainda
  // não confirmados pelo WebSocket) NÃO serve pra isso: o ACK do WS é quase
  // instantâneo, bem antes do debounce (2-10s) que realmente grava no banco --
  // usá-lo fazia o indicador "Salvando" piscar tão rápido que ficava
  // imperceptível. A mensagem stateless dispara a cada alteração de QUALQUER
  // pessoa conectada (onChange roda pro Y.Doc inteiro, não só pro autor local),
  // então todo mundo na sala vê o mesmo estado de salvamento.
  provider.on('stateless', ({ payload }) => {
    try {
      const { status } = JSON.parse(payload)
      if (status === 'saving' || status === 'saved' || status === 'error') {
        emit('sync-status', status === 'saved' ? 'synced' : status)
      }
    } catch {
      // mensagem stateless de outra finalidade (ex.: preview de histórico) -- ignora
    }
  })

  // Espelha o conteúdo (próprio ou de outra pessoa, já mesclado pelo CRDT) pra
  // prévia -- sem isso o DocumentoPreview só via o `conteudo` de quando o
  // elemento foi carregado, porque nada mais escreve em editorStore fora deste
  // evento (ver comentário de updateContent em stores/editor.js). Throttled:
  // a prévia refaz a numeração de figuras/artigos inteira a cada chamada, cara
  // demais pra rodar em todo keystroke.
  emitirContentLive = throttle(({ editor }) => {
    emit('content-live', JSON.stringify(editor.getJSON()))
  }, 400)

  // Consome a flag deixada pelo onBeforeUnmount do editor anterior (ver
  // declaração de focoPendente acima) -- devolve o cursor pro editor recém-criado
  // se o usuário estava digitando bem no instante da transição local->colaborativo.
  const deveDevolverFoco = focoPendente
  focoPendente = false

  editor = useEditor({
    // Trancado até o primeiro 'synced' (ver handler acima) -- fecha a janela
    // de corrida em que a digitação do usuário chegaria ao Y.Doc antes da
    // reconciliação rodar, o que fazia o reconcile desistir e perder o texto
    // digitado no editor local antes deste remount.
    editable: false,
    autofocus: false,
    extensions: [
      ...editorExtensionsColaborativas,
      Collaboration.configure({ document: provider.document, field: 'default' }),
      CollaborationCursor.configure({
        provider,
        user: {
          name: rotuloDoUsuario(authStore.usuario),
          color: corDoUsuario(authStore.usuario?.id),
          usuarioId: authStore.usuario?.id ?? null,
        },
        // O y-prosemirror por baixo só filtra o cursor pelo clientID da própria
        // conexão Yjs -- não pelo usuário autenticado. Uma conexão antiga do MESMO
        // usuário que não fechou direito (ex.: outra aba, ou reload em dev/HMR
        // enquanto uma sala estava aberta) ainda aparece como "outra pessoa" com
        // clientID diferente, então o navegador mostra o próprio nome de volta.
        // Filtrando aqui por usuarioId (não só clientID) o cursor nunca aparece
        // pra quem já é o dono dele, venha de onde vier a conexão duplicada.
        render(user) {
          if (user.usuarioId != null && user.usuarioId === authStore.usuario?.id) {
            const vazio = document.createElement('span')
            vazio.style.display = 'none'
            return vazio
          }
          const cursor = document.createElement('span')
          cursor.classList.add('collaboration-cursor__caret')
          cursor.setAttribute('style', `border-color: ${user.color}`)
          const label = document.createElement('div')
          label.classList.add('collaboration-cursor__label')
          label.setAttribute('style', `background-color: ${user.color}`)
          label.insertBefore(document.createTextNode(user.name), null)
          cursor.insertBefore(label, null)
          return cursor
        },
      }),
    ],
    onUpdate(payload) {
      usuarioEditouAntesDoSync = true
      emitirContentLive(payload)
    },
  })
} else {
  // Guarda o último valor que O PRÓPRIO editor emitiu -- ver watch abaixo: sem
  // isso, cada tecla digitada ecoa modelValue de volta pra cá (via
  // onContentUpdate no pai -> editorStore -> prop), e comparar via
  // getJSON()/JSON.stringify (que não é estável: TipTap às vezes inclui/omite
  // `attrs` default como {"textAlign":null} de formas diferentes do JSON
  // recebido) fazia esse eco parecer uma mudança "externa" e chamar
  // setContent() no meio da digitação -- o que derruba o foco do ProseMirror,
  // perdendo qualquer tecla seguinte (o editor fica com o cursor em lugar
  // nenhum). Comparando string-a-string contra o que a gente mesmo emitiu, o
  // eco nunca dispara setContent.
  let ultimoValorEmitido = null

  editor = useEditor({
    content: parseContent(props.modelValue),
    editable: !props.readonly,
    extensions: editorExtensions,
    onUpdate({ editor }) {
      const json = JSON.stringify(editor.getJSON())
      ultimoValorEmitido = json
      emit('update:modelValue', json)
    },
  })

  // Só faz sentido nesse modo -- no colaborativo o Y.Doc é a única fonte de verdade
  // e sincroniza sozinho; reagir a modelValue aqui reintroduziria a possibilidade de
  // sobrescrever o que está sendo digitado ao vivo por outra pessoa.
  watch(() => props.modelValue, (val) => {
    if (!editor.value) return
    if (val === ultimoValorEmitido) return
    const parsed = parseContent(val)
    if (!parsed) return
    editor.value.commands.setContent(parsed, false)
  })
}

watch(() => props.readonly, (val) => {
  editor.value?.setEditable(!val)
})

onBeforeUnmount(() => {
  // Só marca a intenção quando este é o modo local -- o próximo mount só existe
  // por causa da transição local->colaborativo (ver :key em DocumentoEditorPage.vue);
  // no colaborativo->colaborativo (troca de elemento selecionado) o foco explícito
  // do usuário em cada clique já resolve isso, sem precisar da flag.
  if (!colaborativo) focoPendente = !!editor.value?.isFocused
  emitirContentLive?.cancel()
  editor.value?.destroy()
  provider?.destroy()
})

async function onFileSelected(event) {
  const arquivo = event.target.files?.[0]
  event.target.value = ''

  if (!arquivo) return

  uploadando.value = true
  try {
    const form = new FormData()
    form.append('arquivo', arquivo)

    const baseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081/v1'
    const resp = await fetch(`${baseUrl}/imagens/upload`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        ...(authStore.token ? { Authorization: `Bearer ${authStore.token}` } : {}),
      },
      body: form,
    })

    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)

    const { url, urlAssinada } = await resp.json()
    primeMinioUrlCache(url, urlAssinada)
    editor.value?.chain().focus().insertContent({
      type: 'figure',
      attrs: { src: url, alt: '', titulo: '', fonte: '' },
    }).run()
  } catch (err) {
    $q.notify({ type: 'negative', message: `Erro ao enviar imagem: ${err.message}` })
  } finally {
    uploadando.value = false
  }
}
</script>

<style>
.wysiwyg-wrapper {
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  overflow: hidden;
  background: #FAFBFC;
}
.wysiwyg-toolbar {
  background: var(--color-surface);
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
}
.wysiwyg-toolbar .q-btn.active {
  background: rgba(0, 0, 0, 0.12) !important;
  color: #333 !important;
}

.wysiwyg-content {
  padding: 16px 20px;
  min-height: 200px;
}
.tiptap-editor .ProseMirror {
  outline: none;
  font-family: 'Calibri', 'Carlito', 'Segoe UI', Arial, sans-serif;
  font-size: 12pt;
  line-height: 1.8;
  color: #1A1A1A;
}
.tiptap-editor .ProseMirror p {
  margin: 0 0 8px;
  text-align: justify;
}
.tiptap-editor .ProseMirror table {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}
.tiptap-editor .ProseMirror table td,
.tiptap-editor .ProseMirror table th {
  border: 1px solid #ccc;
  padding: 6px 10px;
  min-width: 80px;
}
.tiptap-editor .ProseMirror table th {
  background: rgba(11, 61, 145, 0.08);
  font-weight: bold;
}
.tiptap-editor .ProseMirror-focused {
  outline: none;
}

/* Cursor de colaboração (CollaborationCursor) -- badge colado acima do cursor da outra
   pessoa, igual Google Docs. Sem isso, a extensão ainda funciona (os spans são
   inseridos), mas ficam sem posição/estilo nenhum -- é só CSS, a extensão não traz o
   dela própria de propósito (para cada app estilizar do seu jeito). Cor vem inline via
   style (definida pela extensão a partir de CollaborationCursor.configure({ user })),
   aqui só a forma/posição. */
.tiptap-editor .collaboration-cursor__caret {
  position: relative;
  margin-left: -1px;
  margin-right: -1px;
  border-left: 1px solid;
  border-right: 1px solid;
  word-break: normal;
  pointer-events: none;
}
.tiptap-editor .collaboration-cursor__label {
  position: absolute;
  top: -1.4em;
  left: -1px;
  font-size: 11px;
  font-weight: 600;
  line-height: normal;
  color: #fff;
  padding: 1px 6px;
  border-radius: 4px 4px 4px 0;
  white-space: nowrap;
  user-select: none;
  pointer-events: none;
  z-index: 20;
}
</style>
