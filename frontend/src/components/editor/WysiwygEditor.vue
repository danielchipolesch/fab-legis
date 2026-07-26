<template>
  <div class="wysiwyg-wrapper">
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
    </div>

    <!-- Editor content area -->
    <div class="wysiwyg-content">
      <editor-content :editor="editor" class="tiptap-editor" />
    </div>
  </div>
</template>

<script setup>
import { watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import TextStyle from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'

const props = defineProps({
  modelValue: { type: String, default: '' },
  readonly:   { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const editor = useEditor({
  content: props.modelValue,
  editable: !props.readonly,
  extensions: [
    StarterKit,
    Underline,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    TextStyle,
    Color,
    Highlight.configure({ multicolor: true }),
    Table.configure({ resizable: true }),
    TableRow,
    TableHeader,
    TableCell,
  ],
  onUpdate({ editor }) {
    emit('update:modelValue', editor.getHTML())
  },
})

watch(() => props.modelValue, (val) => {
  if (editor.value && editor.value.getHTML() !== val) {
    editor.value.commands.setContent(val, false)
  }
})

watch(() => props.readonly, (val) => {
  editor.value?.setEditable(!val)
})

onBeforeUnmount(() => editor.value?.destroy())
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

</style>
