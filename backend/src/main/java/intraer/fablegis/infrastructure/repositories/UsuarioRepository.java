package intraer.fablegis.infrastructure.repositories;

import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Direto no banco: não mexe na versão (@Version) nem em dt_alteracao do usuário só por ele ter entrado.
    @org.springframework.data.jpa.repository.Modifying
    @Query("update Usuario u set u.dtUltimoAcesso = :quando where u.id = :id")
    void registrarUltimoAcesso(@Param("id") Long id, @Param("quando") java.sql.Timestamp quando);

    Optional<Usuario> findByCpf(String cpf);
    boolean existsByCpf(String cpf);

    // Quem tem o papel (ativo, de verdade): destinatário dos avisos de "aguardando publicação" -- o papel PUBLIC só existe no CENDOC.
    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.papeis p WHERE u.ativo = true AND u.sistema = false AND p = :papel ORDER BY u.nome")
    List<Usuario> findAtivosByPapel(@Param("papel") PapelEnum papel);

    // Quantos usuários ativos têm o papel: garante que o CENDOC nunca fique sem Administrador Geral (ADM_GER).
    @Query("SELECT COUNT(DISTINCT u) FROM Usuario u JOIN u.papeis p WHERE u.ativo = true AND u.sistema = false AND p = :papel")
    long contarAtivosComPapel(@Param("papel") PapelEnum papel);

    // Os aprovadores ativos de uma OM (papel APROV): destinatários do aviso de "aguardando aprovação" (DocumentoStatusService) --
    // sempre restrito à OM do documento, nunca cross-OM.
    @Query("""
            SELECT DISTINCT u FROM Usuario u JOIN u.papeis p
            WHERE u.ativo = true AND u.sistema = false
              AND p = :papel AND u.om.id = :omId
            ORDER BY u.nome
            """)
    List<Usuario> findByOmIdAndPapel(@Param("omId") Long omId, @Param("papel") PapelEnum papel);

    // Busca de coautor por nome/nome de guerra -- deliberadamente SEM filtro de OM:
    // coautoria não é restrita à OM do autor (ver DocumentoCompartilhamentoService),
    // então a busca precisa ser system-wide igual à regra de negócio já é.
    @Query(value = """
            SELECT u FROM Usuario u
            WHERE u.ativo = true AND u.sistema = false AND u.id <> :excluirId
              AND (cast(function('unaccent', lower(u.nome)) as string) LIKE cast(function('unaccent', lower(concat('%', :termo, '%'))) as string)
                OR cast(function('unaccent', lower(u.nomeGuerra)) as string) LIKE cast(function('unaccent', lower(concat('%', :termo, '%'))) as string))
            ORDER BY u.nome
            """)
    List<Usuario> buscarPorNome(@Param("termo") String termo, @Param("excluirId") Long excluirId);
}
