-- Perfis (papéis) de acesso com CÓDIGO e NOME POR EXTENSO, e com o escopo no próprio código:
--   EDIT    -> ELB      Elaborador
--   APROV   -> APR_OM   Aprovador da OM
--   PUBLIC  -> PUB_BCA  Publicador do BCA
--   AUDITOR -> AUD_GER  Auditor Geral   (lotado no CENDOC)  |  AUD_OM  Auditor da OM  (demais OMs)
--   ADMIN   -> ADM_GER  Administrador Geral (CENDOC)        |  ADM_OM  Administrador da OM (demais OMs)
-- A tabela t_papel é o catálogo (código + nome + descrição) e t_usuario_papel passa a referenciá-la; o enum PapelEnum diz o mesmo
-- (PapelEnumTest confere). O escopo de ADMIN e AUDITOR sai da lotação que a pessoa tem hoje.
CREATE TABLE t_papel (
    sg_papel VARCHAR(20)  PRIMARY KEY,
    nm_papel VARCHAR(60)  NOT NULL UNIQUE,
    ds_papel VARCHAR(255) NOT NULL
);

INSERT INTO t_papel (sg_papel, nm_papel, ds_papel) VALUES
    ('ELB',     'Elaborador',          'Cria e edita documentos como autor ou coautor e os envia para aprovação'),
    ('APR_OM',  'Aprovador da OM',     'Aprova ou devolve os documentos da própria OM (qualquer aprovador da OM age)'),
    ('PUB_BCA', 'Publicador do BCA',   'Registra a publicação e a revogação de documentos (papel exclusivo do CENDOC)'),
    ('AUD_OM',  'Auditor da OM',       'Consulta a trilha de auditoria da própria OM'),
    ('AUD_GER', 'Auditor Geral',       'Consulta a trilha de auditoria de todas as OMs (papel exclusivo do CENDOC)'),
    ('ADM_OM',  'Administrador da OM', 'Gerencia os usuários da própria OM; não altera outros administradores'),
    ('ADM_GER', 'Administrador Geral', 'Gerencia os usuários de todas as OMs e os administradores das OMs; cadastra habilitações e DOM (papel exclusivo do CENDOC)');

UPDATE t_usuario_papel SET sg_papel = 'ELB'     WHERE sg_papel = 'EDIT';
UPDATE t_usuario_papel SET sg_papel = 'APR_OM'  WHERE sg_papel = 'APROV';
UPDATE t_usuario_papel SET sg_papel = 'PUB_BCA' WHERE sg_papel = 'PUBLIC';

UPDATE t_usuario_papel p
   SET sg_papel = CASE WHEN o.sg_organizacao_militar = 'CENDOC' THEN 'ADM_GER' ELSE 'ADM_OM' END
  FROM t_usuario u JOIN t_organizacao_militar o ON o.id_organizacao_militar = u.om_id
 WHERE p.usuario_id = u.id_usuario AND p.sg_papel = 'ADMIN';

UPDATE t_usuario_papel p
   SET sg_papel = CASE WHEN o.sg_organizacao_militar = 'CENDOC' THEN 'AUD_GER' ELSE 'AUD_OM' END
  FROM t_usuario u JOIN t_organizacao_militar o ON o.id_organizacao_militar = u.om_id
 WHERE p.usuario_id = u.id_usuario AND p.sg_papel = 'AUDITOR';

ALTER TABLE t_usuario_papel ADD CONSTRAINT fk_usuario_papel_papel FOREIGN KEY (sg_papel) REFERENCES t_papel (sg_papel);
