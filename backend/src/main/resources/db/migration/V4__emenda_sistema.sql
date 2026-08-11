-- ── Remove qualquer check constraint no campo st_documento ──────────────────
-- (Hibernate gera com nomes variados; dropamos todos e não recriamos,
--  pois a entidade usa columnDefinition = "VARCHAR(30)" sem constraint.)
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT con.conname
        FROM   pg_constraint con
        JOIN   pg_class      rel ON rel.oid = con.conrelid
        JOIN   pg_attribute  att ON att.attrelid = rel.oid
                                AND att.attnum = ANY(con.conkey)
        WHERE  rel.relname = 't_documento'
          AND  con.contype = 'c'
          AND  att.attname = 'st_documento'
    LOOP
        EXECUTE 'ALTER TABLE t_documento DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
    END LOOP;
END $$;

-- ── Novos campos em t_documento ──────────────────────────────────────────────
ALTER TABLE t_documento
    ADD COLUMN IF NOT EXISTS dt_em_alteracao      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS tx_portaria_referencia TEXT,
    ADD COLUMN IF NOT EXISTS tx_bca_referencia      TEXT;

-- ── Novos campos em t_portaria (ItemPartePreliminar) ─────────────────────────
ALTER TABLE t_portaria
    ADD COLUMN IF NOT EXISTS st_emenda             VARCHAR(20) DEFAULT 'INALTERADO',
    ADD COLUMN IF NOT EXISTS tx_conteudo_original  TEXT,
    ADD COLUMN IF NOT EXISTS tx_titulo_original    TEXT,
    ADD COLUMN IF NOT EXISTS tx_justificativa_emenda TEXT;

-- ── Novos campos em t_item_parte_normativa ───────────────────────────────────
ALTER TABLE t_item_parte_normativa
    ADD COLUMN IF NOT EXISTS st_emenda             VARCHAR(20) DEFAULT 'INALTERADO',
    ADD COLUMN IF NOT EXISTS tx_conteudo_original  TEXT,
    ADD COLUMN IF NOT EXISTS tx_titulo_original    TEXT,
    ADD COLUMN IF NOT EXISTS tx_justificativa_emenda TEXT;

-- ── Novos campos em t_item_parte_final ──────────────────────────────────────
ALTER TABLE t_item_parte_final
    ADD COLUMN IF NOT EXISTS st_emenda             VARCHAR(20) DEFAULT 'INALTERADO',
    ADD COLUMN IF NOT EXISTS tx_conteudo_original  TEXT,
    ADD COLUMN IF NOT EXISTS tx_titulo_original    TEXT,
    ADD COLUMN IF NOT EXISTS tx_justificativa_emenda TEXT;
