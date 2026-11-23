-- Rascunho vira Minuta SÓ ao enviar para aprovação (e não mais na primeira edição, no primeiro anexo ou por um passo de organização), e a
-- Minuta passa a ser a etapa que era "Em revisão": o documento em aprovação ou em conferência, editado por quem aprova. A etapa EM_REVISAO
-- deixa de existir. Devolver volta a RASCUNHO; excluir e cancelar só valem em RASCUNHO; nasce o status ARQUIVADO (a autoridade arquiva uma
-- Minuta, com justificativa). Ver docs/ciclo-de-vida.md e SituacaoLocalEnum.
--
-- Os documentos que já existem são rearranjados. A ORDEM importa: primeiro a Minuta antiga (nunca foi enviada) volta a RASCUNHO, só depois
-- a Em revisão vira a Minuta nova -- no sentido contrário, os dois grupos se misturariam.

-- Documentos: quem estava em MINUTA (só organizado, nunca enviado) volta a RASCUNHO; quem estava EM_REVISAO passa a MINUTA.
UPDATE t_documento SET st_situacao_local = 'RASCUNHO' WHERE st_situacao_local = 'MINUTA';
UPDATE t_documento SET st_situacao_local = 'MINUTA' WHERE st_situacao_local = 'EM_REVISAO';

-- dt_minuta passa a ser a data do ENVIO PARA APROVAÇÃO (o que era dt_em_revisao). Quem nunca foi enviado fica sem ela.
UPDATE t_documento SET dt_minuta = dt_em_revisao;
ALTER TABLE t_documento DROP COLUMN dt_em_revisao;

-- Histórico: o passo antigo RASCUNHO -> MINUTA não existe mais (era só organização) e sai; nos demais, as mesmas conversões nas duas colunas e
-- na descrição ("EM_REVISAO -> MINUTA" era devolver, e agora é MINUTA -> RASCUNHO; "MINUTA -> EM_REVISAO" era enviar, e agora é RASCUNHO -> MINUTA).
DELETE FROM t_historico_documento
WHERE sg_tipo_alteracao = 'ALTERACAO_STATUS' AND sg_status_anterior = 'RASCUNHO' AND sg_status_novo = 'MINUTA';

UPDATE t_historico_documento
SET sg_status_anterior = CASE sg_status_anterior
        WHEN 'MINUTA'     THEN 'RASCUNHO'
        WHEN 'EM_REVISAO' THEN 'MINUTA'
        ELSE sg_status_anterior
    END,
    sg_status_novo = CASE sg_status_novo
        WHEN 'MINUTA'     THEN 'RASCUNHO'
        WHEN 'EM_REVISAO' THEN 'MINUTA'
        ELSE sg_status_novo
    END,
    tx_descricao = replace(replace(replace(tx_descricao, 'MINUTA', '#R#'), 'EM_REVISAO', 'MINUTA'), '#R#', 'RASCUNHO')
WHERE sg_tipo_alteracao = 'ALTERACAO_STATUS'
  AND (sg_status_anterior IN ('MINUTA', 'EM_REVISAO') OR sg_status_novo IN ('MINUTA', 'EM_REVISAO'));

-- Arquivamento: quando, por quem (a autoridade que aprova) e por quê (justificativa obrigatória) -- só em ARQUIVADO.
ALTER TABLE t_documento
    ADD COLUMN dt_arquivamento TIMESTAMP,
    ADD COLUMN tx_justificativa_arquivamento TEXT,
    ADD COLUMN arquivador_id BIGINT REFERENCES t_usuario (id_usuario);
