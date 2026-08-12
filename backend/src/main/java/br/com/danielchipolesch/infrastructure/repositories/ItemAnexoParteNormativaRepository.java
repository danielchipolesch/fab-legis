package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemAnexoParteNormativaRepository extends JpaRepository<ItemAnexoParteNormativa, Long> {

    @Query("SELECT i FROM ItemAnexoParteNormativa i WHERE i.documento.id = :documentoId AND i.parent IS NULL ORDER BY i.elementOrder ASC")
    List<ItemAnexoParteNormativa> findRootItemsByDocumentoId(@Param("documentoId") Long documentoId);

    List<ItemAnexoParteNormativa> findByParentOrderByElementOrderAsc(ItemAnexoParteNormativa parent);

    @Modifying
    @Transactional
    @Query(value = "UPDATE t_item_parte_normativa SET parent_id = NULL WHERE documento_id = :documentoId", nativeQuery = true)
    void nullifyParentsForDocument(@Param("documentoId") Long documentoId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ItemAnexoParteNormativa i WHERE i.documento.id = :documentoId")
    void deleteAllByDocumentoId(@Param("documentoId") Long documentoId);

    @Modifying
    @Transactional
    @Query("UPDATE ItemAnexoParteNormativa i SET i.elementOrder = i.elementOrder * 100 WHERE i.documento.id = :documentoId AND i.elementOrder IS NOT NULL")
    void respacarElementOrders(@Param("documentoId") Long documentoId);
}
