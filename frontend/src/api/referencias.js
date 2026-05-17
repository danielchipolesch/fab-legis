/**
 * Endpoints de referência (espécies normativas e assuntos básicos).
 * Usados para popular selects no modo real (VITE_USE_MOCK_API=false).
 */

import * as http from './client.js'

const USE_MOCK = import.meta.env.VITE_USE_MOCK_API !== 'false'

// ─── Mock fallbacks ───────────────────────────────────────────────────────────

const ESPECIES_MOCK = [
  { id: null, sigla: 'ICA',  nome: 'Instrução do Comando da Aeronáutica' },
  { id: null, sigla: 'NSCA', nome: 'Norma de Sistema do Comando da Aeronáutica' },
  { id: null, sigla: 'MCA',  nome: 'Manual do Comando da Aeronáutica' },
  { id: null, sigla: 'RCA',  nome: 'Regulamento do Comando da Aeronáutica' },
  { id: null, sigla: 'DCA',  nome: 'Diretriz do Comando da Aeronáutica' },
  { id: null, sigla: 'PCA',  nome: 'Plano do Comando da Aeronáutica' },
  { id: null, sigla: 'OCA',  nome: 'Ordem do Comando da Aeronáutica' },
  { id: null, sigla: 'TCA',  nome: 'Tabela do Comando da Aeronáutica' },
]

const ASSUNTOS_MOCK = [
  { id: null, codigo: '5',  nome: 'Publicações' },
  { id: null, codigo: '12', nome: 'Administração' },
  { id: null, codigo: '19', nome: 'Organização' },
  { id: null, codigo: '20', nome: 'Organização Principal' },
  { id: null, codigo: '21', nome: 'Organização Geral' },
  { id: null, codigo: '30', nome: 'Pessoal' },
  { id: null, codigo: '37', nome: 'Ensino' },
  { id: null, codigo: '55', nome: 'Operações' },
]

// ─── Interface pública ────────────────────────────────────────────────────────

export async function listEspeciesNormativas() {
  if (USE_MOCK) return ESPECIES_MOCK
  const data = await http.get('/especie-normativa/obter-todos?size=50&sortBy=id')
  return Array.isArray(data) ? data : []
}

export async function listAssuntosBasicos() {
  if (USE_MOCK) return ASSUNTOS_MOCK
  const data = await http.get('/assunto-basico/obter-todos?size=200&sortBy=id')
  return Array.isArray(data) ? data : []
}

/**
 * Normaliza um item de espécie normativa para { id, sigla, nome, label }.
 * A API retorna { id, acronym, name, description }.
 */
export function normalizeEspecie(e) {
  return {
    id:    e.id,
    sigla: e.acronym ?? e.sigla,
    nome:  e.name    ?? e.nome,
    label: `${e.acronym ?? e.sigla} — ${e.name ?? e.nome}`,
  }
}

/**
 * Normaliza um item de assunto básico para { id, codigo, nome, label }.
 * A API retorna { idAssuntoBasico, codigo, nome, descricao }.
 */
export function normalizeAssunto(a) {
  return {
    id:     a.idAssuntoBasico ?? a.id,
    codigo: a.codigo,
    nome:   a.nome,
    label:  `${a.codigo} — ${a.nome}`,
  }
}
