// Mesmo algoritmo do backend (CpfValidator.java) -- dígitos verificadores mod
// 11, rejeitando sequências de um único dígito repetido. Mantido em duplicata
// de propósito: o frontend só usa isso para feedback imediato no formulário,
// o backend é quem de fato garante a validade (nunca confiar só no cliente).
export function validarCpf(cpf) {
  const digits = onlyDigits(cpf)
  if (digits.length !== 11) return false
  if (/^(\d)\1{10}$/.test(digits)) return false

  const n = digits.split('').map(Number)
  if (calcCheckDigit(n, 9, 10) !== n[9]) return false
  return calcCheckDigit(n, 10, 11) === n[10]
}

export function onlyDigits(cpf) {
  return (cpf ?? '').replace(/\D/g, '')
}

export function formatarCpf(cpf) {
  const d = onlyDigits(cpf)
  if (d.length !== 11) return cpf ?? ''
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`
}

// Máscara progressiva para uso em @update:model-value de um q-input, aplicada
// enquanto o usuário digita.
export function mascaraCpf(valor) {
  const d = onlyDigits(valor).slice(0, 11)
  if (d.length <= 3) return d
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`
}

function calcCheckDigit(digits, length, startWeight) {
  let sum = 0
  for (let i = 0; i < length; i++) sum += digits[i] * (startWeight - i)
  const rest = sum % 11
  return rest < 2 ? 0 : 11 - rest
}
