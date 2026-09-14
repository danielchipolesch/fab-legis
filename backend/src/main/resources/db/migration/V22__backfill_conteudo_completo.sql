-- V21 acrescentou a busca full-text sobre tx_conteudo_completo, mas expôs um
-- bug pré-existente: o fallback antigo de gerarFullTextContent (corrigido no
-- mesmo commit desta migration, ver TipTapPlainTextExtractor) concatenava o
-- JSON BRUTO do TipTap em tx_conteudo_completo quando o frontend não mandava
-- o texto pronto -- linhas já gravadas antes da correção continuam com esse
-- JSON cru na coluna, poluindo tsv_busca (coluna GERADA a partir dela). Este
-- backfill reextrai o texto puro dos nós "text" do próprio tx_conteudo_item
-- (percorrendo a árvore "content" recursivamente, mesmo algoritmo de
-- TipTapPlainTextExtractor.java) só nas linhas afetadas -- identificadas pelo
-- próprio sintoma: tx_conteudo_completo contendo o JSON do doc.
--
-- Laço por linha (não um único UPDATE com subquery correlacionada) para que
-- uma linha com conteudo_item malformado não derrube a migration inteira --
-- só pula essa linha e registra um NOTICE.
DO $$
DECLARE
    r RECORD;
    texto TEXT;
BEGIN
    FOR r IN SELECT id_item AS id, tx_titulo_item AS titulo, tx_conteudo_item AS conteudo
             FROM t_item_parte_normativa
             WHERE tx_conteudo_completo LIKE '%"type":"doc"%'
    LOOP
        BEGIN
            SELECT string_agg(node ->> 'text', ' ') INTO texto
            FROM (
                WITH RECURSIVE nodes AS (
                    SELECT r.conteudo::jsonb AS node
                    UNION ALL
                    SELECT jsonb_array_elements(coalesce(n.node -> 'content', '[]'::jsonb))
                    FROM nodes n
                )
                SELECT node FROM nodes WHERE node ->> 'text' IS NOT NULL
            ) t;
            UPDATE t_item_parte_normativa
            SET tx_conteudo_completo = NULLIF(trim(coalesce(r.titulo, '') || ' ' || coalesce(texto, '')), '')
            WHERE id_item = r.id;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Falha ao reextrair texto do item % (t_item_parte_normativa): %', r.id, SQLERRM;
        END;
    END LOOP;
END $$;

DO $$
DECLARE
    r RECORD;
    texto TEXT;
BEGIN
    FOR r IN SELECT id_portaria AS id, tx_titulo_item AS titulo, tx_conteudo_item AS conteudo
             FROM t_portaria
             WHERE tx_conteudo_completo LIKE '%"type":"doc"%'
    LOOP
        BEGIN
            SELECT string_agg(node ->> 'text', ' ') INTO texto
            FROM (
                WITH RECURSIVE nodes AS (
                    SELECT r.conteudo::jsonb AS node
                    UNION ALL
                    SELECT jsonb_array_elements(coalesce(n.node -> 'content', '[]'::jsonb))
                    FROM nodes n
                )
                SELECT node FROM nodes WHERE node ->> 'text' IS NOT NULL
            ) t;
            UPDATE t_portaria
            SET tx_conteudo_completo = NULLIF(trim(coalesce(r.titulo, '') || ' ' || coalesce(texto, '')), '')
            WHERE id_portaria = r.id;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Falha ao reextrair texto do item % (t_portaria): %', r.id, SQLERRM;
        END;
    END LOOP;
END $$;

DO $$
DECLARE
    r RECORD;
    texto TEXT;
BEGIN
    FOR r IN SELECT id_item AS id, tx_titulo_item AS titulo, tx_conteudo_item AS conteudo
             FROM t_item_parte_final
             WHERE tx_conteudo_completo LIKE '%"type":"doc"%'
    LOOP
        BEGIN
            SELECT string_agg(node ->> 'text', ' ') INTO texto
            FROM (
                WITH RECURSIVE nodes AS (
                    SELECT r.conteudo::jsonb AS node
                    UNION ALL
                    SELECT jsonb_array_elements(coalesce(n.node -> 'content', '[]'::jsonb))
                    FROM nodes n
                )
                SELECT node FROM nodes WHERE node ->> 'text' IS NOT NULL
            ) t;
            UPDATE t_item_parte_final
            SET tx_conteudo_completo = NULLIF(trim(coalesce(r.titulo, '') || ' ' || coalesce(texto, '')), '')
            WHERE id_item = r.id;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Falha ao reextrair texto do item % (t_item_parte_final): %', r.id, SQLERRM;
        END;
    END LOOP;
END $$;
