-- Backfill: linhas de t_emenda_historico gravadas ANTES da V7 existir nunca foram
-- carimbadas com o ciclo de publicação — tx_ciclo_referencia ficou NULL para sempre.
-- Sem isso, ficam invisíveis no Quadro de Justificativas: não aparecem como "ciclo em
-- andamento" (o elemento correspondente já foi publicado, não está mais pendente — ver
-- EmendaService.listarItensPendentes) nem pertencem a nenhum ciclo reconhecível (nunca
-- foram carimbadas).
--
-- Não é possível reconstruir com precisão A QUAL publicação cada uma pertenceu: o
-- texto da cláusula de ciclos anteriores ao mais recente já era sobrescrito pelo
-- desenho antigo (antes de clausulaEmenda virar permanente, na V4/V6). Agrupa todas
-- sob um rótulo genérico só para deixarem de desaparecer — melhor um agrupamento
-- honesto do que uma separação por ciclo fabricada sem dado real por trás.
UPDATE t_emenda_historico h
SET tx_ciclo_referencia = 'Publicações anteriores a esta funcionalidade'
WHERE h.tx_ciclo_referencia IS NULL
  AND h.acao <> 'DESFAZER'
  AND (
    (h.secao = 'PARTE_NORMATIVA' AND EXISTS (
        SELECT 1 FROM t_item_parte_normativa i
        WHERE i.id_item = h.elemento_id AND i.tx_clausula_emenda IS NOT NULL))
    OR (h.secao = 'PARTE_PRELIMINAR' AND EXISTS (
        SELECT 1 FROM t_portaria i
        WHERE i.id_portaria = h.elemento_id AND i.tx_clausula_emenda IS NOT NULL))
    OR (h.secao = 'PARTE_FINAL' AND EXISTS (
        SELECT 1 FROM t_item_parte_final i
        WHERE i.id_item = h.elemento_id AND i.tx_clausula_emenda IS NOT NULL))
  );
