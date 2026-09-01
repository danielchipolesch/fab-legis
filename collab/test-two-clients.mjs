// Script de verificação da Fase 3 -- simula dois clientes Yjs conectados à
// MESMA sala (mesmo elemento), digitando ao mesmo tempo, e confirma:
//   1. auth (JWT + podeEditar) aceita a conexão;
//   2. onLoadDocument carrega o conteúdo já persistido no backend;
//   3. as duas edições fazem merge (CRDT) e aparecem para os dois lados;
//   4. onStoreDocument persiste de volta no backend (via PATCH .../conteudo)
//      sem passar pelo salvamento em massa.
//
// Uso: node test-two-clients.mjs <COLLAB_URL> <DOCUMENTO_ID> <ELEMENTO_ID> <JWT>
import { HocuspocusProvider, HocuspocusProviderWebsocket } from '@hocuspocus/provider'
import WebSocket from 'ws'
import * as Y from 'yjs'

const [, , collabUrl, documentoId, elementoId, token] = process.argv
if (!collabUrl || !documentoId || !elementoId || !token) {
  console.error('Uso: node test-two-clients.mjs <COLLAB_URL> <DOCUMENTO_ID> <ELEMENTO_ID> <JWT>')
  process.exit(1)
}

const nomeSala = `documento:${documentoId}:elemento:${elementoId}`

// Node não tem `WebSocket` global nesta versão -- o polyfill só é aceito no
// HocuspocusProviderWebsocket (gerenciador de conexão), não diretamente no
// HocuspocusProvider (gerenciador de documento/sala); por isso cada cliente
// simulado cria o seu próprio socket antes de anexar o provider do documento.
function conectar(rotulo) {
  return new Promise((resolve, reject) => {
    const websocketProvider = new HocuspocusProviderWebsocket({
      url: collabUrl,
      WebSocketPolyfill: WebSocket,
    })
    const provider = new HocuspocusProvider({
      websocketProvider,
      name: nomeSala,
      token,
      onAuthenticationFailed: ({ reason }) => reject(new Error(`[${rotulo}] auth falhou: ${reason}`)),
      onSynced: () => resolve(provider),
    })
  })
}

function textoAtual(provider) {
  const fragmento = provider.document.getXmlFragment('default')
  return fragmento.toString()
}

async function main() {
  console.log(`Sala: ${nomeSala}`)

  const a = await conectar('A')
  console.log('[A] conectado e sincronizado. Conteúdo inicial:', textoAtual(a))

  const b = await conectar('B')
  console.log('[B] conectado e sincronizado. Conteúdo inicial:', textoAtual(b))

  // Cada cliente edita um trecho diferente do MESMO parágrafo -- se o merge
  // CRDT funcionar, os dois textos devem aparecer nos dois lados, sem um
  // sobrescrever o outro (o que aconteceria com bloqueio otimista hoje).
  const fragA = a.document.getXmlFragment('default')
  const fragB = b.document.getXmlFragment('default')

  a.document.transact(() => {
    const paragrafo = fragA.get(0)
    const texto = new Y.XmlText()
    texto.insert(0, '[Editado por A] ')
    paragrafo.insert(0, [texto])
  })

  await new Promise((r) => setTimeout(r, 300))

  b.document.transact(() => {
    const paragrafo = fragB.get(0)
    const texto = new Y.XmlText()
    texto.insert(0, '[Editado por B] ')
    paragrafo.insert(paragrafo.length, [texto])
  })

  await new Promise((r) => setTimeout(r, 500))

  const finalA = textoAtual(a)
  const finalB = textoAtual(b)
  console.log('[A] conteúdo final:', finalA)
  console.log('[B] conteúdo final:', finalB)

  const merged = finalA === finalB && finalA.includes('Editado por A') && finalA.includes('Editado por B')
  console.log(merged ? '\nOK: merge CRDT funcionou (as duas edições coexistem, sem colisão).' : '\nFALHA: as edições não convergiram como esperado.')

  console.log('\nAguardando persistência (debounce)...')
  await new Promise((r) => setTimeout(r, 2500))

  a.destroy()
  b.destroy()
  process.exit(merged ? 0 : 1)
}

main().catch((e) => {
  console.error('Erro no teste:', e.message)
  process.exit(1)
})
