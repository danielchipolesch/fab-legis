-- Quando um elemento já publicado (com clausulaEmenda congelada) sofre uma NOVA
-- emenda, a cláusula antiga é movida para cá antes de ser limpa (ver EmendaService),
-- em vez de simplesmente descartada. Isso permite mostrar a cláusula histórica ao
-- lado da redação anterior tachada (ex.: "incluído pela Portaria X" riscado junto do
-- texto que ela introduziu), distinta da cláusula atual (nunca tachada) que descreve a
-- emenda em curso — exigência da LC 95/1998 ao encadear múltiplas alterações.
ALTER TABLE t_item_parte_normativa
    ADD COLUMN IF NOT EXISTS tx_clausula_emenda_anterior TEXT;

ALTER TABLE t_portaria
    ADD COLUMN IF NOT EXISTS tx_clausula_emenda_anterior TEXT;

ALTER TABLE t_item_parte_final
    ADD COLUMN IF NOT EXISTS tx_clausula_emenda_anterior TEXT;
