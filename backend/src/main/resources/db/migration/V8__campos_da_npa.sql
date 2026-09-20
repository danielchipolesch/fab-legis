-- Campos que só a NPA tem (CamposEspecificosDaEspecie): uma linha por documento, criada com o documento e removida
-- junto com ele. Ficam fora de t_documento para os atos normativos não carregarem colunas que nunca usam.
--   tx_setor_emissor : setor que emite a NPA (linha do cabeçalho, abaixo da OM) -- texto livre
--   tx_local         : "Local" da linha "Local, dd de mês de aaaa" do fecho; a data é a da aprovação
--   tx_assinaturas   : JSON com os blocos de assinatura em texto livre: [{"rotulo": "...", "linhas": ["..."]}]
CREATE TABLE t_documento_npa (
    documento_id     BIGINT PRIMARY KEY REFERENCES t_documento (id_documento) ON DELETE CASCADE,
    tx_setor_emissor VARCHAR(255) NOT NULL,
    tx_local         VARCHAR(120) NOT NULL,
    tx_assinaturas   TEXT NOT NULL
);
