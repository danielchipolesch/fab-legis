package intraer.fablegis.infrastructure.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Liga as tarefas agendadas (@Scheduled). Hoje só há uma: a limpeza do cache de PDF/HTML em tramitação
// (DocumentoRenderCacheService.removerExpirados).
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
