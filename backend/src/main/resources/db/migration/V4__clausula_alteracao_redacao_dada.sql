-- Cláusula das ALTERAÇÕES passa a ser "(redação dada pela Portaria X, publicada no BCA Y)" em vez de
-- "(alterado pela ...)". As de inclusão ("incluído") e revogação ("revogado") não mudam. Só o prefixo
-- das cláusulas já congeladas é reescrito; o restante do texto (portaria/BCA) permanece.
UPDATE t_portaria              SET tx_clausula_emenda          = regexp_replace(tx_clausula_emenda,          '^\(alterado pela ', '(redação dada pela ') WHERE tx_clausula_emenda          LIKE '(alterado pela %';
UPDATE t_portaria              SET tx_clausula_emenda_anterior = regexp_replace(tx_clausula_emenda_anterior, '^\(alterado pela ', '(redação dada pela ') WHERE tx_clausula_emenda_anterior LIKE '(alterado pela %';
UPDATE t_item_parte_normativa  SET tx_clausula_emenda          = regexp_replace(tx_clausula_emenda,          '^\(alterado pela ', '(redação dada pela ') WHERE tx_clausula_emenda          LIKE '(alterado pela %';
UPDATE t_item_parte_normativa  SET tx_clausula_emenda_anterior = regexp_replace(tx_clausula_emenda_anterior, '^\(alterado pela ', '(redação dada pela ') WHERE tx_clausula_emenda_anterior LIKE '(alterado pela %';
UPDATE t_item_parte_final      SET tx_clausula_emenda          = regexp_replace(tx_clausula_emenda,          '^\(alterado pela ', '(redação dada pela ') WHERE tx_clausula_emenda          LIKE '(alterado pela %';
UPDATE t_item_parte_final      SET tx_clausula_emenda_anterior = regexp_replace(tx_clausula_emenda_anterior, '^\(alterado pela ', '(redação dada pela ') WHERE tx_clausula_emenda_anterior LIKE '(alterado pela %';
