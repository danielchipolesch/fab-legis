-- ── Atualiza constraint de status em t_documento ────────────────────────────
ALTER TABLE t_documento
    DROP CONSTRAINT IF EXISTS t_documento_st_documento_check;

ALTER TABLE t_documento
    ADD CONSTRAINT t_documento_st_documento_check
        CHECK (st_documento IN (
            'RASCUNHO', 'MINUTA', 'APROVADO', 'PUBLICADO',
            'EM_ALTERACAO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO'
        ));

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
