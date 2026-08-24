<template>
  <q-page class="q-pa-xl">

    <div class="row items-center justify-between q-mb-xl">
      <div>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Gestão de Usuários</h1>
        <p class="text-body2 text-grey-7 q-mb-none">
          Cadastro e administração dos usuários com acesso autorizado ao sistema
        </p>
      </div>
      <q-btn color="primary" unelevated size="lg" @click="abrirCriacao">
        <q-icon left name="mdi-account-plus-outline" />
        Novo Usuário
      </q-btn>
    </div>

    <q-card flat bordered>
      <q-table
        :rows="usuarios"
        :columns="columns"
        row-key="id"
        :loading="carregando"
        :rows-per-page-options="[15, 25, 50]"
        :pagination="{ rowsPerPage: 15, sortBy: 'nome' }"
        flat
      >
        <template #body-cell-posto="props">
          <q-td :props="props">
            <q-chip v-if="props.row.postoGraduacaoBigrama" dense square size="sm" color="blue-grey-2" text-color="blue-grey-10">
              {{ props.row.postoGraduacaoBigrama }}
              <q-tooltip>{{ props.row.postoGraduacaoNome }}</q-tooltip>
            </q-chip>
            <span v-else class="text-grey-5 text-caption">—</span>
          </q-td>
        </template>

        <template #body-cell-nomeGuerra="props">
          <q-td :props="props">
            {{ props.row.nomeGuerra || '—' }}
          </q-td>
        </template>

        <template #body-cell-cpf="props">
          <q-td :props="props">{{ formatarCpf(props.row.cpf) }}</q-td>
        </template>

        <template #body-cell-om="props">
          <q-td :props="props">{{ props.row.omNome }}</q-td>
        </template>

        <template #body-cell-papeis="props">
          <q-td :props="props">
            <div class="row justify-center q-gutter-xs">
              <q-chip dense square size="sm" color="blue-2" text-color="secondary">Redator</q-chip>
              <q-chip
                v-for="p in props.row.papeis"
                :key="p"
                dense square size="sm"
                :color="PAPEL_CHIP[p]?.color ?? 'grey-3'"
                :text-color="PAPEL_CHIP[p]?.textColor ?? 'grey-9'"
              >
                {{ PAPEL_CHIP[p]?.label ?? p }}
              </q-chip>
            </div>
          </q-td>
        </template>

        <template #body-cell-ativo="props">
          <q-td :props="props">
            <q-chip dense square size="sm" :color="props.row.ativo ? 'green-2' : 'grey-3'" :text-color="props.row.ativo ? 'green-10' : 'grey-8'">
              {{ props.row.ativo ? 'Ativo' : 'Inativo' }}
            </q-chip>
          </q-td>
        </template>

        <template #body-cell-actions="props">
          <q-td :props="props" class="text-center">
            <div class="row justify-center no-wrap" style="gap:4px">
              <q-btn icon="mdi-pencil-outline" size="sm" flat round dense color="primary" @click="abrirEdicao(props.row)">
                <q-tooltip anchor="top middle" self="bottom middle">Editar</q-tooltip>
              </q-btn>
              <q-btn icon="mdi-key-outline" size="sm" flat round dense color="primary" @click="abrirRedefinirSenha(props.row)">
                <q-tooltip anchor="top middle" self="bottom middle">Redefinir senha</q-tooltip>
              </q-btn>
            </div>
          </q-td>
        </template>

        <template #no-data>
          <div class="full-width column items-center q-py-xl text-grey-7">
            <q-icon size="56px" class="q-mb-sm" name="mdi-account-search-outline" />
            <p>Nenhum usuário cadastrado.</p>
          </div>
        </template>
      </q-table>
    </q-card>

    <!-- Criar / Editar -->
    <q-dialog v-model="dialogForm" persistent>
      <q-card style="min-width:520px;max-width:520px">
        <q-card-section class="row items-center q-pa-lg q-pb-sm">
          <q-icon :name="editando ? 'mdi-account-edit-outline' : 'mdi-account-plus-outline'" color="primary" size="24px" class="q-mr-sm" />
          <span class="text-h6 text-weight-bold">{{ editando ? 'Editar Usuário' : 'Novo Usuário' }}</span>
          <q-space />
          <q-btn icon="mdi-close" size="sm" flat round dense :disable="salvando" @click="dialogForm = false" />
        </q-card-section>

        <q-separator />

        <q-card-section class="q-pa-lg">
          <q-form ref="formRef" class="column q-gutter-md" @submit.prevent="salvar">
            <div class="row q-col-gutter-sm">
              <q-select
                class="col-5"
                v-model="form.postoGraduacaoId"
                :options="postoOptions"
                option-label="label"
                option-value="value"
                emit-value
                map-options
                clearable
                label="Posto/Grad."
                outlined dense
                :disable="salvando"
              />
              <q-input
                class="col-7"
                v-model="form.nome"
                label="Nome completo *"
                outlined dense
                :rules="[obrigatorio]"
                :disable="salvando"
              />
            </div>

            <q-input
              v-model="form.nomeGuerra"
              label="Nome de guerra"
              outlined dense
              :disable="salvando"
            />

            <q-input
              :model-value="form.cpf"
              @update:model-value="val => form.cpf = mascaraCpf(val)"
              label="CPF *"
              outlined dense
              maxlength="14"
              :disable="salvando || editando"
              :rules="[cpfValidoRule]"
            />

            <q-input
              v-model="form.email"
              label="E-mail"
              type="email"
              outlined dense
              :rules="[emailValidoRule]"
              :disable="salvando"
            />

            <q-input
              v-if="!editando"
              v-model="form.senha"
              label="Senha *"
              outlined dense
              :type="mostrarSenha ? 'text' : 'password'"
              :rules="[obrigatorio, senhaMinLen]"
              :disable="salvando"
            >
              <template #append>
                <q-icon
                  :name="mostrarSenha ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                  class="cursor-pointer"
                  @click="mostrarSenha = !mostrarSenha"
                />
              </template>
            </q-input>

            <q-select
              v-model="form.omId"
              :options="omOptions"
              option-label="label"
              option-value="value"
              emit-value
              map-options
              label="Organização Militar *"
              outlined dense
              :rules="[obrigatorio]"
              :disable="salvando"
            />

            <div>
              <div class="text-caption text-grey-7 q-mb-xs">Papéis adicionais (todo usuário já pode criar/editar seus documentos)</div>
              <div class="row q-gutter-md">
                <q-checkbox v-model="form.aprovador" label="Aprovador" :disable="salvando" />
                <q-checkbox v-model="form.admin" label="Administrador" :disable="salvando" />
                <q-checkbox v-model="form.auditor" label="Auditor" :disable="salvando" />
              </div>
            </div>

            <q-toggle
              v-if="editando"
              v-model="form.ativo"
              label="Usuário ativo"
              color="primary"
              :disable="salvando"
            />
          </q-form>
        </q-card-section>

        <q-separator />

        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat :disable="salvando" @click="dialogForm = false">Cancelar</q-btn>
          <q-btn color="primary" unelevated :loading="salvando" @click="salvar">
            <q-icon left name="mdi-check" />
            {{ editando ? 'Salvar' : 'Criar Usuário' }}
          </q-btn>
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Redefinir senha -->
    <q-dialog v-model="dialogSenha" persistent>
      <q-card style="min-width:420px">
        <q-card-section class="row items-center q-pa-lg q-pb-sm">
          <q-icon name="mdi-key-outline" color="primary" size="24px" class="q-mr-sm" />
          <span class="text-h6 text-weight-bold">Redefinir Senha</span>
          <q-space />
          <q-btn icon="mdi-close" size="sm" flat round dense :disable="redefinindo" @click="dialogSenha = false" />
        </q-card-section>
        <q-separator />
        <q-card-section class="q-pa-lg">
          <p class="text-body2 text-grey-7 q-mb-md">
            Nova senha para <strong>{{ alvoSenha?.nome }}</strong>.
          </p>
          <q-form ref="senhaFormRef" @submit.prevent="confirmarRedefinirSenha">
            <q-input
              v-model="novaSenha"
              label="Nova senha *"
              outlined dense
              :type="mostrarNovaSenha ? 'text' : 'password'"
              :rules="[obrigatorio, senhaMinLen]"
              :disable="redefinindo"
            >
              <template #append>
                <q-icon
                  :name="mostrarNovaSenha ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                  class="cursor-pointer"
                  @click="mostrarNovaSenha = !mostrarNovaSenha"
                />
              </template>
            </q-input>
          </q-form>
        </q-card-section>
        <q-separator />
        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat :disable="redefinindo" @click="dialogSenha = false">Cancelar</q-btn>
          <q-btn color="primary" unelevated :loading="redefinindo" @click="confirmarRedefinirSenha">Confirmar</q-btn>
        </q-card-actions>
      </q-card>
    </q-dialog>

  </q-page>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import * as usuariosApi from '@/api/usuarios.js'
import { validarCpf, mascaraCpf, formatarCpf, onlyDigits } from '@/utils/cpf.js'
import { useAuthStore } from '@/stores/auth.js'

const $q = useQuasar()
const auth = useAuthStore()

const usuarios  = ref([])
const oms       = ref([])
const postos    = ref([])
const carregando = ref(false)

const columns = [
  { name: 'nome',   label: 'Nome Completo', field: 'nome', align: 'left', sortable: true },
  { name: 'posto',  label: 'Posto/Grad.', field: 'postoGraduacaoBigrama', align: 'center', sortable: true, style: 'width:100px' },
  { name: 'nomeGuerra', label: 'Nome de Guerra', field: 'nomeGuerra', align: 'left', sortable: true },
  { name: 'cpf',    label: 'CPF',     field: 'cpf',    align: 'center', sortable: true, style: 'width:160px' },
  { name: 'om',     label: 'OM',      field: 'omNome', align: 'center', sortable: true },
  { name: 'papeis', label: 'Papéis',  field: 'papeis', align: 'center' },
  { name: 'ativo',  label: 'Situação', field: 'ativo',  align: 'center', sortable: true, style: 'width:110px' },
  { name: 'actions', label: 'Ações',  field: 'actions', align: 'center', style: 'width:110px' },
]

const omOptions = computed(() => oms.value.map(om => ({ label: `${om.nome} (${om.sigla})`, value: om.id })))
// postos já vem ordenado pela hierarquia militar (ver PostoGraduacaoRepository), não
// alfabeticamente -- mantém essa mesma ordem no seletor.
const postoOptions = computed(() => postos.value.map(p => ({ label: `${p.bigrama} — ${p.nome}`, value: p.id })))

const PAPEL_CHIP = {
  ADMIN:     { label: 'Admin',     color: 'deep-orange-2', textColor: 'deep-orange-10' },
  APROVADOR: { label: 'Aprovador', color: 'teal-2',        textColor: 'teal-10' },
  AUDITOR:   { label: 'Auditor',   color: 'indigo-2',      textColor: 'indigo-10' },
}

async function carregar() {
  carregando.value = true
  try {
    const [listaUsuarios, listaOms, listaPostos] = await Promise.all([
      usuariosApi.listUsuarios(),
      usuariosApi.listOrganizacoesMilitares(),
      usuariosApi.listPostosGraduacoes(),
    ])
    usuarios.value = listaUsuarios
    oms.value = listaOms
    postos.value = listaPostos
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar usuários: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)

const obrigatorio = (v) => (v != null && String(v).trim() !== '') || 'Campo obrigatório'
const senhaMinLen = (v) => (String(v ?? '').length >= 8) || 'Mínimo de 8 caracteres'
const cpfValidoRule = (v) => validarCpf(v) || 'CPF inválido'
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const emailValidoRule = (v) => !v || EMAIL_RE.test(v) || 'E-mail inválido'

// ── Criar / Editar ───────────────────────────────────────────────────────

const dialogForm = ref(false)
const formRef     = ref(null)
const salvando    = ref(false)
const editando    = ref(null) // Usuario sendo editado, ou null (criação)
const mostrarSenha = ref(false)

const form = reactive({
  nome: '', nomeGuerra: '', cpf: '', email: '', postoGraduacaoId: null, senha: '',
  omId: null, aprovador: false, admin: false, auditor: false, ativo: true,
})

function resetForm() {
  Object.assign(form, {
    nome: '', nomeGuerra: '', cpf: '', email: '', postoGraduacaoId: null, senha: '',
    omId: null, aprovador: false, admin: false, auditor: false, ativo: true,
  })
  formRef.value?.resetValidation()
}

function abrirCriacao() {
  editando.value = null
  resetForm()
  dialogForm.value = true
}

function abrirEdicao(usuario) {
  editando.value = usuario
  Object.assign(form, {
    nome: usuario.nome,
    nomeGuerra: usuario.nomeGuerra ?? '',
    cpf: formatarCpf(usuario.cpf),
    email: usuario.email ?? '',
    postoGraduacaoId: usuario.postoGraduacaoId ?? null,
    senha: '',
    omId: usuario.omId,
    aprovador: usuario.papeis.includes('APROVADOR'),
    admin: usuario.papeis.includes('ADMIN'),
    auditor: usuario.papeis.includes('AUDITOR'),
    ativo: usuario.ativo,
  })
  formRef.value?.resetValidation()
  dialogForm.value = true
}

function papeisSelecionados() {
  const papeis = []
  if (form.aprovador) papeis.push('APROVADOR')
  if (form.admin) papeis.push('ADMIN')
  if (form.auditor) papeis.push('AUDITOR')
  return papeis
}

async function salvar() {
  const valid = await formRef.value.validate()
  if (!valid) return

  salvando.value = true
  try {
    if (editando.value) {
      const usuarioAtualizado = await usuariosApi.updateUsuario(editando.value.id, {
        nome: form.nome,
        nomeGuerra: form.nomeGuerra,
        email: form.email,
        postoGraduacaoId: form.postoGraduacaoId,
        omId: form.omId,
        ativo: form.ativo,
        papeis: papeisSelecionados(),
      })
      // Se o admin editou o próprio usuário, atualiza o topbar na hora --
      // sem isso, o nome/posto exibido ficaria desatualizado até um novo login.
      if (auth.usuario && editando.value.id === auth.usuario.id) {
        auth.atualizarPerfil(usuarioAtualizado)
      }
      $q.notify({ type: 'positive', message: 'Usuário atualizado.' })
    } else {
      await usuariosApi.createUsuario({
        nome: form.nome,
        nomeGuerra: form.nomeGuerra,
        cpf: onlyDigits(form.cpf),
        email: form.email,
        postoGraduacaoId: form.postoGraduacaoId,
        senha: form.senha,
        omId: form.omId,
        papeis: papeisSelecionados(),
      })
      $q.notify({ type: 'positive', message: 'Usuário criado.' })
    }
    dialogForm.value = false
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao salvar usuário: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    salvando.value = false
  }
}

// ── Redefinir senha ──────────────────────────────────────────────────────

const dialogSenha   = ref(false)
const senhaFormRef  = ref(null)
const redefinindo   = ref(false)
const alvoSenha      = ref(null)
const novaSenha       = ref('')
const mostrarNovaSenha = ref(false)

function abrirRedefinirSenha(usuario) {
  alvoSenha.value = usuario
  novaSenha.value = ''
  senhaFormRef.value?.resetValidation()
  dialogSenha.value = true
}

async function confirmarRedefinirSenha() {
  const valid = await senhaFormRef.value.validate()
  if (!valid) return

  redefinindo.value = true
  try {
    await usuariosApi.redefinirSenha(alvoSenha.value.id, novaSenha.value)
    $q.notify({ type: 'positive', message: 'Senha redefinida.' })
    dialogSenha.value = false
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao redefinir senha: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    redefinindo.value = false
  }
}
</script>
