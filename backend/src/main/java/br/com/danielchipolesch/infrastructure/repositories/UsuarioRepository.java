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

    // Quem deve ser avisado quando um documento da OM entra em situação que
    // aguarda aprovação (ver NotificacaoService/DocumentoStatusService) --
    // ADMIN também aprova em qualquer OM (ver DocumentoAcessoService),
    // então também recebe.
    @Query("""
            SELECT DISTINCT u FROM Usuario u JOIN u.papeis p
            WHERE u.ativo = true AND u.sistema = false
              AND p IN :papeis
              AND (p = br.com.danielchipolesch.domain.entities.usuario.PapelEnum.ADMIN OR u.om.id = :omId)
            """)
    List<Usuario> findAprovadoresDaOmOuAdmins(@Param("omId") Long omId, @Param("papeis") List<PapelEnum> papeis);
}
