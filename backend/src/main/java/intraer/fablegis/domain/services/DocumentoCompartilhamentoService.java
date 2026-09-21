package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.usuarioDtos.CompartilharDocumentoRequestDto;
import intraer.fablegis.application.dtos.usuarioDtos.CompartilhamentoResponseDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.DocumentoCompartilhamento;
import intraer.fablegis.domain.entities.notificacao.TipoNotificacaoEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.ResourceAlreadyExistsException;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.util.CpfValidator;
import intraer.fablegis.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class DocumentoCompartilhamentoService {

    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DocumentoCompartilhamentoRepository compartilhamentoRepository;
    @Autowired private NotificacaoService notificacaoService;

    public List<CompartilhamentoResponseDto> listar(Long documentoId) {
        return compartilhamentoRepository.findByDocumentoId(documentoId).stream()
                .map(CompartilhamentoResponseDto::from)
                .toList();
    }

    // Batch (1 query pra página inteira) -- usado por DocumentoController.getAll
    // pra marcar, na listagem, de quais documentos o usuário logado é coautor
    // (ver DocumentoResponseSemAnexoTextualDto.ehAutorOuCoautor).
    public Set<Long> listarIdsCompartilhadosComUsuario(Long usuarioId, Collection<Long> documentoIds) {
        if (documentoIds.isEmpty()) return Set.of();
        return Set.copyOf(compartilhamentoRepository.findDocumentoIdsCompartilhadosComUsuario(usuarioId, documentoIds));
    }

    @Transactional
    public CompartilhamentoResponseDto compartilhar(Long documentoId, CompartilharDocumentoRequestDto request) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado."));

        String cpf = CpfValidator.onlyDigits(request.cpf());
        Usuario usuario = usuarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum usuário cadastrado com esse CPF."));

        if (usuario.isSistema()) {
            throw new ResourceNotFoundException("Nenhum usuário cadastrado com esse CPF.");
        }
        if (documento.getAutor().getId().equals(usuario.getId())) {
            throw new ResourceAlreadyExistsException("Este usuário já é o autor do documento.");
        }
        if (compartilhamentoRepository.existsByDocumentoIdAndUsuarioId(documentoId, usuario.getId())) {
            throw new ResourceAlreadyExistsException("Documento já compartilhado com este usuário.");
        }

        var compartilhamento = new DocumentoCompartilhamento();
        compartilhamento.setDocumento(documento);
        compartilhamento.setUsuario(usuario);
        var salvo = compartilhamentoRepository.save(compartilhamento);

        String descricao = documento.getIdentificacao();
        notificacaoService.criar(usuario, TipoNotificacaoEnum.DOCUMENTO_COMPARTILHADO,
                documento.getAutor().getNome() + " compartilhou o documento " + descricao + " com você.",
                documento.getId(), descricao);

        return CompartilhamentoResponseDto.from(salvo);
    }

    @Transactional
    public void remover(Long documentoId, Long usuarioId) {
        var compartilhamento = compartilhamentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Compartilhamento não encontrado."));
        compartilhamentoRepository.delete(compartilhamento);
    }
}
