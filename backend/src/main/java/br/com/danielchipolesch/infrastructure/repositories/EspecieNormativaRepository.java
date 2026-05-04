package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EspecieNormativaRepository extends JpaRepository<EspecieNormativa, Long> {
    boolean existsBySigla(String siglaEspecieNormativa);
}
