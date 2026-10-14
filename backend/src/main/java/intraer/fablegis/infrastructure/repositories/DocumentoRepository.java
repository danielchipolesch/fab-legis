package intraer.fablegis.infrastructure.repositories;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.numeracaoDocumento.AssuntoBasico;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// JpaSpecificationExecutor -- mesmo padrão de LogAuditoriaRepository: filtros dinâmicos
// (aba/busca/espécie/situação, ver DocumentoSpecifications) montados só para o que foi
// realmente informado, sem "(:param IS NULL OR campo = :param)" (esse padrão falha no
// driver do Postgres quando o parâmetro é de um tipo que ele não consegue inferir
// isolado, comparado só a IS NULL).
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {

    List<Documento> findByEspecieNormativaAndAssuntoBasico(EspecieNormativa especieNormativa, AssuntoBasico assuntoBasico);

    // Trava (até o fim da transação) a numeração de uma espécie + assunto: quem cria um documento a pega antes de escolher o menor
    // número livre, e a outra criação simultânea espera, em vez de escolher o mesmo número. É uma trava do PostgreSQL, então vale
    // também entre réplicas do backend (CriacaoDeEspecieConvencional). O select de fora existe só para a consulta devolver algo.
    @Query(value = "select 1 from (select pg_advisory_xact_lock(:chave)) trava", nativeQuery = true)
    int travarNumeracao(@Param("chave") long chave);

    // "Substituir" (NPA): a NPA que substitui esta -- a mais recente que não foi cancelada; vazio se ninguém a substitui.
    Optional<Documento> findFirstByDocumentoSubstituidoIdAndSituacaoLocalNotOrderByIdDesc(Long id, SituacaoLocalEnum situacao);

    // Filas das telas de Revisão/Publicação (ver DocumentoService): os documentos de uma OM numa das etapas, ou de todas as OMs.
    List<Documento> findByOmIdAndSituacaoLocalIn(Long omId, Collection<SituacaoLocalEnum> status);
    List<Documento> findBySituacaoLocalIn(Collection<SituacaoLocalEnum> status);
//    List<Documento> findByEspecieNormativaAndAssuntoBasico(
//            @Param("documentationTypeName") String documentationTypeAcronym,
//            @Param("basicSubjectName") String basicSubjectName
//    );
}
