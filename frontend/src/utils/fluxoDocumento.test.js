import { describe, it, expect } from 'vitest'
import {
  destinoDeAprovacao, destinoDeDevolucao, DESTINO_DE_CONCLUSAO, ehRevogacao, ehPrimeiraPublicacao,
  ehAlteracaoPublicada, podeDevolverPublicacao, temVersaoVigente, temVersaoEmTramitacao, versaoPadrao,
  eventoDoHistorico, exibeSeloRevogado, exibePortaria,
} from './fluxoDocumento.js'
import {
  temEtapaEmCurso, situacaoBcaMeta, situacaoLocalMeta, SITUACAO_BCA_META, SITUACAO_LOCAL_META,
} from './statusDocumento.js'

const doc = (situacao_bca, situacao_local) => ({ situacao_bca, situacao_local })

describe('destinoDeAprovacao', () => {
  it('texto em revisão vai para publicação', () => {
    expect(destinoDeAprovacao(doc('NAO_PUBLICADO', 'EM_REVISAO'))).toBe('EM_PUBLICACAO')
    expect(destinoDeAprovacao(doc('PUBLICADO', 'EM_REVISAO'))).toBe('EM_PUBLICACAO')
  })
  it('análise de revogação vai para EM_REVOGACAO', () => {
    expect(destinoDeAprovacao(doc('PUBLICADO', 'ANALISE_REVOGACAO'))).toBe('EM_REVOGACAO')
  })
})

describe('destinoDeDevolucao', () => {
  it('nunca publicado volta para a minuta', () => {
    expect(destinoDeDevolucao(doc('NAO_PUBLICADO', 'EM_REVISAO'))).toBe('MINUTA')
    expect(destinoDeDevolucao(doc('NAO_PUBLICADO', 'EM_PUBLICACAO'))).toBe('MINUTA')
  })
  it('já publicado volta para a alteração em curso', () => {
    expect(destinoDeDevolucao(doc('PUBLICADO', 'EM_REVISAO'))).toBe('EM_ALTERACAO')
    expect(destinoDeDevolucao(doc('PUBLICADO', 'EM_PUBLICACAO'))).toBe('EM_ALTERACAO')
  })
  it('devolver a análise de revogação só encerra a etapa: o documento segue PUBLICADO', () => {
    expect(destinoDeDevolucao(doc('PUBLICADO', 'ANALISE_REVOGACAO'))).toBe('SEM_ETAPA')
  })
})

describe('publicação e revogação', () => {
  it('concluir a etapa é SEM_ETAPA', () => {
    expect(DESTINO_DE_CONCLUSAO).toBe('SEM_ETAPA')
  })
  it('reconhece a revogação', () => {
    expect(ehRevogacao(doc('PUBLICADO', 'EM_REVOGACAO'))).toBe(true)
    expect(ehRevogacao(doc('PUBLICADO', 'EM_PUBLICACAO'))).toBe(false)
  })
  it('a parte preliminar só é coletada na primeira publicação', () => {
    expect(ehPrimeiraPublicacao(doc('NAO_PUBLICADO', 'EM_PUBLICACAO'))).toBe(true)
    expect(ehPrimeiraPublicacao(doc('PUBLICADO', 'EM_PUBLICACAO'))).toBe(false)
    expect(ehPrimeiraPublicacao(doc('PUBLICADO', 'EM_REVOGACAO'))).toBe(false)
  })
  it('publicar um documento já publicado é uma alteração', () => {
    expect(ehAlteracaoPublicada(doc('PUBLICADO', 'EM_PUBLICACAO'))).toBe(true)
    expect(ehAlteracaoPublicada(doc('NAO_PUBLICADO', 'EM_PUBLICACAO'))).toBe(false)
    expect(ehAlteracaoPublicada(doc('PUBLICADO', 'EM_REVOGACAO'))).toBe(false)
  })
  it('só EM_PUBLICACAO pode ser devolvido (EM_REVOGACAO só sai para REVOGADO)', () => {
    expect(podeDevolverPublicacao(doc('NAO_PUBLICADO', 'EM_PUBLICACAO'))).toBe(true)
    expect(podeDevolverPublicacao(doc('PUBLICADO', 'EM_REVOGACAO'))).toBe(false)
  })
})

describe('versões do documento', () => {
  it('a versão vigente existe desde a primeira publicação', () => {
    expect(temVersaoVigente(doc('NAO_PUBLICADO', 'MINUTA'))).toBe(false)
    expect(temVersaoVigente(doc('PUBLICADO', 'SEM_ETAPA'))).toBe(true)
    expect(temVersaoVigente(doc('REVOGADO', 'SEM_ETAPA'))).toBe(true)
  })
  it('a versão em tramitação existe enquanto houver etapa em curso', () => {
    expect(temVersaoEmTramitacao(doc('PUBLICADO', 'EM_ALTERACAO'))).toBe(true)
    expect(temVersaoEmTramitacao(doc('NAO_PUBLICADO', 'RASCUNHO'))).toBe(true)
    expect(temVersaoEmTramitacao(doc('PUBLICADO', 'SEM_ETAPA'))).toBe(false)
  })
  it('a versão padrão é a em tramitação, se houver, senão a vigente', () => {
    expect(versaoPadrao(doc('PUBLICADO', 'EM_ALTERACAO'))).toBe('TRAMITACAO')
    expect(versaoPadrao(doc('PUBLICADO', 'SEM_ETAPA'))).toBe('VIGENTE')
    expect(versaoPadrao(doc('REVOGADO', 'SEM_ETAPA'))).toBe('VIGENTE')
  })
})

describe('metadados de situação', () => {
  it('toda situação BCA e local tem rótulo, ícone e cor', () => {
    for (const m of [...Object.values(SITUACAO_BCA_META), ...Object.values(SITUACAO_LOCAL_META)]) {
      expect(m.label).toBeTruthy()
      expect(m.icon).toBeTruthy()
      expect(m.bg).toBeTruthy()
      expect(m.fg).toBeTruthy()
    }
  })
  it('situação desconhecida não quebra', () => {
    expect(situacaoBcaMeta('XYZ').label).toBe('XYZ')
    expect(situacaoLocalMeta('XYZ').label).toBe('XYZ')
  })
  it('SEM_ETAPA não é etapa em curso', () => {
    expect(temEtapaEmCurso('SEM_ETAPA')).toBe(false)
    expect(temEtapaEmCurso(null)).toBe(false)
    expect(temEtapaEmCurso('MINUTA')).toBe(true)
  })
})

describe('eventoDoHistorico', () => {
  const h = (statusAnterior, statusNovo, descricao = '') => ({ statusAnterior, statusNovo, descricao })

  it('etapa local mostra o rótulo da etapa', () => {
    expect(eventoDoHistorico(h('MINUTA', 'EM_REVISAO')).titulo).toBe('Em Revisão')
  })
  it('primeira publicação', () => {
    const e = eventoDoHistorico(h('EM_PUBLICACAO', 'SEM_ETAPA', 'EM_PUBLICACAO → SEM_ETAPA (situação BCA: NAO_PUBLICADO → PUBLICADO)'))
    expect(e.titulo).toBe('Publicado')
  })
  it('revogação', () => {
    const e = eventoDoHistorico(h('EM_REVOGACAO', 'SEM_ETAPA', 'EM_REVOGACAO → SEM_ETAPA (situação BCA: PUBLICADO → REVOGADO)'))
    expect(e.titulo).toBe('Revogado')
  })
  it('alteração publicada não muda a situação BCA', () => {
    expect(eventoDoHistorico(h('EM_PUBLICACAO', 'SEM_ETAPA', 'EM_PUBLICACAO → SEM_ETAPA')).titulo).toBe('Alteração publicada')
  })
  it('alteração cancelada', () => {
    expect(eventoDoHistorico(h('EM_ALTERACAO', 'SEM_ETAPA', 'EM_ALTERACAO → SEM_ETAPA')).titulo).toBe('Alteração cancelada')
  })
  it('análise de revogação devolvida', () => {
    expect(eventoDoHistorico(h('ANALISE_REVOGACAO', 'SEM_ETAPA')).titulo).toBe('Revogação devolvida')
  })
  it('registro legado (antes da separação em BCA/Local) migrado de PUBLICADO', () => {
    expect(eventoDoHistorico(h('EM_PUBLICACAO', 'SEM_ETAPA', 'EM_PUBLICACAO → PUBLICADO')).titulo).toBe('Publicado')
  })
})

describe('exibeSeloRevogado (revogação total: selo, nunca tachado)', () => {
  it('documento revogado leva o selo', () => {
    expect(exibeSeloRevogado(doc('REVOGADO', 'SEM_ETAPA'))).toBe(true)
  })
  it('a revogação aprovada, ainda em tramitação, leva o selo', () => {
    expect(exibeSeloRevogado(doc('PUBLICADO', 'EM_REVOGACAO'))).toBe(true)
  })
  it('publicado, em análise de revogação, em alteração ou não publicado não leva', () => {
    expect(exibeSeloRevogado(doc('PUBLICADO', 'SEM_ETAPA'))).toBe(false)
    expect(exibeSeloRevogado(doc('PUBLICADO', 'ANALISE_REVOGACAO'))).toBe(false)
    expect(exibeSeloRevogado(doc('PUBLICADO', 'EM_ALTERACAO'))).toBe(false)
    expect(exibeSeloRevogado(doc('NAO_PUBLICADO', 'MINUTA'))).toBe(false)
    expect(exibeSeloRevogado(null)).toBe(false)
  })
})

describe('exibePortaria (só depois da 1ª publicação)', () => {
  it('documento não publicado não exibe a portaria, em nenhuma etapa', () => {
    for (const local of ['RASCUNHO', 'MINUTA', 'EM_REVISAO', 'EM_PUBLICACAO', 'CANCELADO']) {
      expect(exibePortaria(doc('NAO_PUBLICADO', local))).toBe(false)
    }
  })
  it('publicado ou revogado exibe, inclusive durante uma alteração (a portaria inicial é perene)', () => {
    expect(exibePortaria(doc('PUBLICADO', 'SEM_ETAPA'))).toBe(true)
    expect(exibePortaria(doc('PUBLICADO', 'EM_ALTERACAO'))).toBe(true)
    expect(exibePortaria(doc('REVOGADO', 'SEM_ETAPA'))).toBe(true)
  })
  it('sem documento ou sem situação não exibe', () => {
    expect(exibePortaria(null)).toBe(false)
    expect(exibePortaria({})).toBe(false)
  })
})
