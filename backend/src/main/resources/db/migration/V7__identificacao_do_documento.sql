-- A identificação do documento (o "DCA 11-3" que aparece em toda tela) passa a ser gravada, criada pelas regras
-- da espécie: nos atos normativos continua sendo SIGLA + assunto básico + sequencial; numa NPA será texto livre,
-- sem assunto básico nem sequencial -- daí as duas colunas deixarem de ser obrigatórias.
ALTER TABLE t_documento ADD COLUMN tx_identificacao VARCHAR(120);

UPDATE t_documento d
   SET tx_identificacao = e.sg_especie_normativa || ' ' || a.cd_assunto_basico || '-' || d.nr_numero_secundario
  FROM t_especie_normativa e, t_assunto_basico a
 WHERE e.id_especie_normativa = d.especie_normativa_id
   AND a.id_assunto_basico = d.assunto_basico_id;

ALTER TABLE t_documento ALTER COLUMN tx_identificacao SET NOT NULL;
ALTER TABLE t_documento ALTER COLUMN assunto_basico_id DROP NOT NULL;
ALTER TABLE t_documento ALTER COLUMN nr_numero_secundario DROP NOT NULL;
