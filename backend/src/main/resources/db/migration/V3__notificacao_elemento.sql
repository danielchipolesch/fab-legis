-- Permite que uma notificação de comentário leve o usuário direto ao elemento
-- comentado (não só ao documento) -- em um documento grande, "abrir o documento"
-- não basta, a pessoa ainda precisaria vasculhar a árvore inteira pra achar o
-- comentário. NULL para os demais tipos de notificação (compartilhamento,
-- aprovação pendente), que já levam pro lugar certo (o próprio documento).
ALTER TABLE t_notificacao
    ADD COLUMN elemento_id BIGINT,
    ADD COLUMN secao       VARCHAR(30);
