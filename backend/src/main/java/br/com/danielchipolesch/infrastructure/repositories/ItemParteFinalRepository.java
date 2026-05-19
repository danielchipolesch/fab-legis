package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemParteFinal;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemParteFinalRepository extends JpaRepository<ItemParteFinal, Long> {

    @Query("SELECT i FROM ItemParteFinal i WHERE i.documento.id = :documentoId ORDER BY i.elementOrder ASC")
    List<ItemParteFinal> findByDocumentoIdOrderByElementOrderAsc(@Param("documentoId") Long documentoId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ItemParteFinal i WHERE i.documento.id = :documentoId")
    void deleteAllByDocumentoId(@Param("documentoId") Long documentoId);
}
