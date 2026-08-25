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

    // Renumera pelo RANK relativo (não multiplica o valor armazenado) para que ciclos
    // repetidos de EM_ALTERACAO não acumulem fatores de 100 e estourem o INTEGER.
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE t_item_parte_final t
            SET nr_ordem = ranked.rn * 100
            FROM (
                SELECT id_item, ROW_NUMBER() OVER (ORDER BY nr_ordem) AS rn
                FROM t_item_parte_final
                WHERE documento_id = :documentoId AND nr_ordem IS NOT NULL
            ) ranked
            WHERE t.id_item = ranked.id_item
            """, nativeQuery = true)
    void respacarElementOrders(@Param("documentoId") Long documentoId);
}
