package intraer.fablegis.infrastructure.repositories;

import intraer.fablegis.domain.entities.estruturaDocumento.DocumentoCompartilhamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoCompartilhamentoRepository extends JpaRepository<DocumentoCompartilhamento, Long> {
    List<DocumentoCompartilhamento> findByDocumentoId(Long documentoId);
    Optional<DocumentoCompartilhamento> findByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    boolean existsByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);

    // Batch (1 query pra página inteira, não 1 por linha) -- usado por
    // DocumentoController.getAll pra marcar, na listagem, quais documentos o
    // usuário logado é coautor (ehAutorOuCoautor no DTO).
    @Query("SELECT c.documento.id FROM DocumentoCompartilhamento c WHERE c.usuario.id = :usuarioId AND c.documento.id IN :documentoIds")
    List<Long> findDocumentoIdsCompartilhadosComUsuario(@Param("usuarioId") Long usuarioId,
                                                          @Param("documentoIds") Collection<Long> documentoIds);
}
