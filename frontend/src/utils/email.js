// Espelha EmailInstitucionalValidator (backend): o e-mail do usuário, quando informado, é do domínio @fab.mil.br.
export const DOMINIO_INSTITUCIONAL = 'fab.mil.br'

export function emailInstitucional(email) {
  if (email == null || String(email).trim() === '') return true
  return /^[^@\s]+@fab\.mil\.br$/.test(String(email).trim().toLowerCase())
}
