-- Presença de edição passa a ser rastreada só em memória, via conexão SSE
-- viva (ver DocumentoPresencaEmitterRegistry): "quem está editando agora" é
-- literalmente "quem tem uma conexão SSE aberta para este documento", sem
-- precisar mais de heartbeat escrito no banco nem de janela de tempo para
-- decidir quem ainda está ativo. A tabela de heartbeat fica sem uso.
DROP TABLE t_documento_edicao_ativa;
