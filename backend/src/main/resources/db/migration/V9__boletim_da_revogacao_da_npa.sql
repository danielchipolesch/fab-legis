-- Referência do Boletim Interno em que a NPA foi revogada ("Boletim Interno Ostensivo nº 20, de 3 de maio de 2026").
-- A da publicação fica em t_documento.tx_bca_referencia (a referência oficial da publicação), que a revogação não
-- sobrescreve: a NPA revogada continua mostrando onde foi publicada.
ALTER TABLE t_documento_npa ADD COLUMN tx_boletim_revogacao VARCHAR(255);
