import { describe, it, expect } from 'vitest'
import { MODULOS, moduloDe, moduloDoDocumento, regrasDe } from './index.js'

// Módulos do sistema: um por tipo de espécie (NSCA 5-3) -- Espécies Convencionais e Comunicações Oficiais Padronizadas (NPA).
describe('módulos', () => {
  it('há um módulo por tipo de espécie, com rota e caminho próprios', () => {
    expect(MODULOS.map(m => m.tipoDeEspecie)).toEqual(['CONVENCIONAL', 'COMUNICACAO_OFICIAL_PADRONIZADA'])
    expect(new Set(MODULOS.map(m => m.rota)).size).toBe(MODULOS.length)
    expect(new Set(MODULOS.map(m => m.caminho)).size).toBe(MODULOS.length)
  })

  it('o documento pertence ao módulo do tipo da sua espécie', () => {
    expect(moduloDoDocumento({ tipo_de_especie: 'COMUNICACAO_OFICIAL_PADRONIZADA' }).rota).toBe('modulo-npa')
    expect(moduloDoDocumento({ tipo_de_especie: 'CONVENCIONAL' }).rota).toBe('modulo-convencionais')
  })

  it('sem tipo informado cai no módulo convencional, como as regras', () => {
    expect(moduloDoDocumento(null).tipoDeEspecie).toBe('CONVENCIONAL')
    expect(moduloDe(undefined)).toBe(regrasDe('CONVENCIONAL').modulo)
  })

  it('as opções do botão Criar do hub são só o nome da espécie, sem o verbo', () => {
    expect(MODULOS.map(m => m.rotuloDoCriar)).toEqual(['Espécie Convencional', 'NPA'])
  })

  it('a NPA não mostra as colunas de espécie e de assunto básico e fala em Boletim Interno', () => {
    const npa = moduloDe('COMUNICACAO_OFICIAL_PADRONIZADA')
    expect(npa.colunasOcultas).toEqual(['especie', 'assunto_basico'])
    expect(npa.rotuloDaSituacaoOficial).toBe('Boletim Interno')
    expect(moduloDe('CONVENCIONAL').colunasOcultas).toEqual([])
  })
})

// O rótulo do anexo de imagem vem das regras da espécie -- o mesmo que o PDF (RotuloDosAnexos, backend) -- e é o que a
// lista de anexos da barra lateral mostra: romano nas convencionais (o ANEXO I é o corpo normativo) e letras na NPA.
describe('rótulo do anexo por espécie', () => {
  it('espécie convencional: ANEXO II, III, IV…', () => {
    const { rotuloDoAnexo } = regrasDe('CONVENCIONAL')
    expect([1, 2, 3, 8, 13].map(rotuloDoAnexo)).toEqual(['ANEXO II', 'ANEXO III', 'ANEXO IV', 'ANEXO IX', 'ANEXO XIV'])
  })

  it('NPA: ANEXO A, B, C… e AA depois do Z', () => {
    const { rotuloDoAnexo } = regrasDe('COMUNICACAO_OFICIAL_PADRONIZADA')
    expect([1, 2, 3, 26, 27].map(rotuloDoAnexo)).toEqual(['ANEXO A', 'ANEXO B', 'ANEXO C', 'ANEXO Z', 'ANEXO AA'])
  })
})

// Quem publica cada espécie e se ela é de uso interno da OM (espelha RegrasDaEspecieNormativa/RegrasDoCicloDeVidaDoDocumento, backend):
// as convencionais, o Publicador do BCA (CENDOC); a NPA, o Publicador do Boletim Interno da própria OM -- e ela fica fora do painel geral.
describe('quem publica e uso interno da OM', () => {
  it('a convencional é publicada pelo CENDOC e tem âmbito do COMAER', () => {
    expect(regrasDe('CONVENCIONAL').quemPublica).toBe('Publicadores do BCA (CENDOC)')
    expect(regrasDe('CONVENCIONAL').usoInternoDaOm).toBe(false)
  })

  it('a NPA é publicada pela própria OM e é de uso interno dela', () => {
    expect(regrasDe('COMUNICACAO_OFICIAL_PADRONIZADA').quemPublica).toBe('Publicadores do Boletim Interno da OM')
    expect(regrasDe('COMUNICACAO_OFICIAL_PADRONIZADA').usoInternoDaOm).toBe(true)
  })
})
