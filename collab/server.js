import { Server } from '@hocuspocus/server'
import jwt from 'jsonwebtoken'
import { prosemirrorJSONToYDoc, yDocToProsemirrorJSON } from 'y-prosemirror'
import { schema } from './schema.js'

const PORT = Number(process.env.PORT ?? 1234)
const JWT_SECRET = process.env.JWT_SECRET
const BACKEND_URL = process.env.BACKEND_URL ?? 'http://backend:8081/v1'

// Nome da sala Yjs: "documento:{id}:elemento:{elementoId}" -- granularidade por
// elemento, não por documento inteiro (ver Decisão de arquitetura 1 do plano de
// colaboração em tempo real: o modelo de dados já é uma árvore de elementos
// independentes, então cada elemento vira sua própria sala).
const NOME_SALA = /^documento:(\d+):elemento:(\d+)$/

function parseNomeSala(documentName) {
  const match = NOME_SALA.exec(documentName)
  if (!match) throw new Error(`Nome de sala invalido: "${documentName}" (esperado documento:{id}:elemento:{elementoId})`)
  return { documentoId: match[1], elementoId: match[2] }
}

// Campo/fragmento Yjs usado pela extensão @tiptap/extension-collaboration no
// frontend (Fase 4) -- precisa ser o mesmo nome dos dois lados, senão o
// provider do navegador sincroniza com um Y.XmlFragment vazio.
const CAMPO_YJS = 'default'

function conteudoPadrao() {
  return { type: 'doc', content: [{ type: 'paragraph' }] }
}

// Busca recursiva na árvore devolvida por GET /v1/documentos/{id} (itensNormativos,
// cada um com `children`) -- mesmo formato usado no resto do frontend.
function encontrarElemento(itens, elementoId) {
  for (const item of itens ?? []) {
    if (String(item.id) === String(elementoId)) return item
    const achado = encontrarElemento(item.children, elementoId)
    if (achado) return achado
  }
  return null
}

// onAuthenticate roda uma vez por conexão, antes de qualquer sync -- valida a
// assinatura/expiração do MESMO JWT que o backend emite (HS384, ver
// JwtService.java) e, em seguida, pergunta ao backend (com esse mesmo token,
// reaproveitando @documentoAcessoService.podeEditar via o novo endpoint
// GET /documentos/{id}/pode-editar) se esta pessoa pode editar o documento.
// O contexto retornado aqui (usuarioId/nome/token) fica disponível nos hooks
// seguintes desta mesma conexão -- é assim que onLoadDocument/onStoreDocument
// sabem com qual token chamar o backend, sem precisar de uma credencial de
// serviço separada: cada leitura/escrita no backend acontece EM NOME do
// usuário conectado, então a autorização (podeEditar) vale pra cada uma, não
// só na entrada da sala.
async function onAuthenticate({ token, documentName }) {
  if (!token) throw new Error('Token ausente.')

  let claims
  try {
    claims = jwt.verify(token, JWT_SECRET, { algorithms: ['HS384'] })
  } catch {
    throw new Error('Token invalido ou expirado.')
  }

  const { documentoId } = parseNomeSala(documentName)
  const resposta = await fetch(`${BACKEND_URL}/documentos/${documentoId}/pode-editar`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!resposta.ok) throw new Error(`Sem permissao para editar o documento ${documentoId}.`)

  return {
    usuarioId: claims.sub,
    nome: claims.nome,
    token,
  }
}

// Roda quando a sala é aberta pela primeira vez (nenhuma conexão anterior com
// o Y.Doc já em memória) -- busca o conteudo atual persistido e inicializa o
// Y.Doc a partir dele. O Postgres continua sendo a fonte de verdade entre
// sessões colaborativas; o Y.Doc em memória só existe enquanto a sala tem
// gente conectada.
async function onLoadDocument({ documentName, context }) {
  const { documentoId, elementoId } = parseNomeSala(documentName)

  const resposta = await fetch(`${BACKEND_URL}/documentos/${documentoId}`, {
    headers: { Authorization: `Bearer ${context.token}` },
  })
  if (!resposta.ok) throw new Error(`Falha ao carregar documento ${documentoId}: HTTP ${resposta.status}`)

  const doc = await resposta.json()
  const elemento = encontrarElemento(doc.itensNormativos, elementoId)
  const conteudoBruto = elemento?.elementContent
  let json
  try {
    json = conteudoBruto ? JSON.parse(conteudoBruto) : conteudoPadrao()
  } catch {
    json = conteudoPadrao()
  }

  return prosemirrorJSONToYDoc(schema, json, CAMPO_YJS)
}

// Debounced (ver Server.configure abaixo) -- converte o Y.Doc de volta para
// JSON TipTap e grava só o `conteudo` deste elemento via o endpoint granular
// da Fase 1 (nunca o PATCH /secoes em massa). Esse mesmo JSON é o que o FOP
// (geração de PDF), a numeração e o diff de emendas já sabem ler -- nada
// muda nessas camadas.
async function onStoreDocument({ documentName, document, context }) {
  const { documentoId, elementoId } = parseNomeSala(documentName)
  const json = yDocToProsemirrorJSON(document, CAMPO_YJS)

  const resposta = await fetch(`${BACKEND_URL}/documentos/${documentoId}/elementos/${elementoId}/conteudo`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${context.token}`,
    },
    body: JSON.stringify({ conteudo: JSON.stringify(json) }),
  })
  if (!resposta.ok) throw new Error(`Falha ao persistir elemento ${elementoId}: HTTP ${resposta.status}`)
}

if (!JWT_SECRET) {
  console.error('JWT_SECRET não definido -- não é possível validar tokens. Abortando.')
  process.exit(1)
}

Server.configure({
  port: PORT,
  debounce: 2000,
  maxDebounce: 10000,
  onAuthenticate,
  onLoadDocument,
  onStoreDocument,
}).listen()
