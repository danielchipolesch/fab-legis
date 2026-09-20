-- Parágrafo único que virou "§ 1º" quando um 2º parágrafo foi incluído por emenda (o ÚNICO caso
-- de renumeração de parágrafo -- Decreto 12.002/2024, art. 14, IV): a linha "Parágrafo único.
-- texto" é riscada e o texto se repete sob "§ 1º" com a cláusula "renumerado do parágrafo único
-- pela Portaria X, publicada no BCA Y". A cláusula é congelada na publicação da alteração
-- (EmendaService.consolidarPublicacao), como a de qualquer outra emenda.
ALTER TABLE t_item_parte_normativa ADD COLUMN tx_clausula_renumeracao TEXT;
