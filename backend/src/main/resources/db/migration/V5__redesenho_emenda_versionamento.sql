-- ── V5: Redesenho do sistema de emenda com versionamento ────────────────────
-- Executar manualmente (Flyway não está ativo):
--   docker exec -it fab-legis-postgres psql -U postgres -d fab-legis-dev -f /path/to/V5.sql
--
-- O Hibernate (ddl-auto=update) cria automaticamente as colunas novas e a
-- tabela t_emenda_historico quando o backend sobe. Este script apenas migra
-- os dados das colunas antigas para as novas e remove as antigas.

-- ── Migrar dados: tx_conteudo_original → tx_conteudo_emenda ─────────────────

-- t_item_parte_normativa
UPDATE t_item_parte_normativa
   SET tx_conteudo_emenda = tx_conteudo_original,
       tx_titulo_emenda   = tx_titulo_original
 WHERE st_emenda = 'ALTERADO'
   AND tx_conteudo_original IS NOT NULL;

-- Para elementos REVOGADOS, o conteúdo original estava em tx_conteudo_original
-- e o campo tx_conteudo_item foi zerado. Agora restauramos tx_conteudo_item
-- a partir de tx_conteudo_original, pois conteudo = original publicado.
UPDATE t_item_parte_normativa
   SET tx_conteudo_item = tx_conteudo_original
 WHERE st_emenda = 'REVOGADO'
   AND tx_conteudo_original IS NOT NULL
   AND (tx_conteudo_item IS NULL OR tx_conteudo_item = '');

-- t_portaria (parte preliminar)
UPDATE t_portaria
   SET tx_conteudo_emenda = tx_conteudo_original,
       tx_titulo_emenda   = tx_titulo_original
 WHERE st_emenda = 'ALTERADO'
   AND tx_conteudo_original IS NOT NULL;

UPDATE t_portaria
   SET tx_conteudo_item = tx_conteudo_original
 WHERE st_emenda = 'REVOGADO'
   AND tx_conteudo_original IS NOT NULL
   AND (tx_conteudo_item IS NULL OR tx_conteudo_item = '');

-- t_item_parte_final
UPDATE t_item_parte_final
   SET tx_conteudo_emenda = tx_conteudo_original,
       tx_titulo_emenda   = tx_titulo_original
 WHERE st_emenda = 'ALTERADO'
   AND tx_conteudo_original IS NOT NULL;

UPDATE t_item_parte_final
   SET tx_conteudo_item = tx_conteudo_original
 WHERE st_emenda = 'REVOGADO'
   AND tx_conteudo_original IS NOT NULL
   AND (tx_conteudo_item IS NULL OR tx_conteudo_item = '');

-- ── Remover colunas antigas (opcional — só após confirmar migração) ──────────
-- ALTER TABLE t_item_parte_normativa DROP COLUMN IF EXISTS tx_conteudo_original;
-- ALTER TABLE t_item_parte_normativa DROP COLUMN IF EXISTS tx_titulo_original;
-- ALTER TABLE t_portaria              DROP COLUMN IF EXISTS tx_conteudo_original;
-- ALTER TABLE t_portaria              DROP COLUMN IF EXISTS tx_titulo_original;
-- ALTER TABLE t_item_parte_final      DROP COLUMN IF EXISTS tx_conteudo_original;
-- ALTER TABLE t_item_parte_final      DROP COLUMN IF EXISTS tx_titulo_original;
