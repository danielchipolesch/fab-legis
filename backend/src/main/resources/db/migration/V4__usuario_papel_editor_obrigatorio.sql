-- Editor passa a ser papel padrão e permanente de todo usuário (exceto o
-- usuário "sistema") -- não dá mais pra criar nem editar um usuário sem ele
-- (ver UsuarioService.papeisComo); a regra de posse sobre documento em si não
-- muda (DocumentoAcessoService continua exigindo autor/coautor). Backfill
-- pontual pra quem já existia antes dessa regra e ainda não tinha o papel --
-- mesmo padrão já usado antes para essa mesma migração de papéis (ver
-- V1__initial.sql, bloco "Reforma de papéis").
INSERT INTO t_usuario_papel (usuario_id, sg_papel)
SELECT id_usuario, 'EDIT'
FROM t_usuario
WHERE fl_sistema = false
  AND id_usuario NOT IN (SELECT usuario_id FROM t_usuario_papel WHERE sg_papel = 'EDIT');
