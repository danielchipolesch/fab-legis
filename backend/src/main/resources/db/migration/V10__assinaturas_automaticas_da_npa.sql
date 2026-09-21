-- "Elaborado por" (autor e coautores) e "Aprovado por" (quem aprova) deixam de ser blocos de assinatura escritos e passam
-- a sair do próprio documento. Os blocos gravados com esses rótulos -- inclusive os dois que toda NPA recebia ao nascer
-- ("Elaborado por" e "Aprovo", este ainda com o texto de orientação) -- são removidos para não duplicar.
UPDATE t_documento_npa
SET tx_assinaturas = COALESCE((
        SELECT jsonb_agg(bloco ORDER BY posicao)
        FROM jsonb_array_elements(tx_assinaturas::jsonb) WITH ORDINALITY AS b(bloco, posicao)
        WHERE lower(regexp_replace(trim(bloco ->> 'rotulo'), ':$', '')) NOT IN ('elaborado por', 'aprovado por')
          AND NOT (lower(regexp_replace(trim(bloco ->> 'rotulo'), ':$', '')) = 'aprovo'
                   AND bloco -> 'linhas' = '["[Nome completo, posto e função da autoridade]"]'::jsonb)
    ), '[]'::jsonb)::text;
