-- Marca a qual ciclo de publicação (portaria/BCA) cada linha de t_emenda_historico
-- pertence. Sem isso, o histórico é um log linear indistinguível entre diferentes
-- alterações do mesmo documento — impossível montar o "Quadro de Justificativas das
-- Modificações Propostas" (NSCA 5-3, Anexo XXIV) separado por publicação.
--
-- Carimbada em EmendaService.consolidarPublicacao(), o mesmo momento em que
-- clausulaEmenda é congelada nos elementos — linhas ainda com tx_ciclo_referencia NULL
-- pertencem ao ciclo de alteração em andamento (ainda não publicado).
ALTER TABLE t_emenda_historico
    ADD COLUMN IF NOT EXISTS tx_ciclo_referencia TEXT;
