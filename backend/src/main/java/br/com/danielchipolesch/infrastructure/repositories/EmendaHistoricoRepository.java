package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.EmendaHistorico;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmendaHistoricoRepository extends JpaRepository<EmendaHistorico, Long> {

    List<EmendaHistorico> findByDocumentoIdOrderByDtEmendaDesc(Long documentoId);

    List<EmendaHistorico> findByDocumentoIdAndElementoIdOrderByDtEmendaDesc(Long documentoId, Long elementoId);

    List<EmendaHistorico> findByDocumentoIdAndSecaoOrderByDtEmendaDesc(Long documentoId, SecaoDocumentoEnum secao);

    // Carimba com o ciclo desta publicação todas as linhas ainda pendentes (sem ciclo)
    // do documento — usado em EmendaService.consolidarPublicacao().
    @Modifying
    @Query("UPDATE EmendaHistorico e SET e.cicloReferencia = :ciclo " +
           "WHERE e.documentoId = :documentoId AND e.cicloReferencia IS NULL")
    void marcarCicloPendentes(@Param("documentoId") Long documentoId, @Param("ciclo") String ciclo);

    // Documentos com pelo menos uma emenda registrada (excluindo DESFAZER) — usado para
    // habilitar o botão "Comparar versões" só quando há de fato algo para comparar.
    @Query("SELECT DISTINCT h.documentoId FROM EmendaHistorico h WHERE h.acao <> 'DESFAZER'")
    List<Long> findDocumentoIdsComHistorico();
}
