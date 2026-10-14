-- A organização militar padrão passa a ser o CENDOC: a OM provisória "SISTEMA" (Organização Padrão do Sistema) deixa de existir.
-- Quem estava nela (o usuário técnico "sistema", o administrador padrão e os documentos que ele criou) vai para o CENDOC; só
-- então a OM é removida (usuários e documentos apontam para ela por chave estrangeira).
-- Instalações novas já nascem assim (DataSeeder cria o administrador na OM do CENDOC); esta migração acerta as que já existem.
UPDATE t_usuario
   SET om_id = (SELECT id_organizacao_militar FROM t_organizacao_militar WHERE sg_organizacao_militar = 'CENDOC')
 WHERE om_id = (SELECT id_organizacao_militar FROM t_organizacao_militar WHERE sg_organizacao_militar = 'SISTEMA');

UPDATE t_documento
   SET om_id = (SELECT id_organizacao_militar FROM t_organizacao_militar WHERE sg_organizacao_militar = 'CENDOC')
 WHERE om_id = (SELECT id_organizacao_militar FROM t_organizacao_militar WHERE sg_organizacao_militar = 'SISTEMA');

DELETE FROM t_organizacao_militar WHERE sg_organizacao_militar = 'SISTEMA';

-- O usuário administrador padrão (CPF 111.444.777-35, o mesmo de sempre): posto, nome completo, nome de guerra e e-mail
-- institucional. Só mexe nele se ele existir (um banco novo o cria no primeiro boot, com os mesmos dados).
UPDATE t_usuario
   SET nm_usuario         = 'DANIEL ANGELO CHIPOLESCH DE ALMEIDA',
       tx_nome_guerra     = 'CHIPOLESCH',
       tx_email           = 'chipoleschdaca@fab.mil.br',
       posto_graduacao_id = (SELECT id_posto_graduacao FROM t_posto_graduacao WHERE sg_bigrama = 'CP')
 WHERE nr_cpf = '11144477735' AND fl_sistema = false;
