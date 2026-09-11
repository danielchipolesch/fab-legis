package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.domain.entities.usuario.PapelEnum;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCpf(String cpf);
    boolean existsByCpf(String cpf);

    // Candidatos elegíveis pra um seletor de "escolher pessoa" (revisor/publicador,
    // ver UsuarioController/DocumentoStatusService) -- sempre restrito à mesma OM de
    // quem está escolhendo, nunca cross-OM.
    @Query("""
            SELECT DISTINCT u FROM Usuario u JOIN u.papeis p
            WHERE u.ativo = true AND u.sistema = false
              AND p = :papel AND u.om.id = :omId
            ORDER BY u.nome
            """)
    List<Usuario> findByOmIdAndPapel(@Param("omId") Long omId, @Param("papel") PapelEnum papel);
}
