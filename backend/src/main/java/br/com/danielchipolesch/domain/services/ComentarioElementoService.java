package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.comentarioDtos.ComentarioElementoCreateRequestDto;
import br.com.danielchipolesch.application.dtos.comentarioDtos.ComentarioElementoResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ComentarioElemento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import br.com.danielchipolesch.domain.entities.notificacao.TipoNotificacaoEnum;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.infrastructure.repositories.*;
import br.com.danielchipolesch.infrastructure.security.AutenticacaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ComentarioElementoService {

    @Autowired private ComentarioElementoRepository comentarioRepository;
    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private DocumentoCompartilhamentoRepository compartilhamentoRepository;
    @Autowired private ItemAnexoParteNormativaRepository itemParteNormativaRepository;
    @Autowired private ItemPartePreliminarRepository itemPartePreliminarRepository;
    @Autowired private ItemParteFinalRepository itemParteFinalRepository;
    @Autowired private NotificacaoService notificacaoService;

    public List<ComentarioElementoResponseDto> listar(Long documentoId) {
        return comentarioRepository.findByDocumentoIdOrderByDtCriacaoAsc(documentoId).stream()
                .map(ComentarioElementoResponseDto::from)
                .toList();
    }

    @Transactional
    public ComentarioElementoResponseDto criar(Long documentoId, ComentarioElementoCreateRequestDto request) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado."));

        ComentarioElemento parent = null;
        if (request.parentId() != null) {
            parent = comentarioRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comentário original não encontrado."));
            if (!parent.getDocumento().getId().equals(documentoId)) {
                throw new ResourceNotFoundException("Comentário original não encontrado.");
            }
        } else if (!elementoPertenceAoDocumento(request.elementoId(), request.secao(), documentoId)) {
            throw new ResourceNotFoundException("Elemento não encontrado neste documento.");
        }

        Usuario autor = AutenticacaoUtil.usuarioAtual();

        var comentario = new ComentarioElemento();
        comentario.setDocumento(documento);
        // Resposta herda o elemento/seção do comentário raiz -- quem responde não
        // escolhe de novo onde a thread está ancorada.
        comentario.setElementoId(parent != null ? parent.getElementoId() : request.elementoId());
        comentario.setSecao(parent != null ? parent.getSecao() : request.secao());
        comentario.setAutor(autor);
        comentario.setTexto(request.texto());
        comentario.setParent(parent);
        var salvo = comentarioRepository.save(comentario);

        notificarParticipantes(documento, autor, salvo);

        return ComentarioElementoResponseDto.from(salvo);
    }

    @Transactional
    public ComentarioElementoResponseDto marcarResolvido(Long documentoId, Long comentarioId, boolean resolvido) {
        var comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentário não encontrado."));
        if (!comentario.getDocumento().getId().equals(documentoId)) {
            throw new ResourceNotFoundException("Comentário não encontrado.");
        }
        comentario.setResolvido(resolvido);
        comentario.setDtResolucao(resolvido ? Timestamp.from(Instant.now()) : null);
        return ComentarioElementoResponseDto.from(comentarioRepository.save(comentario));
    }

    private boolean elementoPertenceAoDocumento(Long elementoId, SecaoDocumentoEnum secao, Long documentoId) {
        return switch (secao) {
            case PARTE_PRELIMINAR -> itemPartePreliminarRepository.existsByIdAndDocumentoId(elementoId, documentoId);
            case PARTE_NORMATIVA -> itemParteNormativaRepository.existsByIdAndDocumentoId(elementoId, documentoId);
            case PARTE_FINAL -> itemParteFinalRepository.existsByIdAndDocumentoId(elementoId, documentoId);
        };
    }

    // Autor, coautores e quem estiver atribuído como revisor/publicador -- o mesmo
    // conjunto que pode comentar (ver DocumentoAcessoService.podeComentar) --, menos
    // quem acabou de comentar (não notifica a própria pessoa).
    private void notificarParticipantes(Documento documento, Usuario autorComentario, ComentarioElemento comentario) {
        Set<Usuario> participantes = new LinkedHashSet<>();
        participantes.add(documento.getAutor());
        if (documento.getRevisorAtribuido() != null) participantes.add(documento.getRevisorAtribuido());
        if (documento.getPublicadorAtribuido() != null) participantes.add(documento.getPublicadorAtribuido());
        compartilhamentoRepository.findByDocumentoId(documento.getId())
                .forEach(c -> participantes.add(c.getUsuario()));
        participantes.remove(autorComentario);

        String descricao = String.format("%s %s-%d",
                documento.getEspecieNormativa().getSigla(),
                documento.getAssuntoBasico().getCodigo(),
                documento.getNumeroSecundario());
        String mensagem = autorComentario.getNome() + " comentou no documento " + descricao + ".";

        for (Usuario destinatario : participantes) {
            notificacaoService.criar(destinatario, TipoNotificacaoEnum.COMENTARIO_NOVO, mensagem,
                    documento.getId(), descricao, comentario.getElementoId(), comentario.getSecao());
        }
    }
}
