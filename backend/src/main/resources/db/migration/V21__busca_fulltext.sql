-- Busca full-text sobre o conteúdo dos dispositivos (não só metadados) --
-- ver docs/funcionalidades.md. unaccent combinado ao stemmer 'portuguese'
-- (config própria) para que "publicacao" (sem acento) ache "publicação".
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TEXT SEARCH CONFIGURATION portuguese_unaccent (COPY = portuguese);
ALTER TEXT SEARCH CONFIGURATION portuguese_unaccent
    ALTER MAPPING FOR hword, hword_part, word WITH unaccent, portuguese_stem;

-- Coluna GERADA (STORED): Postgres recalcula sozinho a cada INSERT/UPDATE,
-- sem trigger nem sincronização manual -- e já popula as linhas existentes
-- ao rodar o ALTER TABLE, sem precisar de um UPDATE de backfill à parte.
-- Indexa tx_titulo_item + tx_conteudo_completo (texto puro, já mantido por
-- DocumentoParteNormativaService.gerarFullTextContent) -- NUNCA
-- tx_conteudo_item, que guarda o JSON bruto do TipTap (ver collab/server.js),
-- não texto pesquisável.
ALTER TABLE t_item_parte_normativa
    ADD COLUMN tsv_busca tsvector
    GENERATED ALWAYS AS (
        to_tsvector('portuguese_unaccent', coalesce(tx_titulo_item, '') || ' ' || coalesce(tx_conteudo_completo, ''))
    ) STORED;
CREATE INDEX idx_item_parte_normativa_busca ON t_item_parte_normativa USING GIN (tsv_busca);

ALTER TABLE t_portaria
    ADD COLUMN tsv_busca tsvector
    GENERATED ALWAYS AS (
        to_tsvector('portuguese_unaccent', coalesce(tx_titulo_item, '') || ' ' || coalesce(tx_conteudo_completo, ''))
    ) STORED;
CREATE INDEX idx_portaria_busca ON t_portaria USING GIN (tsv_busca);

ALTER TABLE t_item_parte_final
    ADD COLUMN tsv_busca tsvector
    GENERATED ALWAYS AS (
        to_tsvector('portuguese_unaccent', coalesce(tx_titulo_item, '') || ' ' || coalesce(tx_conteudo_completo, ''))
    ) STORED;
CREATE INDEX idx_item_parte_final_busca ON t_item_parte_final USING GIN (tsv_busca);
