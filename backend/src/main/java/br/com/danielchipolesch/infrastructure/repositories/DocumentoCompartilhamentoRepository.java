package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoCompartilhamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoCompartilhamentoRepository extends JpaRepository<DocumentoCompartilhamento, Long> {
    List<DocumentoCompartilhamento> findByDocumentoId(Long documentoId);
    Optional<DocumentoCompartilhamento> findByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    boolean existsByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    void deleteByDocumentoId(Long documentoId);
}
