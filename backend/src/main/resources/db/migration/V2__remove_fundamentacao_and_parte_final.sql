-- Remove FUNDAMENTACAO from parte_preliminar
DELETE FROM t_portaria WHERE sg_tipo_item = 'FUNDAMENTACAO';

-- Move FECHO and ASSINATURA from parte_final to parte_preliminar,
-- with high order numbers so they appear after epigrafe/ementa/preambulo
INSERT INTO t_portaria (documento_id, sg_tipo_item, nr_ordem, tx_titulo_item, tx_conteudo_item, tx_conteudo_completo, dt_criacao, dt_atualizacao)
SELECT documento_id, sg_tipo_item, 100 + COALESCE(nr_ordem, 0), tx_titulo_item, tx_conteudo_item, tx_conteudo_completo, dt_criacao, dt_atualizacao
FROM t_item_parte_final
WHERE sg_tipo_item IN ('FECHO', 'ASSINATURA');

-- Delete all parte_final records (clausula_revogatoria, clausula_vigencia, fecho, assinatura, referenda)
DELETE FROM t_item_parte_final;
