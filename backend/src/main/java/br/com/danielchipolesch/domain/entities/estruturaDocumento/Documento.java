package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.hateoas.RepresentationModel;

import java.sql.Timestamp;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_documento")
@Data
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")

public class Documento extends RepresentationModel<Documento> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "especie_normativa_id", nullable = false)
    private EspecieNormativa especieNormativa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assunto_basico_id", nullable = false)
    private AssuntoBasico assuntoBasico;

    @Column(name = "nr_numero_secundario", nullable = false)
    private Integer numeroSecundario;

    @Column(name = "nm_titulo_documento", nullable = false)
    private String tituloDocumento;

    @Column(name = "st_documento", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentoStatusEnum documentoStatus;

//    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "portaria_id")
    @Column(name = "portaria_id")
    private Long idPortaria;

//    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY) // Bidirecional
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "documento_id")
    private List<ItemAnexoParteNormativa> itens;

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
