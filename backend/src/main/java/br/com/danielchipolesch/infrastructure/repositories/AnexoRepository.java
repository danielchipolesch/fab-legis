package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Anexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    List<Anexo> findByDocumentoIdOrderByOrdemAsc(Long documentoId);

    Optional<Anexo> findByIdAndDocumentoId(Long id, Long documentoId);

    @Query("SELECT COALESCE(MAX(a.ordem), 0) FROM Anexo a WHERE a.documento.id = :documentoId")
    int findMaxOrdemByDocumentoId(Long documentoId);

    void deleteAllByDocumentoId(Long documentoId);
}
