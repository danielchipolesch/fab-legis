package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemPartePreliminar;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemPartePreliminarRepository extends JpaRepository<ItemPartePreliminar, Long> {

    @Query("SELECT i FROM ItemPartePreliminar i WHERE i.documento.id = :documentoId ORDER BY i.elementOrder ASC")
    List<ItemPartePreliminar> findByDocumentoIdOrderByElementOrderAsc(@Param("documentoId") Long documentoId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ItemPartePreliminar i WHERE i.documento.id = :documentoId")
    void deleteAllByDocumentoId(@Param("documentoId") Long documentoId);

    @Modifying
    @Transactional
    @Query("UPDATE ItemPartePreliminar i SET i.elementOrder = i.elementOrder * 100 WHERE i.documento.id = :documentoId AND i.elementOrder IS NOT NULL")
    void respacarElementOrders(@Param("documentoId") Long documentoId);
}
