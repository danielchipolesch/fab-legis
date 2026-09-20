import { useAuthStore } from '@/stores/auth.js'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

// Este serviço faz fetch() cru (não via api/client.js, pois a resposta é um blob
// binário, não JSON) -- precisa montar o header de autenticação manualmente, ou
// toda chamada cai no .anyRequest().authenticated() do backend como 401/403.
function authHeaders() {
  const token = useAuthStore().token
  return token ? { Authorization: `Bearer ${token}` } : {}
}

// Mensagem legível de uma resposta de erro: o backend responde JSON ({ message }), inclusive o 503
// de "sistema ocupado" (todas as vagas de geração de PDF em uso -- ver LimitadorGeracaoPdf).
async function mensagemDeErro(response) {
  let msg = `Erro ${response.status}`
  try {
    const text = await response.text()
    if (text) {
      try { msg = JSON.parse(text).message ?? text } catch { msg = text }
    }
  } catch { /* noop */ }
  return msg
}

function sanitize(str) {
  // Remove apenas caracteres proibidos em nomes de arquivo (Windows + Linux)
  return (str ?? '').replace(/[<>:"/\\|?*]/g, '').trim()
}

// extensao: 'pdf' ou 'html' -- mesmo esquema de nome nos dois formatos, já que a
// regra de geração/armazenamento é a mesma (ver DocumentoStatusService).
function buildFilename(documento, extensao) {
  const numero = [documento.numero_basico, documento.numero_secundario].filter(Boolean).join('-')
  const ano = documento.data_criacao ? documento.data_criacao.slice(0, 4) : String(new Date().getFullYear())
  const partes = [
    sanitize(documento.especie),
    sanitize(numero),
    sanitize(documento.titulo),
    sanitize(ano),
  ].filter(Boolean)
  return partes.join('_') + '.' + extensao
}

// versao: 'VIGENTE' (a da Situação BCA) ou 'TRAMITACAO' (a da etapa local em curso). Sem ela, o
// backend serve a em tramitação, se houver, senão a vigente (ver VersoesDocumento no backend).
function pdfUrl(documentoId, versao) {
  return `${API_BASE}/documentos/${documentoId}/pdf${versao ? `?versao=${versao}` : ''}`
}

function htmlUrl(documentoId, versao) {
  return `${API_BASE}/documentos/${documentoId}/html${versao ? `?versao=${versao}` : ''}`
}

// Só o blob do PDF (para exibir num iframe, sem baixar) -- a versão em tramitação é renderizada
// na hora pelo backend quando o texto ainda muda, então quem chama deve pedir só sob demanda.
export async function buscarPdfBlob(documentoId, versao) {
  const response = await fetch(pdfUrl(documentoId, versao), { method: 'GET', headers: authHeaders() })
  if (!response.ok) {
    throw new Error(await mensagemDeErro(response))
  }
  // Força o tipo: sem ele (ou com um genérico) o iframe mostra os bytes do PDF como texto.
  const blob = await response.blob()
  return new Blob([blob], { type: 'application/pdf' })
}

async function baixarArquivo(response, filename) {
  if (!response.ok) {
    throw new Error(await mensagemDeErro(response))
  }

  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

export async function gerarPdf(documento, versao) {
  const response = await fetch(pdfUrl(documento.id, versao), { method: 'GET', headers: authHeaders() })
  await baixarArquivo(response, buildFilename(documento, 'pdf'))
}

export async function gerarHtml(documento, versao) {
  const response = await fetch(htmlUrl(documento.id, versao), { method: 'GET', headers: authHeaders() })
  await baixarArquivo(response, buildFilename(documento, 'html'))
}

export async function gerarMapaAlteracaoPdf(documentoId, payload, filenameHint) {
  // Abre a aba ANTES do fetch (síncrono, na mesma call stack do clique) para não
  // ser bloqueado pelo popup blocker — só depois preenchemos a URL com o PDF.
  const novaAba = window.open('', '_blank')
  const response = await fetch(`${API_BASE}/documentos/${documentoId}/mapa-alteracao/pdf`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload),
  })
  if (!response.ok) {
    novaAba?.close()
    throw new Error(await mensagemDeErro(response))
  }
  const blob = await response.blob()
  // Empacota o blob num File nomeado: navegadores usam esse nome como sugestão ao
  // salvar o PDF a partir da aba (Ctrl+S / botão de download do visualizador nativo).
  const filename = `mapa-alteracao_${sanitize(filenameHint)}.pdf`
  const file = new File([blob], filename, { type: 'application/pdf' })
  const url = URL.createObjectURL(file)
  if (novaAba) {
    novaAba.location.href = url
  } else {
    window.open(url, '_blank')
  }
}
