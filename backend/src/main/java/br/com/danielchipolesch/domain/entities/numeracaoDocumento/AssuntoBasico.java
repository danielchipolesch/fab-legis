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
@Table(name = "t_assunto_basico")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class AssuntoBasico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_assunto_basico")
    private Long id;

    @Column(name = "cd_assunto_basico", length = 4, nullable = false)
    private String codigo;

    @Column(name = "nm_classificacao", nullable = false)
    private String nome;

    @Column(name = "tx_descricao", columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Column(name = "dt_criacao", updatable = false)
    @CreationTimestamp
    private Timestamp dtCriacao;

    @Column(name = "dt_alteracao")
    @UpdateTimestamp
    private Timestamp dtAlteracao;

    @Column(name = "nr_versao", nullable = false)
    @Version
    private Integer versao;
}
