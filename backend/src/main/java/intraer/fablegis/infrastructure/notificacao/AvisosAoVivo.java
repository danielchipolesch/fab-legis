package intraer.fablegis.infrastructure.notificacao;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Avisos ao vivo (SSE) para o frontend se atualizar sem recarregar a página. Vão pela conexão de notificações de cada usuário
// (GET /v1/notificacoes/stream), cada um com o seu nome de evento, e SEMPRE depois do commit da transação que mudou o dado: um rollback
// não pode ter avisado ninguém. O evento não carrega o dado em si -- só diz "mudou"; quem recebe lê de novo na API, que é a fonte de verdade
// (e já aplica a permissão de cada pessoa).
//
//   documento   (para todos os conectados) um documento foi criado, mudou de situação ou foi excluído -- listas, filas, cards e o editor aberto
//               nele se atualizam. Vai para todos porque a leitura é aberta: as listas dos módulos mostram documentos de qualquer OM.
//   acesso      (só para a pessoa) o acesso dela mudou -- papéis, OM ou ativação --; o frontend renova o token (as claims dele são a fonte dos
//               papéis na tela) e reavalia a página aberta. O servidor, por sua vez, já decide pelo banco a cada requisição.
//   habilitacao (para quem é das OMs afetadas) mudou o que a OM pode criar -- ver HabilitacaoDeCriacaoService.
@Component
@RequiredArgsConstructor
public class AvisosAoVivo {

    public static final String EVENTO_DOCUMENTO = "documento";
    public static final String EVENTO_ACESSO = "acesso";
    public static final String EVENTO_HABILITACAO = "habilitacao";

    private final NotificacaoEmitterRegistry emitterRegistry;

    public void documentoMudou(Documento documento) {
        documentoMudou(documento.getId(), documento.getSituacaoLocal().name(), documento.getSituacaoBca().name());
    }

    // `situacaoLocal` nula = o documento deixou de existir (excluído).
    public void documentoMudou(Long documentoId, String situacaoLocal, String situacaoBca) {
        var dados = new java.util.LinkedHashMap<String, Object>();
        dados.put("id", documentoId);
        dados.put("situacaoLocal", situacaoLocal);
        dados.put("situacaoBca", situacaoBca);
        emitterRegistry.enviarATodosAposCommit(EVENTO_DOCUMENTO, dados);
    }

    // `ativo` falso: a conta foi desativada -- o frontend encerra a sessão em vez de tentar renovar.
    public void acessoMudou(Long usuarioId, boolean ativo) {
        emitterRegistry.enviarEventoAposCommit(List.of(usuarioId), EVENTO_ACESSO, Map.of("ativo", ativo));
    }

    public void habilitacaoMudou(java.util.Collection<Long> usuarioIds) {
        emitterRegistry.enviarEventoAposCommit(usuarioIds, EVENTO_HABILITACAO, Map.of());
    }
}
