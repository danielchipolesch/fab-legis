<template>
  <q-page class="login-page column items-center justify-center">
    <q-spinner v-if="redirecionando" color="primary" size="42px" />

    <q-card v-else flat bordered style="width:380px; max-width:92vw">
      <q-card-section class="column items-center q-pt-xl q-pb-md">
        <q-avatar size="56px" color="primary" text-color="white" square style="border-radius:8px">
          <q-icon name="mdi-gavel" size="30px" />
        </q-avatar>
        <div class="text-h6 text-weight-bold text-primary q-mt-md">FAB Legis</div>
        <div class="text-caption text-grey-7">Gestão de Legislação do COMAER</div>
      </q-card-section>

      <q-separator />

      <q-card-section class="q-pa-lg">
        <!-- POST HTML tradicional (não fetch) pro /login do backend -- é assim
             que o cookie de sessão do Spring Security e o redirect de volta
             pro /oauth2/authorize (que já estava pendente, ver auth.iniciarLogin)
             funcionam. username precisa ser o CPF só com dígitos, mesmo formato
             que UsuarioDetailsService.loadUserByUsername espera. -->
        <form :action="`${OAUTH2_BASE_URL}/login`" method="POST" @submit="aoEnviar">
          <input type="hidden" name="username" :value="onlyDigits(cpf)" />
          <input type="hidden" name="password" :value="senha" />

          <div class="column" style="gap:4px">
            <q-input
              :model-value="cpf"
              @update:model-value="val => cpf = mascaraCpf(val)"
              label="CPF"
              outlined
              dense
              :rules="[v => !!onlyDigits(v) || 'Informe o CPF', v => validarCpf(v) || 'CPF inválido']"
              lazy-rules
              maxlength="14"
              autofocus
            >
              <template #prepend><q-icon name="mdi-card-account-details-outline" /></template>
            </q-input>

            <q-input
              v-model="senha"
              label="Senha"
              outlined
              dense
              :type="mostrarSenha ? 'text' : 'password'"
              :rules="[v => !!v || 'Informe a senha']"
              lazy-rules
            >
              <template #prepend><q-icon name="mdi-lock-outline" /></template>
              <template #append>
                <q-icon
                  :name="mostrarSenha ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                  class="cursor-pointer"
                  @click="mostrarSenha = !mostrarSenha"
                />
              </template>
            </q-input>

            <q-banner v-if="erro" dense class="bg-red-1 text-negative q-mt-sm" rounded>
              {{ erro }}
            </q-banner>

            <q-btn
              type="submit"
              color="primary"
              unelevated
              class="q-mt-md"
              :loading="enviando"
              label="Entrar"
            />
          </div>
        </form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore, OAUTH2_BASE_URL } from '@/stores/auth.js'
import { validarCpf, mascaraCpf, onlyDigits } from '@/utils/cpf.js'

const route = useRoute()
const auth = useAuthStore()

const cpf = ref('')
const senha = ref('')
const mostrarSenha = ref(false)
const enviando = ref(false)

// Se erro=1 veio pela URL (ver failureUrl no AuthorizationServerConfig), a
// pessoa já está no meio do fluxo (senha errada na tentativa anterior) --
// mostra o formulário direto com o aviso, sem reiniciar o /oauth2/authorize.
const erro = ref(route.query.erro ? 'CPF ou senha inválidos.' : null)

// Só mostra o formulário quando já estamos voltando de um redirect do
// Authorization Server -- "?continuar=1" é o marcador (setado pelo backend,
// AuthorizationServerConfig) que confirma isso; senão, dispara o fluxo OAuth2
// primeiro. Não dá pra usar sessionStorage.oauth2.state como esse sinal (como
// era antes): um valor de uma tentativa anterior abandonada (outra aba, outra
// origem localhost/127.0.0.1, um refresh no meio do caminho) sobrevive e
// engana a página a achar que já existe um fluxo em andamento, pulando a
// chamada a /oauth2/authorize -- o POST /login seguinte autentica numa sessão
// SEM nenhuma autorização pendente salva, e cai de volta em "/" sem emitir
// "code" nenhum (bug real, já observado em produção local).
const redirecionando = ref(!route.query.erro && route.query.continuar !== '1')

onMounted(async () => {
  if (!redirecionando.value) return
  // Falsete de segurança: se a navegação de verdade (window.location.href,
  // dentro de iniciarLogin) travar por algum motivo que não lança exceção
  // nenhuma (ex.: bloqueada silenciosamente por uma extensão do navegador),
  // esse timeout garante que o spinner não fique girando pra sempre sem
  // nenhuma explicação.
  const timeout = setTimeout(() => {
    erro.value = 'O login está demorando mais que o esperado. Verifique sua conexão e tente novamente.'
    redirecionando.value = false
  }, 8000)
  try {
    await auth.iniciarLogin(route.query.redirect ?? '/')
    // iniciarLogin normalmente NUNCA resolve de fato -- ela navega a página
    // inteira pra fora daqui (window.location.href) antes de retornar. Só
    // chega neste ponto se, por algum motivo, a navegação não aconteceu.
  } catch {
    // Sem isso, qualquer falha aqui (ex.: crypto.subtle indisponível no
    // navegador, backend fora do ar) deixava o spinner girando pra sempre,
    // sem nenhum sinal do que deu errado -- exatamente o tipo de falha
    // silenciosa que não pode acontecer numa tela de login.
    clearTimeout(timeout)
    erro.value = 'Não foi possível iniciar o login. Verifique sua conexão e tente novamente.'
    redirecionando.value = false
  }
})

function aoEnviar(event) {
  const form = event.target
  const [cpfVisivel, senhaVisivel] = form.querySelectorAll('input:not([type=hidden])')

  // Alguns navegadores preenchem os campos via autofill/gerenciador de senha
  // sem disparar os eventos que o v-model do Vue escuta -- o campo aparece
  // preenchido na tela, mas cpf.value/senha.value continuam vazios no estado
  // reativo. Isso fazia a validação abaixo barrar a submissão em silêncio
  // (preventDefault, sem nenhum erro visível) -- clicar "Entrar" parecia não
  // fazer nada. Lê o valor de verdade do DOM como reforço.
  const cpfDigits = onlyDigits(cpfVisivel?.value || cpf.value)
  const senhaValor = senhaVisivel?.value || senha.value

  if (!validarCpf(cpfDigits) || !senhaValor) {
    event.preventDefault()
    erro.value = !validarCpf(cpfDigits) ? 'CPF inválido.' : 'Informe a senha.'
    return
  }

  // Escreve direto nos campos ocultos que realmente são submetidos -- não só
  // via cpf.value/senha.value (a reatividade do Vue só atualiza o DOM no
  // próximo tick, tarde demais: o navegador já estaria submetendo o form com
  // o valor hidden antigo). Isso garante que, mesmo se cpf.value/senha.value
  // estavam desincronizados por causa do autofill, o valor certo (lido do
  // campo visível agora mesmo) é o que efetivamente viaja no POST.
  const hiddenUsername = form.querySelector('input[name=username]')
  const hiddenPassword = form.querySelector('input[name=password]')
  hiddenUsername.value = cpfDigits
  hiddenPassword.value = senhaValor

  enviando.value = true
  // Sem preventDefault daqui em diante -- o <form> segue com o POST normal
  // do navegador, é exatamente isso que precisamos (navegação de verdade,
  // não fetch, pro cookie de sessão do Spring Security funcionar).
}
</script>

<style scoped>
.login-page {
  background: var(--color-background, #F4F6FA);
  min-height: 100vh;
}
</style>
