-- Situação BCA (real, espelha o repositório oficial) x Situação Local (etapa interna).
--
-- Até aqui uma única coluna (st_documento) misturava as duas: PUBLICADO -> EM_ALTERACAO
-- "despublicava" o ato no sistema, mesmo estando oficialmente publicado. Agora:
--   st_situacao_bca   NAO_PUBLICADO | PUBLICADO | REVOGADO   (só muda com portaria + BCA)
--   st_situacao_local RASCUNHO | MINUTA | EM_REVISAO | EM_PUBLICACAO | EM_ALTERACAO |
--                     ANALISE_REVOGACAO | EM_REVOGACAO | CANCELADO | SEM_ETAPA
-- APROVADO/ALTERADO deixam de ser estados (aprovar cascateia direto para EM_PUBLICACAO);
-- PUBLICADO/REVOGADO viram situação BCA + SEM_ETAPA. Ver SituacaoBcaEnum/SituacaoLocalEnum.

ALTER TABLE t_documento ADD COLUMN st_situacao_bca VARCHAR(30);
ALTER TABLE t_documento RENAME COLUMN st_documento TO st_situacao_local;

-- BCA: revogado se o antigo status já era REVOGADO; publicado se já houve uma primeira
-- publicação (dt_publicacao é o discriminante que o serviço sempre usou); senão, não publicado.
UPDATE t_documento
SET st_situacao_bca = CASE
        WHEN st_situacao_local = 'REVOGADO' THEN 'REVOGADO'
        WHEN dt_publicacao IS NOT NULL      THEN 'PUBLICADO'
        ELSE 'NAO_PUBLICADO'
    END;
ALTER TABLE t_documento ALTER COLUMN st_situacao_bca SET NOT NULL;

-- Versão em tramitação armazenada (EM_PUBLICACAO/EM_REVOGACAO): o PDF/HTML gravado nessas
-- etapas era regenerado na aprovação, com marca d'água -- é a cópia "em tramitação". A versão
-- vigente anterior (se houve) já tinha sido sobrescrita por ele e não é recuperável.
ALTER TABLE t_documento ADD COLUMN url_pdf_tramitacao  VARCHAR(255);
ALTER TABLE t_documento ADD COLUMN url_html_tramitacao VARCHAR(255);
UPDATE t_documento
SET url_pdf_tramitacao  = url_pdf,
    url_html_tramitacao = url_html,
    url_pdf  = NULL,
    url_html = NULL
WHERE st_situacao_local IN ('EM_PUBLICACAO', 'APROVADO', 'ALTERADO', 'EM_REVOGACAO');

UPDATE t_documento
SET st_situacao_local = CASE st_situacao_local
        WHEN 'APROVADO'  THEN 'EM_PUBLICACAO'
        WHEN 'ALTERADO'  THEN 'EM_PUBLICACAO'
        WHEN 'PUBLICADO' THEN 'SEM_ETAPA'
        WHEN 'REVOGADO'  THEN 'SEM_ETAPA'
        ELSE st_situacao_local
    END;

-- Histórico: mesmas conversões nas duas colunas.
UPDATE t_historico_documento
SET sg_status_anterior = CASE sg_status_anterior
        WHEN 'APROVADO'  THEN 'EM_PUBLICACAO'
        WHEN 'ALTERADO'  THEN 'EM_PUBLICACAO'
        WHEN 'PUBLICADO' THEN 'SEM_ETAPA'
        WHEN 'REVOGADO'  THEN 'SEM_ETAPA'
        ELSE sg_status_anterior
    END,
    sg_status_novo = CASE sg_status_novo
        WHEN 'APROVADO'  THEN 'EM_PUBLICACAO'
        WHEN 'ALTERADO'  THEN 'EM_PUBLICACAO'
        WHEN 'PUBLICADO' THEN 'SEM_ETAPA'
        WHEN 'REVOGADO'  THEN 'SEM_ETAPA'
        ELSE sg_status_novo
    END;
