-- Orientação da página A4 em que cada anexo de imagem é impresso no PDF (RETRATO | PAISAGEM).
-- Os anexos já existentes continuam em retrato, que era o único comportamento até aqui.
ALTER TABLE t_anexo ADD COLUMN sg_orientacao VARCHAR(10) NOT NULL DEFAULT 'RETRATO';
