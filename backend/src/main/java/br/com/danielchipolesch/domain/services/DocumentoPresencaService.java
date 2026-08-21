package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.usuarioDtos.PresencaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoEdicaoAtiva;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoEdicaoAtivaRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.security.AutenticacaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

// Presença "melhor esforço": o frontend chama registrarHeartbeatEListarOutros
// a cada ~10s enquanto o editor de um documento está aberto (ver
// DocumentEditorPage.vue). Uma janela de ATIVIDADE_SEGUNDOS sem heartbeat
// novo já basta para considerar que a pessoa não está mais editando -- não
// há job de limpeza, a linha antiga só é ignorada na consulta seguinte.
//
// Isso resolve só a PERMISSÃO de coedição (avisar "fulano também está
// editando"); não impede duas pessoas de salvar por cima uma da outra --
// isso é responsabilidade de DocumentoConcorrenciaService (checagem de
// versão), uma peça deliberadamente separada.
@Service
public class DocumentoPresencaService {

    private static final long ATIVIDADE_SEGUNDOS = 20;

    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private DocumentoEdicaoAtivaRepository edicaoAtivaRepository;

    @Transactional
    public List<PresencaResponseDto> registrarHeartbeatEListarOutros(Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado."));
        Usuario usuarioAtual = AutenticacaoUtil.usuarioAtual();

        var existente = edicaoAtivaRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioAtual.getId());
        var registro = existente.orElseGet(DocumentoEdicaoAtiva::new);
        registro.setDocumento(documento);
        registro.setUsuario(usuarioAtual);
        registro.setUltimoHeartbeat(Timestamp.from(Instant.now()));
        edicaoAtivaRepository.save(registro);

        Timestamp limite = Timestamp.from(Instant.now().minusSeconds(ATIVIDADE_SEGUNDOS));
        return edicaoAtivaRepository.findByDocumentoIdAndUltimoHeartbeatAfter(documentoId, limite).stream()
                .filter(e -> !e.getUsuario().getId().equals(usuarioAtual.getId()))
                .map(e -> new PresencaResponseDto(e.getUsuario().getId(), e.getUsuario().getNome()))
                .toList();
    }
}
