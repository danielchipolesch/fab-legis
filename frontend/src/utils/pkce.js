// PKCE (RFC 7636) manual, sem lib nova -- pouco código, evita depender de
// oidc-client-ts pra um fluxo que é só isto: gerar um verifier aleatório,
// derivar o challenge (SHA-256, base64url) e mandar pro /oauth2/authorize.

function base64Url(bytes) {
  let binario = ''
  for (const b of bytes) binario += String.fromCharCode(b)
  return btoa(binario).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

export function gerarCodeVerifier() {
  const bytes = new Uint8Array(32)
  crypto.getRandomValues(bytes)
  return base64Url(bytes)
}

export async function gerarCodeChallenge(verifier) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))
  return base64Url(new Uint8Array(digest))
}

export function gerarState() {
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  return base64Url(bytes)
}
