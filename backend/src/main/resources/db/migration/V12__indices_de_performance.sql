-- Índices faltando em colunas batidas o tempo todo por filtro/join, sem mudar nenhum
-- comportamento -- só o plano de execução. Até aqui essas consultas faziam sequential scan.

-- t_documento: autor/OM identificam "meus documentos"/"minha OM" (DocumentoSpecifications), as
-- situações filtram toda aba de listagem e os 4 cards do hub, e revisor/publicador atribuído
-- filtram o card "Aguardando Minha Ação".
CREATE INDEX idx_documento_autor ON t_documento(autor_id);
CREATE INDEX idx_documento_om ON t_documento(om_id);
CREATE INDEX idx_documento_situacao_bca ON t_documento(st_situacao_bca);
CREATE INDEX idx_documento_situacao_local ON t_documento(st_situacao_local);
CREATE INDEX idx_documento_revisor_atribuido ON t_documento(revisor_atribuido_id);
CREATE INDEX idx_documento_publicador_atribuido ON t_documento(publicador_atribuido_id);
-- Composto: DocumentoSpecifications.publicadosERevogados filtra pelas duas colunas juntas.
CREATE INDEX idx_documento_situacao_local_bca ON t_documento(st_situacao_local, st_situacao_bca);

-- Itens/anexos/histórico do documento: toda abertura de documento, geração de PDF/HTML e o
-- cache de renderização (DocumentoRenderCacheService) buscam essas tabelas por documento_id.
CREATE INDEX idx_anexo_documento ON t_anexo(documento_id);
CREATE INDEX idx_portaria_documento ON t_portaria(documento_id);
CREATE INDEX idx_item_parte_normativa_documento ON t_item_parte_normativa(documento_id);
CREATE INDEX idx_item_parte_normativa_parent ON t_item_parte_normativa(parent_id);
CREATE INDEX idx_item_parte_final_documento ON t_item_parte_final(documento_id);
CREATE INDEX idx_historico_documento_documento ON t_historico_documento(documento_id);
