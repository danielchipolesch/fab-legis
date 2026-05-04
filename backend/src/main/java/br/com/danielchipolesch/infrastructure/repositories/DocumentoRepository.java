package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    List<Documento> findByEspecieNormativaAndAssuntoBasico(EspecieNormativa especieNormativa, AssuntoBasico assuntoBasico);
//    List<Documento> findByEspecieNormativaAndAssuntoBasico(
//            @Param("documentationTypeName") String documentationTypeAcronym,
//            @Param("basicSubjectName") String basicSubjectName
//    );
}
