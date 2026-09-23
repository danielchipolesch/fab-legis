-- A coluna criada em V5 (st_regime) passa a se chamar pelo que ela realmente escolhe: qual conjunto de regras
-- (numeração, criação, layout, ciclo de vida) a espécie segue -- ATO_NORMATIVO (DCA, ICA, NSCA...) ou, no futuro, NPA.
ALTER TABLE t_especie_normativa RENAME COLUMN st_regime TO st_tipo_regras;
