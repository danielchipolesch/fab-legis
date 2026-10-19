// Métricas para o Prometheus (o equivalente, neste serviço Node, do /actuator/prometheus do backend). Ficam numa porta própria
// (METRICS_PORT, padrão 9464) que o Docker Compose NÃO publica e o Nginx NÃO repassa: só a rede interna (o Prometheus) as alcança.
// Além das métricas padrão do Node (CPU, memória, atraso do event loop, GC), as do que importa para a colaboração: conexões e salas
// abertas, autenticações, e o tempo e o resultado de carregar e gravar cada elemento no backend.
import { createServer } from 'node:http'
import client from 'prom-client'

const registro = new client.Registry()
client.collectDefaultMetrics({ register: registro, prefix: 'collab_node_' })

export const autenticacoes = new client.Counter({
  name: 'collab_autenticacoes_total', help: 'Conexões autenticadas ou recusadas', labelNames: ['resultado'], registers: [registro],
})
export const persistencias = new client.Counter({
  name: 'collab_persistencias_total', help: 'Gravações de elemento no backend', labelNames: ['resultado'], registers: [registro],
})
export const duracaoDaPersistencia = new client.Histogram({
  name: 'collab_persistencia_segundos', help: 'Tempo da gravação (PATCH) de um elemento no backend',
  buckets: [0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5], registers: [registro],
})
export const duracaoDoCarregamento = new client.Histogram({
  name: 'collab_carregamento_segundos', help: 'Tempo de carregar um elemento do backend ao abrir a sala',
  buckets: [0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5], registers: [registro],
})

// `contagens` devolve { conexoes, salas } na hora da raspagem (o Hocuspocus é quem sabe).
export function publicarMetricas(porta, contagens) {
  new client.Gauge({ name: 'collab_conexoes_abertas', help: 'Conexões WebSocket abertas', registers: [registro], collect() { this.set(contagens().conexoes) } })
  new client.Gauge({ name: 'collab_salas_abertas', help: 'Salas (elementos) com o documento em memória', registers: [registro], collect() { this.set(contagens().salas) } })
  createServer(async (req, res) => {
    if (req.url !== '/metrics') { res.writeHead(404).end(); return }
    try {
      const corpo = await registro.metrics()
      res.writeHead(200, { 'Content-Type': registro.contentType }).end(corpo)
    } catch (erro) {
      console.error('[metricas] Falha ao montar as métricas:', erro)
      res.writeHead(500).end()
    }
  }).listen(porta)
}
