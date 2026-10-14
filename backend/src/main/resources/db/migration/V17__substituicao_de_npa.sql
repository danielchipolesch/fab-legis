-- "Substituir" uma NPA publicada: a NPA nova guarda qual NPA ela substitui. Para mudar uma NPA publicada cria-se outra e
-- revoga-se a anterior (a NPA não passa por alteração); este vínculo registra a ligação entre as duas. Nulo = documento
-- que não substitui nenhum outro.
ALTER TABLE t_documento ADD COLUMN substitui_documento_id BIGINT REFERENCES t_documento (id_documento);
CREATE INDEX idx_documento_substitui ON t_documento (substitui_documento_id);
