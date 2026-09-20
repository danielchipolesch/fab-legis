package br.com.danielchipolesch.domain.entities.numeracaoDocumento;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import br.com.danielchipolesch.domain.regras.TipoDeRegras;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "t_especie_normativa")
//@DynamicUpdate
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class EspecieNormativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especie_normativa")
    private Long id;

    @Column(name = "sg_especie_normativa", nullable = false)
    private String sigla;

    @Column(name = "nm_especie_normativa", nullable = false)
    private String nome;

    // Conjunto de regras a que a espécie obedece (numeração, criação, layout, ciclo de vida) -- ver TipoDeRegras.
    @Enumerated(EnumType.STRING)
    @Column(name = "st_tipo_regras", nullable = false, length = 30)
    private TipoDeRegras tipoDeRegras = TipoDeRegras.ATO_NORMATIVO;

    @Column(name = "tx_descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "dt_criacao", updatable = false)
    @CreationTimestamp
    private Timestamp dtCriacao;

    @Column(name = "dt_alteracao")
    @UpdateTimestamp
    private Timestamp dtAlteracao;

    @Version
    @Column(name = "nr_versao", nullable = false)
    private Integer version;
}
