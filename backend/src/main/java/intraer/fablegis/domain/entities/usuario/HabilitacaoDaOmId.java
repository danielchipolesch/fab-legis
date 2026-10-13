package intraer.fablegis.domain.entities.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

// Chave de uma habilitação: a OM e a espécie normativa que ela pode criar.
@Embeddable
public record HabilitacaoDaOmId(
        @Column(name = "organizacao_militar_id") Long organizacaoMilitarId,
        @Column(name = "especie_normativa_id") Long especieNormativaId
) implements Serializable {
}
