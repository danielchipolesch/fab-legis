package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.PortariaPublicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PortariaPublicacaoRepository extends JpaRepository<PortariaPublicacao, Long> {

    List<PortariaPublicacao> findByDocumentoIdOrderByDtCriacaoAsc(Long documentoId);

    @Query("SELECT COALESCE(MAX(p.numeroSequencial), 0) FROM PortariaPublicacao p " +
            "WHERE p.documento.id = :documentoId AND p.tipo = 'ALTERACAO'")
    int findMaxNumeroSequencialAlteracao(Long documentoId);
}
