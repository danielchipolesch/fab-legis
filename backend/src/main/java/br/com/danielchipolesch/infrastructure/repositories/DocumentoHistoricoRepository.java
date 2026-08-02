package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoHistoricoRepository extends JpaRepository<DocumentoHistorico, Long> {

    List<DocumentoHistorico> findByDocumentoIdOrderByDtRegistroDesc(Long documentoId);

    void deleteAllByDocumentoId(Long documentoId);
}
