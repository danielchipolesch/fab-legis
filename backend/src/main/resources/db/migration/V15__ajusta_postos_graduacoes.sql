-- Ajustes ao catálogo de postos/graduações da FAB (ver migração V14):
-- bigrama de Taifeiro-Mor corrigido para TM, e inclusão das classes de
-- Soldado e Taifeiro (1ª/2ª classe), que a V14 não cobria.
UPDATE t_posto_graduacao SET sg_bigrama = 'TM' WHERE sg_bigrama = 'TF';

INSERT INTO t_posto_graduacao (nm_posto_graduacao, sg_bigrama, nr_ordem) VALUES
    ('Soldado de Primeira Classe', 'S1', 175),
    ('Soldado de Segunda Classe',  'S2', 176),
    ('Taifeiro de Primeira Classe','T1', 177),
    ('Taifeiro de Segunda Classe', 'T2', 178);
