-- Marca permanente e independente do status de emenda ao vivo (st_emenda), usada
-- exclusivamente pela numeração com sufixo de letra (ex.: "Art. 5-A"). Sem essa
-- separação, um artigo incluído por emenda que depois fosse alterado ou revogado
-- perderia a marca INCLUIDO (necessária para nunca consumir numeração sequencial) e
-- todos os artigos seguintes seriam indevidamente renumerados — violando a vedação de
-- renumeração da LC 95/1998. Com a marca separada, st_emenda fica livre para refletir
-- o ciclo de vida real da emenda (INCLUIDO -> ALTERADO -> REVOGADO, cada um com sua
-- própria cláusula), sem afetar a numeração.
ALTER TABLE t_item_parte_normativa
    ADD COLUMN IF NOT EXISTS fl_incluido_emenda BOOLEAN NOT NULL DEFAULT false;

-- Backfill: até aqui, nada nunca tirava um elemento do status INCLUIDO, então todo elemento atualmente INCLUIDO foi de fato incluído por emenda em algum momento.
UPDATE t_item_parte_normativa SET fl_incluido_emenda = true WHERE st_emenda = 'INCLUIDO';
