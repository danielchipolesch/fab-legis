package intraer.fablegis.infrastructure.repositories;

import intraer.fablegis.domain.entities.auditoria.LogDeAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

// Specification pelo mesmo motivo de LogAuditoriaRepository: só os filtros realmente informados viram predicado.
@Repository
public interface LogDeAcessoRepository extends JpaRepository<LogDeAcesso, Long>, JpaSpecificationExecutor<LogDeAcesso> {
}
