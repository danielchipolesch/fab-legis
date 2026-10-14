-- Os perfis do Painel Gerencial passam a ter o código do próprio módulo (e deixam de se chamar "Gestor"):
--   GES_OM  Gestor da OM  -> PNL_OM   Painel Gerencial da OM
--   GES_GER Gestor Geral  -> PNL_GER  Painel Gerencial Geral
-- Quem já tinha o perfil antigo passa a ter o novo. (O V22 não foi reescrito: um banco que já o aplicou acusaria checksum diferente.)
INSERT INTO t_papel (sg_papel, nm_papel, ds_papel) VALUES
    ('PNL_OM',  'Painel Gerencial da OM', 'Consulta os indicadores do Painel Gerencial da própria OM'),
    ('PNL_GER', 'Painel Gerencial Geral', 'Consulta os indicadores do Painel Gerencial de todas as OMs, com filtro por OM (perfil exclusivo do CENDOC)');

UPDATE t_usuario_papel SET sg_papel = 'PNL_OM'  WHERE sg_papel = 'GES_OM';
UPDATE t_usuario_papel SET sg_papel = 'PNL_GER' WHERE sg_papel = 'GES_GER';

DELETE FROM t_papel WHERE sg_papel IN ('GES_OM', 'GES_GER');
