package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.auditoria.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

// JpaSpecificationExecutor em vez de uma @Query com "(:param IS NULL OR
// campo = :param)" -- esse padrão, quando :param é Timestamp e chega nulo,
// faz o driver do Postgres falhar com "could not determine data type of
// parameter" (ele não consegue inferir o tipo de um bind isolado só
// comparado a IS NULL, sem nenhum outro contexto de tipo). Specification
// monta os predicados só para os filtros realmente informados -- ver
// LogAuditoriaService.filtrar -- então um filtro nulo nunca vira parâmetro.
@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long>, JpaSpecificationExecutor<LogAuditoria> {
}
