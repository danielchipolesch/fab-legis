-- Regime normativo da espécie: o conjunto de regras (numeração, criação, layout, ciclo de vida) a que ela
-- obedece. Todas as espécies existentes são atos normativos; a NPA ganhará o seu regime depois.
ALTER TABLE t_especie_normativa ADD COLUMN st_regime VARCHAR(30) NOT NULL DEFAULT 'ATO_NORMATIVO';
