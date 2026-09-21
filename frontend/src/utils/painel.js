// Regras dos cards do hub (tela inicial): a ação de cada linha e os detalhes do "Exibir detalhes". Funções puras sobre o
// documento no formato do frontend (backendParaFrontend), para poderem ser testadas (painel.test.js).
//
// Os três cards são por trabalho da pessoa, não por espécie -- cada linha se identifica pelo módulo dela (perfis/index.js):
//   em_andamento : meus documentos (autoria ou coautoria) ainda em trabalho
//   aguardando   : documentos atribuídos a mim como revisor ou publicador
//   publicados   : publicados e revogados, de todas as OMs
import { moduloDoDocumento } from '@/perfis/index.js'

export const CARTOES = ['em_andamento', 'aguardando', 'publicados']

const editar = (doc, query) => ({ name: 'documento-editar', params: { id: doc.id }, ...(query ? { query } : {}) })
const visualizar = (doc, query) => ({ name: 'documento-visualizar', params: { id: doc.id }, ...(query ? { query } : {}) })

// Quais selos a linha mostra. Nos cards de trabalho, a etapa em curso (situação local); e, se o documento já tem versão publicada
// em vigor, também o selo "Publicado" -- um publicado em alteração tem os dois, e é isso que o distingue de um rascunho. Um nunca
// publicado leva só a etapa ("Não publicado" seria ruído). No card de publicados, só a situação oficial (BCA/Boletim Interno).
export function selosDaLinha(cartao, doc) {
  if (cartao === 'publicados') return 'bca'
  return doc.situacao_bca === 'PUBLICADO' ? 'ambos' : 'local'
}

// Editar só quando dá: etapa de escrita e a pessoa é autora ou coautora (mesma regra de DocumentoAcessoService.podeEditar).
function podeEditar(doc) {
  return ['RASCUNHO', 'MINUTA', 'EM_ALTERACAO'].includes(doc.situacao_local) && doc.eh_autor_ou_coautor === true
}

// A ação da linha: o que a pessoa faz a seguir e para onde o link leva -- direto o documento ou a tela da ação
// (Revisão/Publicação), nunca uma ação executada no hub. `pendente` acende o aviso (⚠) de "há ação sua".
export function acaoDaLinha(cartao, doc) {
  if (cartao === 'publicados') return { rotulo: 'Visualizar', rota: visualizar(doc), pendente: false }

  if (cartao === 'aguardando') {
    switch (doc.situacao_local) {
      // Quem revisa lê (e pode editar) o texto: abre o documento, e o breadcrumb volta para a fila de Revisão.
      case 'EM_REVISAO':        return { rotulo: 'Revisar', rota: editar(doc, { origem: 'revisao' }), pendente: true }
      case 'ANALISE_REVOGACAO': return { rotulo: 'Analisar revogação', rota: visualizar(doc, { origem: 'revisao' }), pendente: true }
      // Publicar e revogar acontecem na tela de Publicação.
      case 'EM_PUBLICACAO':     return { rotulo: 'Publicar', rota: { name: 'publicacao' }, pendente: true }
      case 'EM_REVOGACAO':      return { rotulo: 'Revogar', rota: { name: 'publicacao' }, pendente: true }
    }
  }

  return podeEditar(doc)
    ? { rotulo: 'Editar', rota: editar(doc), pendente: false }
    : { rotulo: 'Acompanhar', rota: visualizar(doc), pendente: false }
}

const ESPERA_DE_VOCE = {
  EM_REVISAO: 'Revisar o documento e aprová-lo ou devolvê-lo',
  ANALISE_REVOGACAO: 'Analisar o pedido de revogação e aprová-lo ou devolvê-lo',
  EM_PUBLICACAO: 'Registrar a publicação do documento',
  EM_REVOGACAO: 'Registrar a revogação do documento',
}

// Com quem o documento está agora, conforme a etapa.
function comQuem(doc) {
  switch (doc.situacao_local) {
    case 'EM_REVISAO':
    case 'ANALISE_REVOGACAO':  return doc.revisor_atribuido_nome ?? 'Revisor ainda não escolhido'
    case 'EM_PUBLICACAO':
    case 'EM_REVOGACAO':       return doc.publicador_atribuido_nome ?? 'Publicador ainda não escolhido'
    default:                   return 'Autor e coautores'
  }
}

// O que já se sabe da publicação: quando foi publicado (e revogado) e as referências oficiais -- nas convencionais, portaria
// e BCA; na NPA, o Boletim Interno (que fica no mesmo campo de referência).
function linhasDePublicacao(doc, modulo, formatarData) {
  const linhas = []
  if (doc.data_publicacao) linhas.push({ rotulo: 'Publicado em', valor: formatarData(doc.data_publicacao) })
  if (doc.situacao_bca === 'REVOGADO' && doc.data_revogacao) {
    linhas.push({ rotulo: 'Revogado em', valor: formatarData(doc.data_revogacao) })
  }
  if (doc.portaria_referencia) linhas.push({ rotulo: 'Portaria', valor: doc.portaria_referencia })
  if (doc.bca_referencia) {
    linhas.push({ rotulo: modulo.rotuloDaSituacaoOficial === 'Situação BCA' ? 'BCA' : 'Boletim Interno', valor: doc.bca_referencia })
  }
  return linhas
}

// As linhas de informação do "Exibir detalhes" -- só leitura. coautores: nomes já formatados; formatarData: formata as datas.
// Primeiro o que identifica o documento (igual nos três cards); depois o que o card pede.
export function detalhesDoDocumento(cartao, doc, { coautores = [], formatarData = (d) => d } = {}) {
  const modulo = moduloDoDocumento(doc)
  const data = (d) => (d ? formatarData(d) : '—')

  const linhas = [
    { rotulo: 'Módulo', valor: modulo.nome },
    { rotulo: 'Espécie', valor: doc.especie ?? '—' },
    { rotulo: 'Código', valor: doc.codigo_documento ?? '—' },
    // Só as espécies convencionais têm assunto básico.
    ...(doc.assunto_basico ? [{ rotulo: 'Assunto Básico', valor: doc.assunto_basico }] : []),
    { rotulo: 'OM', valor: doc.om_nome ? (doc.om_sigla ? `${doc.om_nome} (${doc.om_sigla})` : doc.om_nome) : '—' },
    { rotulo: 'Autor', valor: doc.autor_nome ?? '—' },
    { rotulo: 'Coautores', valor: coautores.length ? coautores.join(', ') : 'Nenhum' },
    { rotulo: 'Criado em', valor: data(doc.data_criacao) },
  ]

  if (cartao === 'em_andamento') {
    linhas.push({ rotulo: 'Com', valor: comQuem(doc) })
  } else if (cartao === 'aguardando') {
    linhas.push({ rotulo: 'Se espera de você', valor: ESPERA_DE_VOCE[doc.situacao_local] ?? '—' })
  }
  linhas.push({ rotulo: 'Última alteração', valor: data(doc.data_alteracao) })
  linhas.push(...linhasDePublicacao(doc, modulo, formatarData))
  return linhas
}
