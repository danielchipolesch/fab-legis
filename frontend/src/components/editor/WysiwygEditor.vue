<template>
  <div class="wysiwyg-wrapper">
    <!-- Toolbar -->
    <div v-if="editor" class="wysiwyg-toolbar d-flex flex-wrap align-center gap-1 px-3 py-2">

      <!-- Heading levels -->
      <v-btn-group density="compact" variant="outlined" rounded="md" color="primary" divided>
        <v-btn
          :class="{ active: !editor.isActive('heading') }"
          size="small"
          icon="mdi-format-paragraph"
          @click="editor.chain().focus().setParagraph().run()"
        >
          <v-tooltip activator="parent" location="top">Parágrafo normal</v-tooltip>
        </v-btn>
        <v-btn
          :class="{ active: editor.isActive('heading', { level: 1 }) }"
          size="small"
          class="heading-btn"
          @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
        >
          H1
          <v-tooltip activator="parent" location="top">Título 1</v-tooltip>
        </v-btn>
        <v-btn
          :class="{ active: editor.isActive('heading', { level: 2 }) }"
          size="small"
          class="heading-btn"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
        >
          H2
          <v-tooltip activator="parent" location="top">Título 2</v-tooltip>
        </v-btn>
        <v-btn
          :class="{ active: editor.isActive('heading', { level: 3 }) }"
          size="small"
          class="heading-btn"
          @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
        >
          H3
          <v-tooltip activator="parent" location="top">Título 3</v-tooltip>
        </v-btn>
      </v-btn-group>

      <v-divider vertical class="mx-1" style="height:24px" />

      <v-btn-group density="compact" variant="outlined" rounded="md" color="primary" divided>
        <v-btn :class="{ active: editor.isActive('bold') }" @click="editor.chain().focus().toggleBold().run()" icon="mdi-format-bold" size="small">
          <v-tooltip activator="parent" location="top">Negrito (Ctrl+B)</v-tooltip>
        </v-btn>
        <v-btn :class="{ active: editor.isActive('italic') }" @click="editor.chain().focus().toggleItalic().run()" icon="mdi-format-italic" size="small">
          <v-tooltip activator="parent" location="top">Itálico (Ctrl+I)</v-tooltip>
        </v-btn>
        <v-btn :class="{ active: editor.isActive('underline') }" @click="editor.chain().focus().toggleUnderline().run()" icon="mdi-format-underline" size="small">
          <v-tooltip activator="parent" location="top">Sublinhado (Ctrl+U)</v-tooltip>
        </v-btn>
      </v-btn-group>

      <v-divider vertical class="mx-1" style="height:24px" />

      <v-btn-group density="compact" variant="outlined" rounded="md" color="primary" divided>
        <v-btn :class="{ active: editor.isActive({ textAlign: 'left' }) }" @click="editor.chain().focus().setTextAlign('left').run()" icon="mdi-format-align-left" size="small" />
        <v-btn :class="{ active: editor.isActive({ textAlign: 'center' }) }" @click="editor.chain().focus().setTextAlign('center').run()" icon="mdi-format-align-center" size="small" />
        <v-btn :class="{ active: editor.isActive({ textAlign: 'right' }) }" @click="editor.chain().focus().setTextAlign('right').run()" icon="mdi-format-align-right" size="small" />
        <v-btn :class="{ active: editor.isActive({ textAlign: 'justify' }) }" @click="editor.chain().focus().setTextAlign('justify').run()" icon="mdi-format-align-justify" size="small" />
      </v-btn-group>

      <v-divider vertical class="mx-1" style="height:24px" />

      <v-btn-group density="compact" variant="outlined" rounded="md" color="primary" divided>
        <v-btn @click="editor.chain().focus().undo().run()" :disabled="!editor.can().undo()" icon="mdi-undo" size="small">
          <v-tooltip activator="parent" location="top">Desfazer</v-tooltip>
        </v-btn>
        <v-btn @click="editor.chain().focus().redo().run()" :disabled="!editor.can().redo()" icon="mdi-redo" size="small">
          <v-tooltip activator="parent" location="top">Refazer</v-tooltip>
        </v-btn>
      </v-btn-group>

      <v-divider vertical class="mx-1" style="height:24px" />

      <!-- Table insertion -->
      <v-btn
        variant="outlined"
        size="small"
        prepend-icon="mdi-table-plus"
        color="primary"
        rounded="md"
        @click="editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()"
      >
        Tabela
      </v-btn>
    </div>

    <!-- Editor content area -->
    <div class="wysiwyg-content">
      <editor-content :editor="editor" class="tiptap-editor" />
    </div>
  </div>
</template>

<script setup>
import { watch, onMounted, onBeforeUnmount } from 'vue'
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
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
  overflow: hidden;
  background: #FAFBFC;
}
.wysiwyg-toolbar {
  background: rgb(var(--v-theme-surface));
  border-bottom: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}
.wysiwyg-toolbar .v-btn.active {
  background: rgba(var(--v-theme-primary), 0.15) !important;
  color: rgb(var(--v-theme-primary)) !important;
}
.wysiwyg-toolbar .heading-btn {
  font-size: 11px;
  font-weight: 700;
  min-width: 32px;
}
.wysiwyg-content {
  padding: 16px 20px;
  min-height: 200px;
}
.tiptap-editor .ProseMirror {
  outline: none;
  font-family: 'Times New Roman', serif;
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
  background: rgba(26,46,90,0.08);
  font-weight: bold;
}
.tiptap-editor .ProseMirror-focused {
  outline: none;
}
.tiptap-editor .ProseMirror h1 {
  font-size: 18pt;
  font-weight: bold;
  color: #1A2E5A;
  margin: 12px 0 6px;
}
.tiptap-editor .ProseMirror h2 {
  font-size: 14pt;
  font-weight: bold;
  color: #1A2E5A;
  margin: 10px 0 4px;
}
.tiptap-editor .ProseMirror h3 {
  font-size: 12pt;
  font-weight: bold;
  color: #2C4A8A;
  margin: 8px 0 4px;
}
</style>
