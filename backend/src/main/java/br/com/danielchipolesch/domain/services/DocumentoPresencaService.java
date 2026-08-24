package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.infrastructure.notificacao.DocumentoPresencaEmitterRegistry;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.security.AutenticacaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// Presença "melhor esforço": o frontend abre esta conexão SSE enquanto o
// editor de um documento está aberto (ver DocumentEditorPage.vue) e cada
// mudança na lista de quem está conectado é empurrada ao vivo -- ver
// DocumentoPresencaEmitterRegistry, que também é quem detecta desconexão.
//
// Isso resolve só a PERCEPÇÃO de coedição (avisar "fulano também está
// editando"); não impede duas pessoas de salvar por cima uma da outra --
// isso é responsabilidade de DocumentoConcorrenciaService (checagem de
// versão), uma peça deliberadamente separada.
@Service
public class DocumentoPresencaService {

    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private DocumentoPresencaEmitterRegistry emitterRegistry;

    public SseEmitter conectar(Long documentoId) {
        if (!documentoRepository.existsById(documentoId)) {
            throw new ResourceNotFoundException("Documento não encontrado.");
        }
        Usuario usuarioAtual = AutenticacaoUtil.usuarioAtual();
        return emitterRegistry.conectar(documentoId, usuarioAtual.getId(), usuarioAtual.getNome());
    }
}
