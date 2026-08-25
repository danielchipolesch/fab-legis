-- Datas de referência da Portaria e do BCA usados para (re)publicar um documento
-- após alteração. Necessárias para validar que a data de cada nova alteração não
-- é anterior à data da alteração anterior.
ALTER TABLE t_documento
    ADD COLUMN IF NOT EXISTS dt_portaria_referencia TIMESTAMP,
    ADD COLUMN IF NOT EXISTS dt_bca_referencia       TIMESTAMP;
