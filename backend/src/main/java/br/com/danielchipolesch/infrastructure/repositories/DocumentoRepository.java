package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

// JpaSpecificationExecutor -- mesmo padrão de LogAuditoriaRepository: filtros dinâmicos
// (aba/busca/espécie/situação, ver DocumentoSpecifications) montados só para o que foi
// realmente informado, sem "(:param IS NULL OR campo = :param)" (esse padrão falha no
// driver do Postgres quando o parâmetro é de um tipo que ele não consegue inferir
// isolado, comparado só a IS NULL).
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {

    List<Documento> findByEspecieNormativaAndAssuntoBasico(EspecieNormativa especieNormativa, AssuntoBasico assuntoBasico);

    // Fila pessoal das telas de Revisão/Publicação (ver RevisaoPage.vue/
    // PublicacaoPage.vue) -- sem paginação de propósito: é o que está atribuído a UMA
    // pessoa, não o acervo inteiro, volume esperado é baixo.
    List<Documento> findByRevisorAtribuidoIdAndDocumentoStatusIn(Long revisorId, Collection<DocumentoStatusEnum> status);
    List<Documento> findByPublicadorAtribuidoIdAndDocumentoStatusIn(Long publicadorId, Collection<DocumentoStatusEnum> status);
//    List<Documento> findByEspecieNormativaAndAssuntoBasico(
//            @Param("documentationTypeName") String documentationTypeAcronym,
//            @Param("basicSubjectName") String basicSubjectName
//    );
}
