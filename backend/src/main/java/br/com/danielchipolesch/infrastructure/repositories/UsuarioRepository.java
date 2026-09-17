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

    // Mesma coisa, mas filtrando por nome/nome de guerra -- usada quando o seletor de
    // pessoa (SelecionarPessoaDialog.vue) tem um termo de busca digitado, em vez da
    // lista inteira de elegíveis da OM. unaccent() (extensão já criada em
    // V1__initial.sql para a busca full-text) deixa a busca insensível a acento.
    @Query(value = """
            SELECT DISTINCT u FROM Usuario u JOIN u.papeis p
            WHERE u.ativo = true AND u.sistema = false
              AND p = :papel AND u.om.id = :omId
              AND (cast(function('unaccent', lower(u.nome)) as string) LIKE cast(function('unaccent', lower(concat('%', :termo, '%'))) as string)
                OR cast(function('unaccent', lower(u.nomeGuerra)) as string) LIKE cast(function('unaccent', lower(concat('%', :termo, '%'))) as string))
            ORDER BY u.nome
            """)
    List<Usuario> findByOmIdAndPapelAndTermo(
            @Param("omId") Long omId, @Param("papel") PapelEnum papel, @Param("termo") String termo);

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
