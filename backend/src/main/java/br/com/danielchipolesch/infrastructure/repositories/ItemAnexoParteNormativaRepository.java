package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemAnexoParteNormativaRepository extends JpaRepository<ItemAnexoParteNormativa, Long> {

    @Query("SELECT i FROM ItemAnexoParteNormativa i WHERE i.documento.id = :documentoId AND i.parent IS NULL")
    List<ItemAnexoParteNormativa> findRootItemsByDocumentoId(@Param("documentoId") Long documentoId);

    List<ItemAnexoParteNormativa> findByParent(ItemAnexoParteNormativa item);
}
