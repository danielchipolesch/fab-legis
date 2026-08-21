package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoEdicaoAtiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoEdicaoAtivaRepository extends JpaRepository<DocumentoEdicaoAtiva, Long> {
    Optional<DocumentoEdicaoAtiva> findByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    List<DocumentoEdicaoAtiva> findByDocumentoIdAndUltimoHeartbeatAfter(Long documentoId, Timestamp limite);
}
