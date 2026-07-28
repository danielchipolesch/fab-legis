const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

function buildFilename(documento) {
  return [documento.especie, documento.numero_basico, documento.numero_secundario]
    .filter(Boolean)
    .join('-') + '.pdf'
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
