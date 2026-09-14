package br.com.danielchipolesch.infrastructure.repositories;

import br.com.danielchipolesch.application.dtos.buscaDtos.ItemBuscaResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

// Busca full-text sobre o CONTEÚDO dos dispositivos (não só metadados, ver
// DocumentoRepository/DocumentoSpecifications) -- cruza as 3 tabelas de item
// (parte preliminar/normativa/final) com UNION ALL, já que compartilham a
// mesma forma (ver comentário em V1__initial.sql). Usa EntityManager nativo
// (não @Query em interface) e mapeia List<Object[]> por posição em vez de
// projeção por interface -- o casamento de alias (documento_id ->
// getDocumentoId()) em query nativa com UNION é frágil o bastante pra não
// valer a economia de código; posição é explícito e não quebra silenciosamente
// se um alias mudar de nome.
//
// Sem checagem de posse nenhuma aqui de propósito: visualizar já é liberado a
// qualquer usuário autenticado, de qualquer OM, em qualquer situação (ver
// DocumentoAcessoService) -- a busca não teria por que ser mais restritiva
// que abrir o próprio documento diretamente.
@Repository
public class BuscaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // 'portuguese_unaccent' (ver V21__busca_fulltext.sql) combina o stemmer
    // 'portuguese' com o dicionário unaccent, pra "publicacao" (sem acento)
    // achar "publicação". websearch_to_tsquery (não plainto_tsquery) entende
    // sintaxe de caixa de busca comum ("frase exata", -excluir, ou) sem o
    // usuário precisar aprender operadores de tsquery.
    private static final String UNIAO_SQL = """
            SELECT d.id_documento, en.sg_especie_normativa, ab.cd_assunto_basico, d.nr_numero_secundario,
                   d.nm_titulo_documento, d.st_documento, 'PARTE_PRELIMINAR', p.sg_tipo_item, p.id_portaria,
                   ts_rank_cd(p.tsv_busca, websearch_to_tsquery('portuguese_unaccent', ?1)) AS relevancia,
                   ts_headline('portuguese_unaccent', coalesce(p.tx_conteudo_completo, ''),
                       websearch_to_tsquery('portuguese_unaccent', ?1),
                       'StartSel=,StopSel=,MaxFragments=1,MaxWords=35,MinWords=15') AS trecho
            FROM t_portaria p
            JOIN t_documento d ON d.id_documento = p.documento_id
            JOIN t_especie_normativa en ON en.id_especie_normativa = d.especie_normativa_id
            JOIN t_assunto_basico ab ON ab.id_assunto_basico = d.assunto_basico_id
            WHERE p.tsv_busca @@ websearch_to_tsquery('portuguese_unaccent', ?1)

            UNION ALL

            SELECT d.id_documento, en.sg_especie_normativa, ab.cd_assunto_basico, d.nr_numero_secundario,
                   d.nm_titulo_documento, d.st_documento, 'PARTE_NORMATIVA', i.sg_tipo_item, i.id_item,
                   ts_rank_cd(i.tsv_busca, websearch_to_tsquery('portuguese_unaccent', ?1)),
                   ts_headline('portuguese_unaccent', coalesce(i.tx_conteudo_completo, ''),
                       websearch_to_tsquery('portuguese_unaccent', ?1),
                       'StartSel=,StopSel=,MaxFragments=1,MaxWords=35,MinWords=15')
            FROM t_item_parte_normativa i
            JOIN t_documento d ON d.id_documento = i.documento_id
            JOIN t_especie_normativa en ON en.id_especie_normativa = d.especie_normativa_id
            JOIN t_assunto_basico ab ON ab.id_assunto_basico = d.assunto_basico_id
            WHERE i.tsv_busca @@ websearch_to_tsquery('portuguese_unaccent', ?1)

            UNION ALL

            SELECT d.id_documento, en.sg_especie_normativa, ab.cd_assunto_basico, d.nr_numero_secundario,
                   d.nm_titulo_documento, d.st_documento, 'PARTE_FINAL', f.sg_tipo_item, f.id_item,
                   ts_rank_cd(f.tsv_busca, websearch_to_tsquery('portuguese_unaccent', ?1)),
                   ts_headline('portuguese_unaccent', coalesce(f.tx_conteudo_completo, ''),
                       websearch_to_tsquery('portuguese_unaccent', ?1),
                       'StartSel=,StopSel=,MaxFragments=1,MaxWords=35,MinWords=15')
            FROM t_item_parte_final f
            JOIN t_documento d ON d.id_documento = f.documento_id
            JOIN t_especie_normativa en ON en.id_especie_normativa = d.especie_normativa_id
            JOIN t_assunto_basico ab ON ab.id_assunto_basico = d.assunto_basico_id
            WHERE f.tsv_busca @@ websearch_to_tsquery('portuguese_unaccent', ?1)
            """;

    @SuppressWarnings("unchecked")
    public List<ItemBuscaResponseDto> buscar(String termo, int limit, long offset) {
        List<Object[]> linhas = entityManager.createNativeQuery(
                        "SELECT * FROM (" + UNIAO_SQL + ") resultados ORDER BY relevancia DESC LIMIT ?2 OFFSET ?3")
                .setParameter(1, termo)
                .setParameter(2, limit)
                .setParameter(3, offset)
                .getResultList();
        return linhas.stream().map(BuscaRepository::mapear).toList();
    }

    public long contar(String termo) {
        Number total = (Number) entityManager.createNativeQuery(
                        "SELECT count(*) FROM (" + UNIAO_SQL + ") resultados")
                .setParameter(1, termo)
                .getSingleResult();
        return total.longValue();
    }

    private static ItemBuscaResponseDto mapear(Object[] linha) {
        return new ItemBuscaResponseDto(
                ((Number) linha[0]).longValue(),
                (String) linha[1],
                (String) linha[2],
                linha[3] != null ? ((Number) linha[3]).intValue() : null,
                (String) linha[4],
                DocumentoStatusEnum.valueOf((String) linha[5]),
                SecaoDocumentoEnum.valueOf((String) linha[6]),
                ItemAnexoParteNormativaTipoEnum.valueOf((String) linha[7]),
                ((Number) linha[8]).longValue(),
                (String) linha[10]
        );
    }
}
