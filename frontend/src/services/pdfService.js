/**
 * Serviço de geração de PDF via backend.
 * O endpoint POST /api/documentos/:id/pdf deve retornar Content-Type: application/pdf.
 */

function buildFilename(documento) {
  return [documento.especie, documento.numero_basico, documento.numero_secundario]
    .filter(Boolean)
    .join('-') + '.pdf'
}

export async function gerarPdf(documento) {
  const response = await fetch(`/api/documentos/${documento.id}/pdf`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(documento),
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
