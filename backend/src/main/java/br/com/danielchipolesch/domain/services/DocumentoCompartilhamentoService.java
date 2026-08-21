package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.usuarioDtos.CompartilharDocumentoRequestDto;
import br.com.danielchipolesch.application.dtos.usuarioDtos.CompartilhamentoResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoCompartilhamento;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceAlreadyExistsException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.util.CpfValidator;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentoCompartilhamentoService {

    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DocumentoCompartilhamentoRepository compartilhamentoRepository;

    public List<CompartilhamentoResponseDto> listar(Long documentoId) {
        return compartilhamentoRepository.findByDocumentoId(documentoId).stream()
                .map(CompartilhamentoResponseDto::from)
                .toList();
    }

    @Transactional
    public CompartilhamentoResponseDto compartilhar(Long documentoId, CompartilharDocumentoRequestDto request) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado."));

        String cpf = CpfValidator.onlyDigits(request.getCpf());
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
        return CompartilhamentoResponseDto.from(salvo);
    }

    @Transactional
    public void remover(Long documentoId, Long usuarioId) {
        var compartilhamento = compartilhamentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Compartilhamento não encontrado."));
        compartilhamentoRepository.delete(compartilhamento);
    }
}
