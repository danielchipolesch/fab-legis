package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Portaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortariaRepository extends JpaRepository<Portaria, Long> {
//    RegulatoryAct findByDocumentId(Document document);
}
