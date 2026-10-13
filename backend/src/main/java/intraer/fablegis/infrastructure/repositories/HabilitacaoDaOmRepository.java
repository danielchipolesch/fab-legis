package intraer.fablegis.infrastructure.repositories;

import intraer.fablegis.domain.entities.usuario.HabilitacaoDaOm;
import intraer.fablegis.domain.entities.usuario.HabilitacaoDaOmId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HabilitacaoDaOmRepository extends JpaRepository<HabilitacaoDaOm, HabilitacaoDaOmId> {

    @Query("select h from HabilitacaoDaOm h where h.id.organizacaoMilitarId = :omId")
    List<HabilitacaoDaOm> findByOmId(@Param("omId") Long omId);

    @Query("select count(h) > 0 from HabilitacaoDaOm h where h.id.organizacaoMilitarId = :omId and h.id.especieNormativaId = :especieId")
    boolean existsByOmIdAndEspecieId(@Param("omId") Long omId, @Param("especieId") Long especieId);

    @Modifying
    @Query("delete from HabilitacaoDaOm h where h.id.organizacaoMilitarId = :omId")
    void deleteByOmId(@Param("omId") Long omId);

    @Modifying
    @Query("delete from HabilitacaoDaOm h where h.id.especieNormativaId = :especieId")
    void deleteByEspecieId(@Param("especieId") Long especieId);
}
