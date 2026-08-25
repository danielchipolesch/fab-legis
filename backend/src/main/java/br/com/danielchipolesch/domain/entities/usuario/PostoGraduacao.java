package br.com.danielchipolesch.domain.entities.usuario;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

// Postos (oficiais) e graduações (praças) da FAB -- catálogo de referência,
// nos mesmos moldes de EspecieNormativa/AssuntoBasico. `bigrama` é o código
// de duas (ou três, para 1º/2º/3º) posições usado nas exibições compactas
// (ex.: menu superior); `ordem` reflete a hierarquia militar (menor = mais
// alto), não ordem alfabética, para popular corretamente o seletor.
@Data
@Entity
@Table(name = "t_posto_graduacao")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class PostoGraduacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_posto_graduacao")
    private Long id;

    @Column(name = "nm_posto_graduacao", nullable = false)
    private String nome;

    @Column(name = "sg_bigrama", nullable = false, unique = true, length = 4)
    private String bigrama;

    @Column(name = "nr_ordem", nullable = false)
    private Integer ordem;

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
