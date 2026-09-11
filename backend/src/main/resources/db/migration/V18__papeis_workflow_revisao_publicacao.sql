-- Novos status intermediários (EM_REVISAO/EM_PUBLICACAO) e fluxo de revogação em
-- 3 etapas (ANALISE_REVOGACAO/EM_REVOGACAO/REVOGADO), com atribuição pessoal --
-- ver DocumentoStatusService/DocumentoAcessoService. ARQUIVADO deixa de existir
-- como conceito (0 linhas com esse status hoje).

ALTER TABLE t_documento
    ADD COLUMN revisor_atribuido_id     BIGINT REFERENCES t_usuario (id_usuario),
    ADD COLUMN publicador_atribuido_id  BIGINT REFERENCES t_usuario (id_usuario),
    ADD COLUMN dt_em_revisao            TIMESTAMP,
    ADD COLUMN dt_em_publicacao         TIMESTAMP,
    ADD COLUMN dt_analise_revogacao     TIMESTAMP,
    ADD COLUMN dt_em_revogacao          TIMESTAMP,
    DROP COLUMN dt_arquivamento;

-- Reforma de papéis: APROVADOR (aprovar+publicar) vira dois papéis separados,
-- APROV (só revisa/aprova) e PUBLIC (só publica formalmente) -- ver PapelEnum.
-- EDIT passa a ser explícito (hoje toda pessoa logada podia criar/editar).
UPDATE t_usuario_papel SET sg_papel = 'APROV' WHERE sg_papel = 'APROVADOR';

-- Admin deixa de ser um super-papel de documento (só gestão de usuários/OMs) --
-- quem tinha ADMIN + APROVADOR ao mesmo tempo perde o papel de documento.
DELETE FROM t_usuario_papel
WHERE sg_papel = 'APROV'
  AND usuario_id IN (SELECT usuario_id FROM t_usuario_papel WHERE sg_papel = 'ADMIN');

-- Backfill: preserva o acesso de quem já cria/edita documentos hoje (implícito,
-- sem papel nenhum) -- exceto quem só administra o sistema (ADMIN).
INSERT INTO t_usuario_papel (usuario_id, sg_papel)
SELECT id_usuario, 'EDIT'
FROM t_usuario
WHERE fl_sistema = false
  AND id_usuario NOT IN (SELECT usuario_id FROM t_usuario_papel WHERE sg_papel = 'ADMIN')
  AND id_usuario NOT IN (SELECT usuario_id FROM t_usuario_papel WHERE sg_papel = 'EDIT');
