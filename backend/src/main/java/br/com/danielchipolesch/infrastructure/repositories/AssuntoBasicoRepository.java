package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssuntoBasicoRepository extends JpaRepository<AssuntoBasico, Long> {

    boolean existsByCodigo(String code);
    AssuntoBasico findByCodigo(String code);
}
