-- Painel Gerencial (novo módulo): dois perfis novos, no mesmo padrão dos demais (trigrama + escopo, com nome por extenso):
--   GES_OM   Gestor da OM   -- indicadores da própria OM (demais OMs)
--   GES_GER  Gestor Geral   -- indicadores de todas as OMs, com filtro por OM (só no CENDOC)
-- O usuário padrão do sistema (o administrador do primeiro boot) já recebe o geral, se existir e estiver no CENDOC.
INSERT INTO t_papel (sg_papel, nm_papel, ds_papel) VALUES
    ('GES_OM',  'Gestor da OM', 'Consulta os indicadores do Painel Gerencial da própria OM'),
    ('GES_GER', 'Gestor Geral', 'Consulta os indicadores do Painel Gerencial de todas as OMs, com filtro por OM (perfil exclusivo do CENDOC)');

UPDATE t_papel SET ds_papel = replace(ds_papel, '(papel exclusivo do CENDOC)', '(perfil exclusivo do CENDOC)');

INSERT INTO t_usuario_papel (usuario_id, sg_papel)
SELECT u.id_usuario, 'GES_GER'
  FROM t_usuario u JOIN t_organizacao_militar o ON o.id_organizacao_militar = u.om_id
 WHERE u.nr_cpf = '11144477735' AND u.fl_sistema = false AND o.sg_organizacao_militar = 'CENDOC'
   AND NOT EXISTS (SELECT 1 FROM t_usuario_papel p WHERE p.usuario_id = u.id_usuario AND p.sg_papel = 'GES_GER');
