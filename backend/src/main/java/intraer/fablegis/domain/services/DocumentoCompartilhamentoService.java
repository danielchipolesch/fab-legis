package intraer.fablegis.domain.services;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.dtos.usuarioDtos.CompartilharDocumentoRequestDto;
import intraer.fablegis.application.dtos.usuarioDtos.CompartilhamentoResponseDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.DocumentoCompartilhamento;
import intraer.fablegis.domain.entities.notificacao.TipoNotificacaoEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.RegraDeNegocioException;
import intraer.fablegis.domain.handlers.exceptions.ResourceAlreadyExistsException;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.util.CpfValidator;
import intraer.fablegis.infrastructure.notificacao.AvisosAoVivo;
import intraer.fablegis.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import intraer.fablegis.infrastructure.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

// Autoria do documento (docs/papeis-e-permissoes.md): uma lista só de autores, todos iguais -- quem criou o documento entra nela na criação
// (ver DocumentoService) e quem é acrescentado depois é autor como ele, sem distinção. Quem tem a autoria edita (nas etapas em que o texto
// muda), envia para aprovação e atribui a autoria a outras pessoas. Quem desvincula um autor é o Administrador da OM do documento, ou o próprio
// autor ao se desvincular, depois de sair da OM do documento (as permissões ficam em DocumentoAcessoService; aqui, a regra que vale para
// todos: o documento nunca fica sem autor).
@Service
@RequiredArgsConstructor
public class DocumentoCompartilhamentoService {

    static final String UNICO_ELABORADOR =
            "Não é possível desvincular: esta é a única pessoa atribuída como elaboradora do documento. Atribua a autoria a outra pessoa antes.";

    private final DocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoCompartilhamentoRepository compartilhamentoRepository;
    private final NotificacaoService notificacaoService;
    private final AvisosAoVivo avisosAoVivo;

    public List<CompartilhamentoResponseDto> listar(Long documentoId) {
        return compartilhamentoRepository.findByDocumentoId(documentoId).stream()
                .map(CompartilhamentoResponseDto::from)
                .toList();
    }

    // Batch (1 query pra página inteira) -- usado por DocumentoController.getAll
    // pra marcar, na listagem, de quais documentos o usuário logado é autor
    // (ver DocumentoResponseSemAnexoTextualDto.ehAutorOuCoautor).
    public Set<Long> listarIdsCompartilhadosComUsuario(Long usuarioId, Collection<Long> documentoIds) {
        if (documentoIds.isEmpty()) return Set.of();
        return Set.copyOf(compartilhamentoRepository.findDocumentoIdsCompartilhadosComUsuario(usuarioId, documentoIds));
    }

    // Atribui a autoria a quem o CPF indica. `ator` é quem atribui: um autor, ou o Administrador da OM do documento (a permissão já foi
    // conferida por DocumentoAcessoService) -- este pode atribuir a autoria a si mesmo.
    @Transactional
    public CompartilhamentoResponseDto compartilhar(Long documentoId, CompartilharDocumentoRequestDto request, Usuario ator) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado."));

        String cpf = CpfValidator.onlyDigits(request.cpf());
        Usuario usuario = usuarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum usuário cadastrado com esse CPF."));

        if (usuario.isSistema()) {
            throw new ResourceNotFoundException("Nenhum usuário cadastrado com esse CPF.");
        }
        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Esta conta está desativada: não pode ser atribuída como autora.");
        }
        if (compartilhamentoRepository.existsByDocumentoIdAndUsuarioId(documentoId, usuario.getId())) {
            throw new ResourceAlreadyExistsException("Este usuário já é autor do documento.");
        }

        var compartilhamento = new DocumentoCompartilhamento();
        compartilhamento.setDocumento(documento);
        compartilhamento.setUsuario(usuario);
        var salvo = compartilhamentoRepository.save(compartilhamento);

        if (ator == null || !ator.getId().equals(usuario.getId())) {
            String descricao = documento.getIdentificacao();
            String quem = ator == null ? "Alguém" : ator.getNome();
            notificacaoService.criar(usuario, TipoNotificacaoEnum.DOCUMENTO_COMPARTILHADO,
                    quem + " atribuiu a você a autoria do documento " + descricao + ".",
                    documento.getId(), descricao);
        }
        avisosAoVivo.documentoMudou(documento);
        return CompartilhamentoResponseDto.from(salvo);
    }

    // Desvincula `usuarioId` da autoria -- ou o Administrador da OM desvinculando um autor, ou o autor desvinculando-se (DocumentoAcessoService
    // decide quem pode). O documento nunca fica sem autor.
    // Devolve o nome de quem deixou de ser autor (para o registro de auditoria).
    @Transactional
    public String desvincular(Long documentoId, Long usuarioId) {
        var autoria = compartilhamentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Autoria não encontrada."));
        if (compartilhamentoRepository.countByDocumentoId(documentoId) <= 1) {
            throw new RegraDeNegocioException(UNICO_ELABORADOR);
        }
        String nome = autoria.getUsuario().getNome();
        compartilhamentoRepository.delete(autoria);
        documentoRepository.findById(documentoId).ifPresent(avisosAoVivo::documentoMudou);
        return nome;
    }
}
