import { describe, it, expect } from 'vitest'
import { acaoDaLinha, detalhesDoDocumento, selosDaLinha } from './painel.js'

// Os cards do hub (docs/funcionalidades.md, "Hub"): a ação de cada linha leva direto ao documento ou à tela da ação, e
// nenhuma ação é executada no hub.

const doc = (extra) => ({
  id: 7, situacao_bca: 'NAO_PUBLICADO', situacao_local: 'RASCUNHO', eh_autor_ou_coautor: true,
  tipo_de_especie: 'CONVENCIONAL', codigo_documento: 'ICA 11-3', ...extra,
})

describe('acaoDaLinha — meus documentos em tramitação', () => {
  it.each(['RASCUNHO', 'MINUTA', 'EM_ALTERACAO'])('%s: Editar, quando sou autor ou coautor', (etapa) => {
    const a = acaoDaLinha('minhas_em_tramitacao', doc({ situacao_local: etapa }))
    expect(a.rotulo).toBe('Editar')
    expect(a.rota).toEqual({ name: 'documento-editar', params: { id: 7 } })
    expect(a.pendente).toBe(false)
  })

  it.each(['EM_REVISAO', 'EM_PUBLICACAO', 'ANALISE_REVOGACAO', 'EM_REVOGACAO'])(
    '%s: Acompanhar, porque está com outra pessoa', (etapa) => {
      const a = acaoDaLinha('minhas_em_tramitacao', doc({ situacao_local: etapa }))
      expect(a.rotulo).toBe('Acompanhar')
      expect(a.rota.name).toBe('documento-visualizar')
    })

  it('sem posse (nem autor nem coautor) não edita', () => {
    expect(acaoDaLinha('minhas_em_tramitacao', doc({ eh_autor_ou_coautor: false })).rotulo).toBe('Acompanhar')
  })
})

describe('acaoDaLinha — em tramitação nas OMs (só visualizo)', () => {
  it.each(['RASCUNHO', 'MINUTA', 'EM_ALTERACAO', 'EM_REVISAO', 'EM_PUBLICACAO'])(
    '%s: sempre Acompanhar, nunca Editar, mesmo com a etapa de escrita', (etapa) => {
      const a = acaoDaLinha('em_tramitacao_de_outros', doc({ situacao_local: etapa, eh_autor_ou_coautor: false }))
      expect(a).toEqual({ rotulo: 'Acompanhar', rota: { name: 'documento-visualizar', params: { id: 7 } }, pendente: false })
    })

  it('mesmo que o documento venha marcado como meu, o card não oferece Editar', () => {
    expect(acaoDaLinha('em_tramitacao_de_outros', doc({ situacao_local: 'RASCUNHO', eh_autor_ou_coautor: true })).rotulo).toBe('Acompanhar')
  })
})

describe('acaoDaLinha — aguardando minha ação', () => {
  it('revisão: abre o documento para revisar, lembrando a origem, e acende o aviso', () => {
    const a = acaoDaLinha('aguardando', doc({ situacao_local: 'EM_REVISAO' }))
    expect(a).toMatchObject({ rotulo: 'Revisar', pendente: true })
    expect(a.rota).toEqual({ name: 'documento-editar', params: { id: 7 }, query: { origem: 'revisao' } })
  })

  it('análise de revogação: abre o documento em leitura', () => {
    const a = acaoDaLinha('aguardando', doc({ situacao_bca: 'PUBLICADO', situacao_local: 'ANALISE_REVOGACAO' }))
    expect(a).toMatchObject({ rotulo: 'Analisar revogação', pendente: true })
    expect(a.rota).toEqual({ name: 'documento-visualizar', params: { id: 7 }, query: { origem: 'revisao' } })
  })

  it('publicação e revogação: a tela de Publicação, onde a ação acontece', () => {
    expect(acaoDaLinha('aguardando', doc({ situacao_local: 'EM_PUBLICACAO' })))
      .toMatchObject({ rotulo: 'Publicar', rota: { name: 'publicacao' }, pendente: true })
    expect(acaoDaLinha('aguardando', doc({ situacao_bca: 'PUBLICADO', situacao_local: 'EM_REVOGACAO' })))
      .toMatchObject({ rotulo: 'Revogar', rota: { name: 'publicacao' }, pendente: true })
  })
})

describe('acaoDaLinha — publicados e revogados', () => {
  it('sempre Visualizar, sem aviso', () => {
    const a = acaoDaLinha('publicados', doc({ situacao_bca: 'PUBLICADO', situacao_local: 'SEM_ETAPA' }))
    expect(a).toEqual({ rotulo: 'Visualizar', rota: { name: 'documento-visualizar', params: { id: 7 } }, pendente: false })
  })
})

describe('selosDaLinha', () => {
  it('publicados e revogados: só a situação oficial', () => {
    expect(selosDaLinha('publicados', doc({ situacao_bca: 'PUBLICADO', situacao_local: 'SEM_ETAPA' }))).toBe('bca')
    expect(selosDaLinha('publicados', doc({ situacao_bca: 'REVOGADO', situacao_local: 'SEM_ETAPA' }))).toBe('bca')
  })

  it('nos cards de trabalho, um documento nunca publicado leva só a etapa', () => {
    expect(selosDaLinha('minhas_em_tramitacao', doc({ situacao_bca: 'NAO_PUBLICADO', situacao_local: 'RASCUNHO' }))).toBe('local')
    expect(selosDaLinha('aguardando', doc({ situacao_bca: 'NAO_PUBLICADO', situacao_local: 'EM_REVISAO' }))).toBe('local')
  })

  it('nos cards de trabalho, um publicado em alteração ou revogação leva a etapa e o selo de publicado', () => {
    expect(selosDaLinha('minhas_em_tramitacao', doc({ situacao_bca: 'PUBLICADO', situacao_local: 'EM_ALTERACAO' }))).toBe('ambos')
    expect(selosDaLinha('aguardando', doc({ situacao_bca: 'PUBLICADO', situacao_local: 'EM_PUBLICACAO' }))).toBe('ambos')
    expect(selosDaLinha('minhas_em_tramitacao', doc({ situacao_bca: 'PUBLICADO', situacao_local: 'EM_REVOGACAO' }))).toBe('ambos')
  })
})

describe('detalhesDoDocumento', () => {
  const valor = (linhas, rotulo) => linhas.find(l => l.rotulo === rotulo)?.valor

  it('sempre traz o que identifica o documento: módulo, espécie, código, OM, autoria e criação', () => {
    const l = detalhesDoDocumento('minhas_em_tramitacao', doc({
      autor_nome: 'Fulano', especie: 'ICA', assunto_basico: 'Organização', om_nome: 'CAE', data_criacao: '2026-01-05',
    }), { coautores: ['Beltrano', 'Cicrano'], formatarData: (d) => `em ${d}` })
    expect(valor(l, 'Módulo')).toBe('Espécies Convencionais')
    expect(valor(l, 'Espécie')).toBe('ICA')
    expect(valor(l, 'Código')).toBe('ICA 11-3')
    expect(valor(l, 'Assunto Básico')).toBe('Organização')
    expect(valor(l, 'OM')).toBe('CAE')
    expect(valor(l, 'Autor')).toBe('Fulano')
    expect(valor(l, 'Coautores')).toBe('Beltrano, Cicrano')
    expect(valor(l, 'Criado em')).toBe('em 2026-01-05')
    expect(valor(detalhesDoDocumento('minhas_em_tramitacao', doc({})), 'Coautores')).toBe('Nenhum')
  })

  it('a OM aparece com o nome e a sigla; sem sigla, só o nome', () => {
    expect(valor(detalhesDoDocumento('minhas_em_tramitacao', doc({ om_nome: 'Centro de Aquisições Específicas', om_sigla: 'CAE' })), 'OM'))
      .toBe('Centro de Aquisições Específicas (CAE)')
    expect(valor(detalhesDoDocumento('minhas_em_tramitacao', doc({ om_nome: 'Centro de Aquisições Específicas' })), 'OM'))
      .toBe('Centro de Aquisições Específicas')
  })

  it('a NPA não tem Assunto Básico', () => {
    const l = detalhesDoDocumento('minhas_em_tramitacao', doc({ tipo_de_especie: 'COMUNICACAO_OFICIAL_PADRONIZADA', especie: 'NPA' }))
    expect(valor(l, 'Módulo')).toBe('NPA')
    expect(valor(l, 'Assunto Básico')).toBeUndefined()
  })

  it('a última alteração aparece nos três cards; sem data, um traço', () => {
    for (const cartao of ['minhas_em_tramitacao', 'aguardando', 'em_tramitacao_de_outros', 'publicados']) {
      expect(valor(detalhesDoDocumento(cartao, doc({ data_alteracao: '2026-02-01' }), { formatarData: (d) => `em ${d}` }), 'Última alteração')).toBe('em 2026-02-01')
    }
    expect(valor(detalhesDoDocumento('minhas_em_tramitacao', doc({})), 'Última alteração')).toBe('—')
  })

  it('um documento em alteração (já publicado) também mostra a publicação vigente', () => {
    const l = detalhesDoDocumento('minhas_em_tramitacao', doc({
      situacao_bca: 'PUBLICADO', situacao_local: 'EM_ALTERACAO', data_publicacao: '2026-03-01', portaria_referencia: 'Portaria nº 12', bca_referencia: 'BCA nº 30',
    }))
    expect(valor(l, 'Portaria')).toBe('Portaria nº 12')
    expect(valor(l, 'BCA')).toBe('BCA nº 30')
    expect(valor(detalhesDoDocumento('minhas_em_tramitacao', doc({})), 'Portaria')).toBeUndefined()
  })

  it('em tramitação: com quem o documento está', () => {
    expect(valor(detalhesDoDocumento('minhas_em_tramitacao', doc({ situacao_local: 'MINUTA' })), 'Com')).toBe('Autor e coautores')
    expect(valor(detalhesDoDocumento('minhas_em_tramitacao', doc({ situacao_local: 'EM_REVISAO', revisor_atribuido_nome: 'Ana' })), 'Com')).toBe('Ana')
    expect(valor(detalhesDoDocumento('minhas_em_tramitacao', doc({ situacao_local: 'EM_PUBLICACAO', publicador_atribuido_nome: 'Rui' })), 'Com')).toBe('Rui')
  })

  it('aguardando: o que se espera da pessoa', () => {
    expect(valor(detalhesDoDocumento('aguardando', doc({ situacao_local: 'EM_REVISAO' })), 'Se espera de você'))
      .toBe('Revisar o documento e aprová-lo ou devolvê-lo')
  })

  it('em tramitação nas OMs: com quem o documento está, como nos meus', () => {
    expect(valor(detalhesDoDocumento('em_tramitacao_de_outros', doc({ situacao_local: 'EM_REVISAO', revisor_atribuido_nome: 'Ana' })), 'Com')).toBe('Ana')
  })

  it('publicados: convencional mostra portaria e BCA; NPA, o Boletim Interno', () => {
    const conv = detalhesDoDocumento('publicados', doc({
      situacao_bca: 'PUBLICADO', portaria_referencia: 'Portaria nº 12', bca_referencia: 'BCA nº 30', om_nome: 'CAE', data_publicacao: '2026-03-01',
    }))
    expect(valor(conv, 'Portaria')).toBe('Portaria nº 12')
    expect(valor(conv, 'BCA')).toBe('BCA nº 30')
    expect(valor(conv, 'OM')).toBe('CAE')

    const npa = detalhesDoDocumento('publicados', doc({
      tipo_de_especie: 'COMUNICACAO_OFICIAL_PADRONIZADA', situacao_bca: 'PUBLICADO', bca_referencia: 'Boletim Interno Ostensivo nº 15',
    }))
    expect(valor(npa, 'Boletim Interno')).toBe('Boletim Interno Ostensivo nº 15')
    expect(valor(npa, 'BCA')).toBeUndefined()
  })

  it('revogado mostra a data da revogação', () => {
    const l = detalhesDoDocumento('publicados', doc({ situacao_bca: 'REVOGADO', data_revogacao: '2026-06-02' }),
      { formatarData: (d) => `em ${d}` })
    expect(valor(l, 'Revogado em')).toBe('em 2026-06-02')
  })
})
