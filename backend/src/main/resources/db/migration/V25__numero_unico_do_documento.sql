-- O número de um ato normativo (espécie + assunto básico + sequencial, "DCA 11-3") passa a ser único NO BANCO. Até aqui o menor número livre
-- era escolhido só na aplicação, e duas criações simultâneas podiam escolher o mesmo (docs/replicas.md). Agora a segunda é recusada pelo
-- índice e a aplicação tenta de novo com o número seguinte (CriacaoComNumeracaoUnica).
-- Só vale onde há assunto básico e sequencial: a NPA se identifica por texto livre e não tem nenhum dos dois.
CREATE UNIQUE INDEX ux_documento_numero
    ON t_documento (especie_normativa_id, assunto_basico_id, nr_numero_secundario)
    WHERE assunto_basico_id IS NOT NULL AND nr_numero_secundario IS NOT NULL;
