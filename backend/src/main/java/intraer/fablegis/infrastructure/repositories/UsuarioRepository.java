package intraer.fablegis.infrastructure.repositories;

import intraer.fablegis.domain.entities.usuario.PerfilEnum;
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

    // Quem tem o perfil (ativo, de verdade): destinatário dos avisos de "aguardando publicação" -- o perfil PUBLIC só existe no CENDOC.
    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.perfis p WHERE u.ativo = true AND u.sistema = false AND p = :perfil ORDER BY u.nome")
    List<Usuario> findAtivosByPerfil(@Param("perfil") PerfilEnum perfil);

    // Quantos usuários ativos têm o perfil: garante que o CENDOC nunca fique sem Administrador Geral (ADM_GER).
    @Query("SELECT COUNT(DISTINCT u) FROM Usuario u JOIN u.perfis p WHERE u.ativo = true AND u.sistema = false AND p = :perfil")
    long contarAtivosComPerfil(@Param("perfil") PerfilEnum perfil);

    // Os aprovadores ativos de uma OM (perfil APROV): destinatários do aviso de "aguardando aprovação" (DocumentoStatusService) --
    // sempre restrito à OM do documento, nunca cross-OM.
    @Query("""
            SELECT DISTINCT u FROM Usuario u JOIN u.perfis p
            WHERE u.ativo = true AND u.sistema = false
              AND p = :perfil AND u.om.id = :omId
            ORDER BY u.nome
            """)
    List<Usuario> findByOmIdAndPerfil(@Param("omId") Long omId, @Param("perfil") PerfilEnum perfil);

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
