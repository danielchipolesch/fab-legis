import { useAuthStore } from '@/stores/auth.js'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

// Este serviço faz fetch() cru (não via api/client.js, pois a resposta é um blob
// binário, não JSON) -- precisa montar o header de autenticação manualmente, ou
// toda chamada cai no .anyRequest().authenticated() do backend como 401/403.
function authHeaders() {
  const token = useAuthStore().token
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function sanitize(str) {
  // Remove apenas caracteres proibidos em nomes de arquivo (Windows + Linux)
  return (str ?? '').replace(/[<>:"/\\|?*]/g, '').trim()
}

function buildFilename(documento) {
  const numero = [documento.numero_basico, documento.numero_secundario].filter(Boolean).join('-')
  const ano = documento.data_criacao ? documento.data_criacao.slice(0, 4) : String(new Date().getFullYear())
  const partes = [
    sanitize(documento.especie),
    sanitize(numero),
    sanitize(documento.titulo),
    sanitize(ano),
  ].filter(Boolean)
  return partes.join('_') + '.pdf'
}

export function pdfUrl(documentoId) {
  return `${API_BASE}/documentos/${documentoId}/pdf`
}

async function baixarPdf(response, filename) {
  if (!response.ok) {
    let msg = `Erro ${response.status}`
    try {
      const text = await response.text()
      if (text) msg = text
    } catch { /* noop */ }
    throw new Error(msg)
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

export async function gerarPdf(documento) {
  const response = await fetch(pdfUrl(documento.id), { method: 'GET', headers: authHeaders() })
  await baixarPdf(response, buildFilename(documento))
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
    let msg = `Erro ${response.status}`
    try {
      const text = await response.text()
      if (text) msg = text
    } catch { /* noop */ }
    throw new Error(msg)
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
