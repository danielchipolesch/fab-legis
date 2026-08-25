-- Congela o texto da cláusula de emenda (ex.: "incluído pela Portaria X, publicada
-- no BCA Y") no próprio elemento no momento da (re)publicação. Sem isso, a cláusula
-- era recalculada ao vivo a partir de t_documento.tx_portaria_referencia/
-- tx_bca_referencia, que são sobrescritos a cada nova republicação — corrompendo
-- retroativamente as cláusulas de ciclos de alteração anteriores.
ALTER TABLE t_item_parte_normativa
    ADD COLUMN IF NOT EXISTS tx_clausula_emenda TEXT;

ALTER TABLE t_portaria
    ADD COLUMN IF NOT EXISTS tx_clausula_emenda TEXT;

ALTER TABLE t_item_parte_final
    ADD COLUMN IF NOT EXISTS tx_clausula_emenda TEXT;
