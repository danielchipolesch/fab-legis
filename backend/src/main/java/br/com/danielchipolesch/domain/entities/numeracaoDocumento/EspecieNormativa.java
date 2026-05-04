package br.com.danielchipolesch.domain.entities.numeracaoDocumento;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
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
