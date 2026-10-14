-- "Enviar para aprovação" não escolhe mais uma pessoa: qualquer aprovador da OM do documento age, e qualquer publicador (só existe
-- no CENDOC) publica. As colunas deixam de ser uma ATRIBUIÇÃO e passam a registrar QUEM aprovou e QUEM publicou (ou revogou):
--   aprovador_id  : quem aprovou o texto (ou a revogação) -- é o "Aprovado por" da NPA; nulo enquanto ninguém aprovou
--   publicador_id : quem registrou a publicação (ou a revogação)
-- As atribuições que estavam em aberto viram "ninguém" (qualquer um pode agir), exceto onde já havia decisão tomada: o aprovador
-- de um documento que já passou da revisão continua sendo quem foi.
ALTER TABLE t_documento RENAME COLUMN revisor_atribuido_id TO aprovador_id;
ALTER TABLE t_documento RENAME COLUMN publicador_atribuido_id TO publicador_id;
ALTER INDEX idx_documento_revisor_atribuido RENAME TO idx_documento_aprovador;
ALTER INDEX idx_documento_publicador_atribuido RENAME TO idx_documento_publicador;
UPDATE t_documento SET aprovador_id = NULL WHERE st_situacao_local IN ('EM_REVISAO', 'ANALISE_REVOGACAO');
UPDATE t_documento SET publicador_id = NULL WHERE st_situacao_local IN ('EM_PUBLICACAO', 'EM_REVOGACAO');
