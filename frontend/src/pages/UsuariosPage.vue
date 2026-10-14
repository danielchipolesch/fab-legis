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

    <!-- Revisão periódica de acessos (docs/autenticacao.md): automática -- o sistema aponta quem precisa de revisão; o admin só
         confere (Manter acesso / Retirar perfis…). Ninguém precisa pedir nada e nada é retirado sozinho: por ora só avisa. -->
    <q-banner v-if="pendentes" rounded class="bg-amber-1 text-amber-10 q-mb-md" data-testid="aviso-de-pendencias">
      <template #avatar><q-icon name="mdi-account-alert-outline" /></template>
      {{ pendentes }} {{ pendentes === 1 ? 'acesso precisa' : 'acessos precisam' }} de revisão.
      <template #action>
        <q-toggle v-model="soPendentes" label="Mostrar só esses" color="amber-9" data-testid="so-pendentes" />
      </template>
    </q-banner>

    <q-card flat bordered>
      <q-table
        :rows="usuariosVisiveis"
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
            {{ props.row.nomeGuerra ? caixaAlta(props.row.nomeGuerra) : '—' }}
          </q-td>
        </template>

        <template #body-cell-cpf="props">
          <q-td :props="props">{{ ocultarCpf(props.row.cpf) }}</q-td>
        </template>

        <template #body-cell-om="props">
          <q-td :props="props">{{ props.row.omNome }}</q-td>
        </template>

        <template #body-cell-papeis="props">
          <q-td :props="props">
            <div class="row justify-center q-gutter-xs">
              <q-chip
                v-for="p in props.row.papeis"
                :key="p"
                dense square size="sm"
                :color="PAPEIS[p]?.cor ?? 'grey-3'"
                :text-color="PAPEIS[p]?.corDoTexto ?? 'grey-9'"
              >
                {{ nomeDoPapel(p) }}
              </q-chip>
            </div>
          </q-td>
        </template>

        <template #body-cell-acesso="props">
          <q-td :props="props">
            <q-chip dense square size="sm" :color="motivoDaRevisao(props.row.motivoDaRevisao).color"
                    :text-color="motivoDaRevisao(props.row.motivoDaRevisao).textColor">
              {{ motivoDaRevisao(props.row.motivoDaRevisao).label }}
              <q-tooltip v-if="motivoDaRevisao(props.row.motivoDaRevisao).explicacao">
                {{ motivoDaRevisao(props.row.motivoDaRevisao).explicacao }}
              </q-tooltip>
            </q-chip>
            <div v-if="temPapelElevado(props.row)" class="text-caption text-grey-7">
              Revisado em {{ formatarData(props.row.dtRevisaoAcesso) }}
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
              <q-btn v-if="props.row.revisavel" icon="mdi-check" size="sm" flat round dense color="positive"
                     :loading="revisando === props.row.id" :data-testid="`manter-acesso-${props.row.cpf}`" @click="manterAcesso(props.row)">
                <q-tooltip anchor="top middle" self="bottom middle">Manter acesso (reconfirma os perfis)</q-tooltip>
              </q-btn>
              <q-btn v-if="props.row.revisavel" icon="mdi-account-minus-outline" size="sm" flat round dense color="negative"
                     :disable="revisando === props.row.id" :data-testid="`retirar-papeis-${props.row.cpf}`" @click="abrirRetirada(props.row)">
                <q-tooltip anchor="top middle" self="bottom middle">Retirar perfis…</q-tooltip>
              </q-btn>
              <q-btn v-if="podeAdministrarEste(props.row)" icon="mdi-pencil-outline" size="sm" flat round dense color="primary" @click="abrirEdicao(props.row)">
                <q-tooltip anchor="top middle" self="bottom middle">Editar</q-tooltip>
              </q-btn>
              <q-btn v-if="podeAdministrarEste(props.row)" icon="mdi-key-outline" size="sm" flat round dense color="primary" @click="abrirRedefinirSenha(props.row)">
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
              :rules="editando ? [] : [cpfValidoRule]"
            />

            <q-input
              v-model="form.email"
              label="E-mail"
              type="email"
              outlined dense
              :rules="[emailValidoRule, emailInstitucionalRule]"
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
              :options="omOptionsFiltradas"
              option-label="label"
              option-value="value"
              emit-value
              map-options
              use-input
              input-debounce="0"
              @filter="filtrarOm"
              label="Organização Militar *"
              outlined dense
              :rules="[obrigatorio]"
              :disable="salvando"
            />

            <div>
              <div class="text-caption text-grey-7 q-mb-xs">Perfis (Elaborador é padrão de todo usuário e não pode ser removido; os demais são adicionais)</div>
              <div class="row q-gutter-md">
                <q-checkbox v-model="form.editor" label="Elaborador" disable>
                  <q-tooltip>Todo usuário tem este perfil -- não dá pra remover.</q-tooltip>
                </q-checkbox>
                <q-checkbox v-model="form.aprovador" label="Aprovador da OM" :disable="salvando || !concediveis.aprovador" />
                <q-checkbox v-model="form.publicador" label="Publicador do BCA" :disable="salvando || !concediveis.publicador">
                  <q-tooltip>A publicação é exclusiva do CENDOC: só quem é lotado no CENDOC (e só o Administrador Geral) recebe este perfil.</q-tooltip>
                </q-checkbox>
                <q-checkbox v-model="form.admin" :label="nomeDoPapel(papelDeAdmin(siglaDaOmDeDestino))" :disable="salvando || !concediveis.admin">
                  <q-tooltip>Só o Administrador Geral (CENDOC) concede o perfil de administrador.</q-tooltip>
                </q-checkbox>
                <q-checkbox v-model="form.auditor" :label="nomeDoPapel(papelDeAuditor(siglaDaOmDeDestino))" :disable="salvando || !concediveis.auditor" />
                <q-checkbox v-model="form.painel" :label="nomeDoPapel(papelDoPainelGerencial(siglaDaOmDeDestino))" :disable="salvando || !concediveis.painel">
                  <q-tooltip>Acesso ao Painel Gerencial: o Painel Gerencial Geral (CENDOC) vê todas as OMs; o Painel Gerencial da OM, só a própria.</q-tooltip>
                </q-checkbox>
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

    <!-- Retirar perfis (revisão de acesso): desmarque o que a pessoa deixa de ter; o que continuar marcado é mantido. -->
    <q-dialog v-model="retirada.aberta" persistent>
      <q-card style="min-width:400px">
        <q-card-section class="row items-center q-pb-none">
          <q-icon name="mdi-account-minus-outline" color="negative" size="24px" class="q-mr-sm" />
          <span class="text-h6">Retirar perfis</span>
        </q-card-section>
        <q-card-section v-if="retirada.usuario" class="q-pt-md">
          <div class="text-weight-medium q-mb-sm">{{ caixaAlta(retirada.usuario.nome) }}</div>
          <div class="text-caption text-grey-7 q-mb-xs">Desmarque o que a pessoa deixa de ter:</div>
          <div class="column">
            <q-checkbox v-for="p in papeisElevadosDe(retirada.usuario)" :key="p" v-model="retirada.mantidos[p]" :label="rotuloDoPapel(p)" />
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="grey-7" v-close-popup />
          <q-btn unelevated label="Confirmar" color="primary" data-testid="confirmar-retirada" @click="confirmarRetirada" />
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
            Nova senha para <strong>{{ caixaAlta(alvoSenha?.nome) }}</strong>.
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
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import * as usuariosApi from '@/api/usuarios.js'
import { validarCpf, mascaraCpf, ocultarCpf, onlyDigits } from '@/utils/cpf.js'
import { caixaAlta, normalizarBusca } from '@/utils/texto.js'
import { useAuthStore } from '@/stores/auth.js'
import { emailInstitucional } from '@/utils/email.js'
import { omsDoEscopo, papeisConcediveis, podeAdministrar } from '@/utils/escopo.js'
import { PAPEIS, nomeDoPapel, ehAdmin, ehAuditor, ehPainelGerencial, papelDeAdmin, papelDeAuditor, papelDoPainelGerencial } from '@/utils/papeis.js'
import { motivoDaRevisao, rotuloDoPapel, contarPendentes, precisaDeRevisao, temPapelElevado, papeisElevadosDe } from '@/utils/revisaoDeAcessos.js'

const $q = useQuasar()
const auth = useAuthStore()

const usuarios  = ref([])
const oms       = ref([])
const postos    = ref([])
const carregando = ref(false)

const columns = [
  { name: 'nome',   label: 'Nome Completo', field: 'nome', align: 'left', sortable: true, format: val => caixaAlta(val) },
  { name: 'posto',  label: 'Posto/Grad.', field: 'postoGraduacaoBigrama', align: 'center', sortable: true, style: 'width:100px' },
  { name: 'nomeGuerra', label: 'Nome de Guerra', field: 'nomeGuerra', align: 'left', sortable: true },
  { name: 'cpf',    label: 'CPF',     field: 'cpf',    align: 'center', sortable: true, style: 'width:160px' },
  { name: 'om',     label: 'OM',      field: 'omNome', align: 'center', sortable: true },
  { name: 'papeis', label: 'Perfis',  field: 'papeis', align: 'center' },
  { name: 'acesso', label: 'Acesso',  field: 'motivoDaRevisao', align: 'center', style: 'width:150px' },
  { name: 'ativo',  label: 'Situação', field: 'ativo',  align: 'center', sortable: true, style: 'width:110px' },
  { name: 'actions', label: 'Ações',  field: 'actions', align: 'center', style: 'width:110px' },
]

// Catálogo real de OMs da FAB passou de 1 (seed antigo) pra 300+ (ver
// V1__initial.sql) -- uma lista desse tamanho sem busca é impraticável de rolar.
const omOptions = computed(() => omsDoEscopo(oms.value, auth.usuario?.omSigla).map(om => ({
  label: `${om.nome} (${om.sigla})`, value: om.id, busca: normalizarBusca(`${om.nome} ${om.sigla}`),
})))
const omOptionsFiltradas = ref([])
watch(omOptions, (val) => { omOptionsFiltradas.value = val }, { immediate: true })

function filtrarOm(val, update) {
  update(() => {
    const termo = normalizarBusca(val)
    omOptionsFiltradas.value = termo
      ? omOptions.value.filter(o => o.busca.includes(termo))
      : omOptions.value
  })
}
// postos já vem ordenado pela hierarquia militar (ver PostoGraduacaoRepository), não
// alfabeticamente -- mantém essa mesma ordem no seletor.
const postoOptions = computed(() => postos.value.map(p => ({ label: `${p.bigrama} — ${p.nome}`, value: p.id })))

// ── Revisão periódica de acessos ─────────────────────────────────────────────
// Automática: o servidor diz, para cada usuário, se o acesso precisa de revisão (motivoDaRevisao) e se QUEM está logado é quem o
// revisa (revisavel). O admin só confere: Manter acesso (reconfirma) ou Retirar perfis…; nada é retirado sozinho, por ora só avisa.
const soPendentes = ref(false)
const revisando = ref(null)
const retirada = reactive({ aberta: false, usuario: null, mantidos: {} })

const pendentes = computed(() => contarPendentes(usuarios.value))
const usuariosVisiveis = computed(() => (soPendentes.value ? usuarios.value.filter(precisaDeRevisao) : usuarios.value))

function formatarData(iso) {
  if (!iso) return '—'
  const [y, m, d] = String(iso).split('T')[0].split('-')
  return `${d}/${m}/${y}`
}

async function enviarRevisao(usuario, papeisMantidos, mensagem) {
  revisando.value = usuario.id
  try {
    const atualizado = await usuariosApi.revisarAcesso(usuario.id, ['ELB', ...papeisMantidos])
    const i = usuarios.value.findIndex(u => u.id === usuario.id)
    if (i >= 0) usuarios.value.splice(i, 1, atualizado)
    if (!pendentes.value) soPendentes.value = false
    $q.notify({ type: 'positive', message: mensagem })
  } catch (e) {
    $q.notify({ type: 'negative', message: `Não foi possível registrar a revisão: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    revisando.value = null
  }
}

function manterAcesso(usuario) {
  enviarRevisao(usuario, papeisElevadosDe(usuario), `Acesso de ${caixaAlta(usuario.nome)} mantido.`)
}

function abrirRetirada(usuario) {
  retirada.usuario = usuario
  retirada.mantidos = Object.fromEntries(papeisElevadosDe(usuario).map(p => [p, true]))
  retirada.aberta = true
}

function confirmarRetirada() {
  const usuario = retirada.usuario
  retirada.aberta = false
  if (!usuario) return
  enviarRevisao(usuario, papeisElevadosDe(usuario).filter(p => retirada.mantidos[p]), `Acesso de ${caixaAlta(usuario.nome)} revisado.`)
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
const emailInstitucionalRule = (v) => emailInstitucional(v) || 'Use um e-mail institucional (@fab.mil.br)'

// ── Criar / Editar ───────────────────────────────────────────────────────

const dialogForm = ref(false)
const formRef     = ref(null)
const salvando    = ref(false)
const editando    = ref(null) // Usuario sendo editado, ou null (criação)
const mostrarSenha = ref(false)

// editor: true por padrão e travado na UI (checkbox disable) -- ver
// UsuarioService.papeisComo no backend, que também força isso independente do
// que vier daqui (mesma regra em duas camadas, não só decoração de tela).
const form = reactive({
  nome: '', nomeGuerra: '', cpf: '', email: '', postoGraduacaoId: null, senha: '',
  omId: null, editor: true, aprovador: false, publicador: false, admin: false, auditor: false, painel: false, ativo: true,
})

// Escopo (utils/escopo.js): o admin local só gerencia a própria OM e não concede Administrador nem Publicador; o geral (CENDOC) gerencia
// todas. A tela esconde o que o servidor recusaria -- o servidor é quem garante.
const siglaDaOmDeDestino = computed(() => oms.value.find(o => o.id === form.omId)?.sigla)
const concediveis = computed(() => papeisConcediveis(auth.usuario?.omSigla, siglaDaOmDeDestino.value))
const podeAdministrarEste = (usuario) => podeAdministrar(auth.usuario?.omSigla, usuario)
// Trocar a OM de destino para uma que não admite o perfil desmarca o que deixou de valer (ex.: Publicador fora do CENDOC).
watch(concediveis, (c) => {
  if (!c.publicador) form.publicador = false
  if (!c.admin) form.admin = false
})

function resetForm() {
  Object.assign(form, {
    nome: '', nomeGuerra: '', cpf: '', email: '', postoGraduacaoId: null, senha: '',
    omId: null, editor: true, aprovador: false, publicador: false, admin: false, auditor: false, painel: false, ativo: true,
  })
  formRef.value?.resetValidation()
}

function abrirCriacao() {
  editando.value = null
  resetForm()
  // O admin local só tem uma OM possível: já vem escolhida.
  if (omOptions.value.length === 1) form.omId = omOptions.value[0].value
  dialogForm.value = true
}

function abrirEdicao(usuario) {
  editando.value = usuario
  Object.assign(form, {
    nome: usuario.nome,
    nomeGuerra: usuario.nomeGuerra ?? '',
    cpf: ocultarCpf(usuario.cpf),
    email: usuario.email ?? '',
    postoGraduacaoId: usuario.postoGraduacaoId ?? null,
    senha: '',
    omId: usuario.omId,
    editor: true, // sempre -- ver comentário no form acima
    aprovador: usuario.papeis.includes('APR_OM'),
    publicador: usuario.papeis.includes('PUB_BCA'),
    admin: ehAdmin(usuario.papeis),
    auditor: ehAuditor(usuario.papeis),
    painel: ehPainelGerencial(usuario.papeis),
    ativo: usuario.ativo,
  })
  formRef.value?.resetValidation()
  dialogForm.value = true
}

function papeisSelecionados() {
  const papeis = []
  if (form.editor) papeis.push('ELB')
  if (form.aprovador) papeis.push('APR_OM')
  if (form.publicador) papeis.push('PUB_BCA')
  // Administrador e Auditor existem em duas versões; vale a da lotação de destino (CENDOC = geral).
  if (form.admin) papeis.push(papelDeAdmin(siglaDaOmDeDestino.value))
  if (form.auditor) papeis.push(papelDeAuditor(siglaDaOmDeDestino.value))
  if (form.painel) papeis.push(papelDoPainelGerencial(siglaDaOmDeDestino.value))
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
