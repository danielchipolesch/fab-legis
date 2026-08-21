package br.com.danielchipolesch.domain.entities.usuario;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.Set;

@Data
@Entity
@Table(name = "t_usuario")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "nm_usuario", nullable = false)
    private String nome;

    @Column(name = "nr_cpf", nullable = false, unique = true, length = 11)
    private String cpf;

    @JsonIgnore
    @Column(name = "tx_senha_hash", nullable = false)
    private String senhaHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "om_id", nullable = false)
    private OrganizacaoMilitar om;

    @Column(name = "fl_ativo", nullable = false)
    private boolean ativo = true;

    // Usuário técnico dono dos documentos que existiam antes da introdução de
    // autoria (ver migração V9) -- nunca autentica (fl_ativo=false, hash
    // inutilizável) e nunca aparece como opção de compartilhamento.
    @Column(name = "fl_sistema", nullable = false)
    private boolean sistema = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "t_usuario_papel", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "sg_papel")
    @Enumerated(EnumType.STRING)
    private Set<PapelEnum> papeis = EnumSet.noneOf(PapelEnum.class);

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
