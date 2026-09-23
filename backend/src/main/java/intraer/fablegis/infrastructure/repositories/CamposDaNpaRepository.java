package intraer.fablegis.infrastructure.repositories;

import intraer.fablegis.domain.entities.estruturaDocumento.CamposDaNpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CamposDaNpaRepository extends JpaRepository<CamposDaNpa, Long> {
}
