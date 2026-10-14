-- Revisão periódica de acessos (docs/autenticacao.md): quem tem papel elevado (aprovador, publicador, auditor, administrador)
-- precisa ter o acesso reconfirmado a cada 6 meses, ao trocar de OM e quando a conta fica 180 dias sem entrar.
--   dt_revisao_acesso : quando o acesso foi reconfirmado (ou concedido); NULO = a revisão está pendente (ex.: trocou de OM)
--   dt_ultimo_acesso  : o último login com sucesso (NULO = ainda não entrou)
-- Quem já existe começa com os dois em "agora": o prazo de 6 meses conta a partir desta migração.
ALTER TABLE t_usuario ADD COLUMN dt_revisao_acesso TIMESTAMP;
ALTER TABLE t_usuario ADD COLUMN dt_ultimo_acesso TIMESTAMP;
UPDATE t_usuario SET dt_revisao_acesso = now(), dt_ultimo_acesso = now() WHERE fl_sistema = false;
