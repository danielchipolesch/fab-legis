package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

// JpaSpecificationExecutor -- mesmo padrão de LogAuditoriaRepository: filtros dinâmicos
// (aba/busca/espécie/situação, ver DocumentoSpecifications) montados só para o que foi
// realmente informado, sem "(:param IS NULL OR campo = :param)" (esse padrão falha no
// driver do Postgres quando o parâmetro é de um tipo que ele não consegue inferir
// isolado, comparado só a IS NULL).
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {

    List<Documento> findByEspecieNormativaAndAssuntoBasico(EspecieNormativa especieNormativa, AssuntoBasico assuntoBasico);
//    List<Documento> findByEspecieNormativaAndAssuntoBasico(
//            @Param("documentationTypeName") String documentationTypeAcronym,
//            @Param("basicSubjectName") String basicSubjectName
//    );
}
