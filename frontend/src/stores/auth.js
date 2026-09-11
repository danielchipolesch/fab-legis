import { defineStore } from 'pinia'
import { setAuthTokenGetter, setUnauthorizedHandler, setRefreshHandler } from '@/api/client.js'
import { gerarCodeVerifier, gerarCodeChallenge, gerarState } from '@/utils/pkce.js'

const STORAGE_KEY = 'fab-legis.auth'
const CLIENT_ID = 'fab-legis-frontend'
// Endpoints do Authorization Server (/oauth2/**, /login, /logout) são
// proxiados pelo PRÓPRIO Vite dev server (ver vite.config.js) pra ficarem na
// MESMA origem do frontend -- string vazia = caminho relativo = sempre a
// origem atual, nunca uma origem cross-site diferente. Isso não é só
// estética: o cookie de sessão do Spring Security (SameSite=Lax) só é
// confiável em requisições same-origin; cross-origin (mesmo POST de uma
// navegação de página inteira, não fetch) o navegador descarta o cookie na
// maioria das vezes, e o /login "funcionava" só que numa sessão sem a
// authorization request salva -- voltava pro /login sem erro nenhum.
// Exportado porque LoginPage.vue também precisa (submete o formulário direto
// pro /login).
export const OAUTH2_BASE_URL = import.meta.env.VITE_OAUTH2_BASE_URL ?? ''
const REDIRECT_URI = import.meta.env.VITE_OAUTH2_REDIRECT_URI ?? `${window.location.origin}/callback`

// Monta a URL de /oauth2/authorize com um par verifier/challenge (PKCE) e
// state novos -- usado tanto pelo login visível (iniciarLogin) quanto pela
// renovação silenciosa (refresh). NÃO grava nada em sessionStorage aqui --
// cada chamador decide onde guardar verifier/state (iniciarLogin usa
// sessionStorage de verdade, já que a página vai navegar pra longe e
// precisar deles de volta; refresh usa só variáveis em memória, pro iframe).
async function montarAuthorizeUrl() {
  const verifier = gerarCodeVerifier()
  const challenge = await gerarCodeChallenge(verifier)
  const state = gerarState()

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: 'openid',
    code_challenge: challenge,
    code_challenge_method: 'S256',
    state,
  })
  return { url: `${OAUTH2_BASE_URL}/oauth2/authorize?${params.toString()}`, verifier, state }
}

function lerArmazenado() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

// Decodifica só o payload do JWT (base64url) pra ler as claims -- não precisa
// verificar assinatura no client, a confiança vem do próprio endpoint via
// TLS (mesma fonte que emitiu o token que estamos decodificando).
function decodificarClaims(jwt) {
  const payload = jwt.split('.')[1]
  const normalizado = payload.replace(/-/g, '+').replace(/_/g, '/')
  const json = decodeURIComponent(
    atob(normalizado)
      .split('')
      .map(c => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
      .join('')
  )
  return JSON.parse(json)
}

export const useAuthStore = defineStore('auth', {
  state: () => {
    const salvo = lerArmazenado()
    return {
      token: salvo?.token ?? null,
      usuario: salvo ? {
        id: salvo.usuarioId,
        nome: salvo.nome,
        cpf: salvo.cpf,
        omId: salvo.omId,
        omNome: salvo.omNome,
        nomeGuerra: salvo.nomeGuerra,
        postoGraduacaoBigrama: salvo.postoGraduacaoBigrama,
        papeis: salvo.papeis ?? [],
      } : null,
      loading: false,
      erro: null,
    }
  },

  getters: {
    isAuthenticated: (state) => !!state.token,
    // ADMIN é puramente administrativo (usuários/OMs) -- não implica EDIT/APROV/PUBLIC
    // nenhum, ver PapelEnum no backend.
    isAdmin: (state) => state.usuario?.papeis?.includes('ADMIN') ?? false,
    isEditor: (state) => state.usuario?.papeis?.includes('EDIT') ?? false,
    isAprovador: (state) => state.usuario?.papeis?.includes('APROV') ?? false,
    isPublicador: (state) => state.usuario?.papeis?.includes('PUBLIC') ?? false,
    // Espelha o backend (hasRole('AUDITOR') -- ver AuditoriaController): papel
    // independente de ADMIN, precisa estar marcado no próprio cadastro.
    isAuditor: (state) => state.usuario?.papeis?.includes('AUDITOR') ?? false,
  },

  actions: {
    // Chamado uma vez no boot do app (main.js) -- plugueia o client HTTP no
    // token/estado deste store sem criar dependência circular entre módulos.
    inicializar() {
      setAuthTokenGetter(() => this.token)
      setUnauthorizedHandler(() => this.logout())
      setRefreshHandler(() => this.refresh())
    },

    _aplicarTokens(tokenResp) {
      const claims = decodificarClaims(tokenResp.access_token)
      this.token = tokenResp.access_token
      this.usuario = {
        id: Number(claims.sub),
        nome: claims.nome,
        cpf: claims.preferred_username,
        omId: claims.om_id != null ? Number(claims.om_id) : null,
        // omNome/nomeGuerra/postoGraduacaoBigrama não vão na claim (mesmo
        // conjunto que o token caseiro já carregava) -- quem precisa desses
        // três em telas específicas já busca via GET /usuarios/{id} quando
        // necessário; o topbar usa nome/cpf, que o token sempre traz.
        omNome: this.usuario?.omId === claims.om_id ? this.usuario?.omNome : null,
        nomeGuerra: this.usuario?.nomeGuerra ?? null,
        postoGraduacaoBigrama: this.usuario?.postoGraduacaoBigrama ?? null,
        papeis: claims.realm_access?.roles ?? [],
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        token: this.token,
        usuarioId: this.usuario.id,
        nome: this.usuario.nome,
        cpf: this.usuario.cpf,
        omId: this.usuario.omId,
        omNome: this.usuario.omNome,
        nomeGuerra: this.usuario.nomeGuerra,
        postoGraduacaoBigrama: this.usuario.postoGraduacaoBigrama,
        papeis: this.usuario.papeis,
      }))
    },

    // Início do fluxo Authorization Code + PKCE: gera verifier/state, guarda
    // em sessionStorage (só dura a aba/sessão, suficiente pro handshake) e
    // navega de VERDADE (não fetch) pro /oauth2/authorize -- é uma troca de
    // página, não uma chamada de API. Chamado pela LoginPage.vue quando ela
    // percebe que ainda não está no meio de um fluxo em andamento.
    async iniciarLogin(redirect) {
      const { url, verifier, state } = await montarAuthorizeUrl()
      sessionStorage.setItem('oauth2.code_verifier', verifier)
      sessionStorage.setItem('oauth2.state', state)
      sessionStorage.setItem('oauth2.redirect', redirect ?? '/')
      window.location.href = url
    },

    // Chamado pela OAuthCallbackPage.vue com o code/state da query string.
    // Troca o code por um access token via POST /oauth2/token (essa parte já
    // é uma chamada de API normal, não navegação).
    async trocarCodePorToken(code, state) {
      await this._trocarCodeInterno(code, state, sessionStorage)
      const redirect = sessionStorage.getItem('oauth2.redirect') ?? '/'
      sessionStorage.removeItem('oauth2.redirect')
      return redirect
    },

    async _trocarCodeInterno(code, state, storage) {
      const stateEsperado = storage.getItem('oauth2.state')
      const verifier = storage.getItem('oauth2.code_verifier')
      storage.removeItem('oauth2.code_verifier')
      storage.removeItem('oauth2.state')
      if (!stateEsperado || state !== stateEsperado || !verifier) {
        throw new Error('Fluxo de login inválido ou expirado. Tente entrar novamente.')
      }

      const body = new URLSearchParams({
        grant_type: 'authorization_code',
        code,
        redirect_uri: REDIRECT_URI,
        client_id: CLIENT_ID,
        code_verifier: verifier,
      })
      const res = await fetch(`${OAUTH2_BASE_URL}/oauth2/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body.toString(),
      })
      if (!res.ok) throw new Error('Não foi possível concluir o login.')
      this._aplicarTokens(await res.json())
    },

    // Renovação silenciosa: o Authorization Server nunca emite refresh token
    // pra um client público (ver RegisteredClient em AuthorizationServerConfig
    // -- decisão de segurança do próprio Spring, não uma escolha nossa), então
    // renovar o access token é refazer o handshake Authorization Code + PKCE
    // -- mas sem mostrar nada pro usuário: um iframe oculto navega pro
    // /oauth2/authorize, reaproveitando o cookie de sessão já existente (se
    // ainda válido, o backend emite o code direto, sem pedir login de novo) e
    // acaba na mesma origem (/callback), de onde dá pra ler a URL final. Se a
    // sessão já caiu (cookie expirado/logout), o backend redireciona pro
    // /login em vez do /callback -- detectamos isso pelo path final e
    // desistimos (dispara o logout normal do client.js).
    async refresh() {
      try {
        const { url, verifier, state } = await montarAuthorizeUrl()
        const iframeStorage = { _v: verifier, _s: state,
          getItem(k) { return k === 'oauth2.state' ? this._s : k === 'oauth2.code_verifier' ? this._v : null },
          removeItem() {} }

        const destino = await new Promise((resolve, reject) => {
          const iframe = document.createElement('iframe')
          iframe.style.display = 'none'
          const timeout = setTimeout(() => { limpar(); reject(new Error('timeout')) }, 8000)
          function limpar() { clearTimeout(timeout); iframe.remove() }
          // O handshake passa por vários hops de origem cruzada (backend) antes
          // de voltar pra nossa origem (/callback ou /login) -- onload dispara a
          // cada hop, mas só conseguimos LER a URL quando já está de volta na
          // mesma origem (SecurityError nos hops intermediários, que só
          // significam "ainda não chegou", não falha).
          iframe.onload = () => {
            let href
            try {
              href = iframe.contentWindow.location.href
            } catch {
              return
            }
            if (href === 'about:blank') return
            limpar()
            resolve(new URL(href))
          }
          document.body.appendChild(iframe)
          iframe.src = url
        })

        if (destino.pathname !== new URL(REDIRECT_URI).pathname) return false // caiu no /login: sessão expirada
        const code = destino.searchParams.get('code')
        const state2 = destino.searchParams.get('state')
        if (!code) return false

        await this._trocarCodeInterno(code, state2, iframeStorage)
        return true
      } catch {
        return false
      }
    },

    // Atualiza os dados de identificação do usuário logado em memória e no
    // localStorage sem precisar de um novo login -- chamado pela tela de
    // usuários quando o admin edita o próprio perfil, para que o topbar
    // reflita a mudança na hora (reatividade do Pinia) em vez de exigir
    // recarregar a página.
    atualizarPerfil(dados) {
      if (!this.usuario) return
      this.usuario = {
        ...this.usuario,
        nome: dados.nome ?? this.usuario.nome,
        nomeGuerra: dados.nomeGuerra ?? null,
        postoGraduacaoBigrama: dados.postoGraduacaoBigrama ?? null,
        omId: dados.omId ?? this.usuario.omId,
        omNome: dados.omNome ?? this.usuario.omNome,
        papeis: dados.papeis ?? this.usuario.papeis,
      }
      const salvo = lerArmazenado()
      if (salvo) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({
          ...salvo,
          nome: this.usuario.nome,
          nomeGuerra: this.usuario.nomeGuerra,
          postoGraduacaoBigrama: this.usuario.postoGraduacaoBigrama,
          omId: this.usuario.omId,
          omNome: this.usuario.omNome,
          papeis: this.usuario.papeis,
        }))
      }
    },

    // Navega de VERDADE pro /logout do backend (não fetch): o cookie de
    // sessão é SameSite=Lax (padrão do Spring Security), então um POST/GET
    // via fetch() cross-origin NUNCA leva o cookie (Lax só manda cookie em
    // navegação de topo) -- o /logout "respondia" 200/302 mas sem sessão
    // nenhuma pra matar, e o próximo /oauth2/authorize reautenticava em
    // silêncio com a sessão antiga ainda viva. logoutSuccessUrl já aponta de
    // volta pro /login (ver AuthorizationServerConfig), então isso já
    // termina no lugar certo sozinho -- quem chama não precisa navegar depois.
    logout() {
      this.token = null
      this.usuario = null
      localStorage.removeItem(STORAGE_KEY)
      window.location.href = `${OAUTH2_BASE_URL}/logout`
    },
  },
})
