package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ComentarioElemento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioElementoRepository extends JpaRepository<ComentarioElemento, Long> {

    // Todos os comentários do documento de uma vez (mesmo padrão de carregar a
    // árvore inteira já usado por DocumentoParteNormativaService) -- o frontend
    // agrupa em threads por elementoId/parentId, sem precisar de uma chamada por
    // elemento comentado.
    List<ComentarioElemento> findByDocumentoIdOrderByDtCriacaoAsc(Long documentoId);
}
