// Avisos ao vivo (SSE): o backend empurra, pela conexão de notificações de cada pessoa (GET /v1/notificacoes/stream), eventos que dizem "mudou" --
// sem o dado -- e o frontend lê de novo na API e se atualiza sem recarregar a página (docs/autenticacao.md, "Atualização ao vivo"):
//   documento    um documento foi criado, mudou de etapa ou foi excluído  -> { id, situacaoLocal, situacaoBca }  (situacaoLocal nula = excluído)
//   acesso       o acesso da pessoa mudou (papéis, OM, ativação)         -> { ativo }
//   habilitacao  mudou o que a OM da pessoa pode criar                   -> {}
// Funções puras (sem Vue nem DOM), para testar; quem as usa é o AppTopBar.vue.

export const EVENTOS_AO_VIVO = ['documento', 'acesso', 'habilitacao']

const JANELA_PADRAO_MS = 400

// Liga um handler por tipo de evento num EventSource (ou parecido: precisa de addEventListener). `data` chega como JSON; JSON inválido ou vazio
// vira {} (o aviso não carrega nada de que dependa a correção).
export function ouvirAvisosAoVivo(eventSource, { aoDocumento, aoAcesso, aoHabilitacao }) {
  const ligar = (nome, tratar) => {
    if (!tratar) return
    eventSource.addEventListener(nome, (evento) => {
      let dados = {}
      try { dados = evento?.data ? JSON.parse(evento.data) : {} } catch { /* aviso sem dado útil */ }
      tratar(dados ?? {})
    })
  }
  ligar('documento', aoDocumento)
  ligar('acesso', aoAcesso)
  ligar('habilitacao', aoHabilitacao)
}

// Junta uma rajada de avisos numa chamada só, depois que ela se acalma: uma etapa que muda de vez em quando é rara, mas uma ação em lote
// (ou várias pessoas agindo juntas) manda vários avisos em seguida, e cada um refaria as listas inteiras. `agendar`/`cancelar` são injetáveis
// para o teste.
export function juntarRajada(funcao, esperaMs = JANELA_PADRAO_MS, agendar = setTimeout, cancelar = clearTimeout) {
  let espera = null
  const chamada = (...args) => {
    if (espera != null) cancelar(espera)
    espera = agendar(() => { espera = null; funcao(...args) }, esperaMs)
  }
  chamada.cancelar = () => { if (espera != null) { cancelar(espera); espera = null } }
  return chamada
}

// O que fazer quando o acesso da própria pessoa mudou: conta desativada encerra a sessão; senão renova o token (as claims dele são a fonte dos
// papéis na tela) e reavalia a página aberta.
export function acaoDeAcesso(aviso) {
  return aviso?.ativo === false ? 'encerrar' : 'renovar'
}

// O aviso de documento é sobre o documento que a pessoa tem aberto (no editor ou no visualizador)?
export function ehODocumentoAberto(aviso, idAberto) {
  return aviso?.id != null && idAberto != null && String(aviso.id) === String(idAberto)
}

// O documento deixou de existir (excluído)?
export function documentoExcluido(aviso) {
  return aviso?.id != null && (aviso.situacaoLocal == null)
}
