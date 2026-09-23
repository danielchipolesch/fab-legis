package intraer.fablegis.infrastructure.repositories;

import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizacaoMilitarRepository extends JpaRepository<OrganizacaoMilitar, Long> {
    Optional<OrganizacaoMilitar> findBySigla(String sigla);
}
