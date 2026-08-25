-- PDF da portaria enviado no ato da publicação (ver DocumentoStatusService) --
-- concatenado com o PDF gerado do documento antes de ambos serem armazenados.
ALTER TABLE t_documento ADD COLUMN tx_url_portaria_pdf VARCHAR(500);
