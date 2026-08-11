package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.EmendaHistorico;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmendaHistoricoRepository extends JpaRepository<EmendaHistorico, Long> {

    List<EmendaHistorico> findByDocumentoIdOrderByDtEmendaDesc(Long documentoId);

    List<EmendaHistorico> findByDocumentoIdAndElementoIdOrderByDtEmendaDesc(Long documentoId, Long elementoId);

    List<EmendaHistorico> findByDocumentoIdAndSecaoOrderByDtEmendaDesc(Long documentoId, SecaoDocumentoEnum secao);
}
