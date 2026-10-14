package intraer.fablegis.domain.entities.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;

// "A OM X está habilitada a criar documentos da espécie normativa Y" (só as Espécies Convencionais dependem disto; ver
// RegraDeHabilitacaoDaOm). Quem habilita e desabilita é o administrador (CENDOC), na tela de habilitação de documentos.
@Entity
@Table(name = "t_om_especie_habilitada")
@Getter
@NoArgsConstructor
public class HabilitacaoDaOm {

    @EmbeddedId
    private HabilitacaoDaOmId id;

    @Column(name = "dt_habilitacao", nullable = false)
    private Timestamp dtHabilitacao = Timestamp.from(Instant.now());

    public HabilitacaoDaOm(Long organizacaoMilitarId, Long especieNormativaId) {
        this.id = new HabilitacaoDaOmId(organizacaoMilitarId, especieNormativaId);
    }

    public Long getOrganizacaoMilitarId() { return id.organizacaoMilitarId(); }

    public Long getEspecieNormativaId() { return id.especieNormativaId(); }
}
