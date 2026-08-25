package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.notificacao.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByDestinatarioIdAndLidaFalseOrderByDtCriacaoDesc(Long destinatarioId);

    Page<Notificacao> findByDestinatarioIdOrderByDtCriacaoDesc(Long destinatarioId, Pageable pageable);

    long countByDestinatarioIdAndLidaFalse(Long destinatarioId);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true, n.dtLeitura = :agora WHERE n.destinatario.id = :destinatarioId AND n.lida = false")
    void marcarTodasComoLidas(@Param("destinatarioId") Long destinatarioId, @Param("agora") Timestamp agora);
}
