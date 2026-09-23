-- O tipo da espécie passa a ter os nomes da NSCA 5-3: Espécies Convencionais (MCA, NSCA, ICA, ROCA, DCA...) e Espécies de
-- Comunicações Oficiais Padronizadas (Capítulo VIII, Seção VIII -- hoje a NPA). A coluna deixa de falar em "regras"
-- (st_tipo_regras) e passa a dizer o que a espécie é (st_tipo_especie); ATO_NORMATIVO vira CONVENCIONAL e NPA vira
-- COMUNICACAO_OFICIAL_PADRONIZADA (31 letras: a coluna, de 30, é alargada antes).
ALTER TABLE t_especie_normativa RENAME COLUMN st_tipo_regras TO st_tipo_especie;
ALTER TABLE t_especie_normativa ALTER COLUMN st_tipo_especie TYPE VARCHAR(40);
UPDATE t_especie_normativa SET st_tipo_especie = 'CONVENCIONAL' WHERE st_tipo_especie = 'ATO_NORMATIVO';
UPDATE t_especie_normativa SET st_tipo_especie = 'COMUNICACAO_OFICIAL_PADRONIZADA' WHERE st_tipo_especie = 'NPA';
