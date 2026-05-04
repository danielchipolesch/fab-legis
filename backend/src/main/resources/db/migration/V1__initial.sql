CREATE TABLE t_especie_normativa(
    id_especie_normativa INT NOT NULL,
    sg_especie_normativa VARCHAR(5) NOT NULL,
    nm_especie_normativa VARCHAR(255) NOT NULL,
    tx_descricao TEXT NOT NULL,
    dt_criacao TIMESTAMP,
    dt_alteracao TIMESTAMP,
    nr_versao INT
)