-- Exportação HTML do ato normativo (NSCA 5-3, Art. 8 XXI/17 V §1/18) -- gerado e
-- armazenado nas mesmas transições de status que o PDF (ver DocumentoStatusService).
ALTER TABLE t_documento ADD COLUMN url_html VARCHAR(255);
