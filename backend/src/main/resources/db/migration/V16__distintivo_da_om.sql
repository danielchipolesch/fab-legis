-- DOM (distintivo da organização militar): a imagem que a NPA mostra no cabeçalho (Anexo XII da NSCA 5-3). Uma por OM,
-- cadastrada pelo administrador; fica no armazenamento de objetos (MinIO) e aqui só guardamos a URL. Nulo = a OM ainda não
-- tem distintivo (o espaço fica em branco, como antes).
ALTER TABLE t_organizacao_militar ADD COLUMN tx_url_distintivo TEXT;
