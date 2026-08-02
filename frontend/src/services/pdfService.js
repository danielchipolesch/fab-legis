const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

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

export async function gerarPdf(documento) {
  const response = await fetch(pdfUrl(documento.id), {
    method: 'GET',
  })

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
  a.download = buildFilename(documento)
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
