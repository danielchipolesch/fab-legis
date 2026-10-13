-- Habilitação de documentos: quais espécies normativas CONVENCIONAIS cada OM pode CRIAR. Uma linha = a OM está habilitada
-- para a espécie. As espécies que não dependem de habilitação (a NPA e as demais Comunicações Oficiais Padronizadas) não
-- entram aqui: toda OM as cria. Consultar, comentar e ser coautor não dependem desta tabela.
-- A habilitação padrão (GABAER e CENDOC, em todas as espécies convencionais) é aplicada no primeiro boot por
-- HabilitacaoPadraoRunner, porque as espécies são semeadas por EspecieNormativaRunner, depois das migrations.
CREATE TABLE t_om_especie_habilitada (
    organizacao_militar_id BIGINT NOT NULL REFERENCES t_organizacao_militar (id_organizacao_militar) ON DELETE CASCADE,
    especie_normativa_id   BIGINT NOT NULL REFERENCES t_especie_normativa (id_especie_normativa) ON DELETE CASCADE,
    dt_habilitacao         TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (organizacao_militar_id, especie_normativa_id)
);

CREATE INDEX idx_om_especie_habilitada_especie ON t_om_especie_habilitada (especie_normativa_id);
