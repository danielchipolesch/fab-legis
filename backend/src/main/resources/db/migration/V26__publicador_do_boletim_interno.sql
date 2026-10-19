-- A NPA é de uso interno da OM e passa a ser publicada (e revogada) na própria OM, no Boletim Interno, e não mais pelo CENDOC.
-- Papel novo, em qualquer OM (inclusive o CENDOC, para as NPAs dele):
--   PUB_BI  Publicador do Boletim Interno  -- publica e revoga as NPAs da própria OM
-- O PUB_BCA continua só no CENDOC e só publica as espécies convencionais. Quem hoje é PUB_BCA (e portanto já publicava NPAs) recebe
-- também o PUB_BI, para o CENDOC seguir publicando as NPAs dele; nas demais OMs o administrador da OM concede o papel.
INSERT INTO t_papel (sg_papel, nm_papel, ds_papel) VALUES
    ('PUB_BI', 'Publicador do Boletim Interno', 'Registra a publicação e a revogação das NPAs da própria OM, no Boletim Interno (qualquer OM)');

UPDATE t_papel SET ds_papel = 'Registra a publicação e a revogação das espécies convencionais, de qualquer OM (papel exclusivo do CENDOC)'
 WHERE sg_papel = 'PUB_BCA';

INSERT INTO t_usuario_papel (usuario_id, sg_papel)
SELECT usuario_id, 'PUB_BI' FROM t_usuario_papel WHERE sg_papel = 'PUB_BCA';
