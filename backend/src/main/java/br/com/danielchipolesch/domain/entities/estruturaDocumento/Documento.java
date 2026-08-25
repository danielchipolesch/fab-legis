package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.hateoas.RepresentationModel;

import java.sql.Timestamp;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_documento")
@Data
@NoArgsConstructor
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

    @Column(name = "st_documento", nullable = false, columnDefinition = "VARCHAR(30)")
    @Enumerated(EnumType.STRING)
    private DocumentoStatusEnum documentoStatus;

    @Column(name = "dt_criacao", updatable = false)
    @CreationTimestamp
    private Timestamp dtCriacao;

    @Column(name = "dt_alteracao")
    @UpdateTimestamp
    private Timestamp dtAlteracao;

    @Column(name = "dt_minuta")
    private Timestamp dtMinuta;

    @Column(name = "dt_aprovacao")
    private Timestamp dtAprovacao;

    @Column(name = "dt_publicacao")
    private Timestamp dtPublicacao;

    @Column(name = "dt_arquivamento")
    private Timestamp dtArquivamento;

    @Column(name = "dt_revogacao")
    private Timestamp dtRevogacao;

    @Column(name = "dt_cancelamento")
    private Timestamp dtCancelamento;

    @Column(name = "url_pdf")
    private String urlPdf;

    @Column(name = "nr_replicas", nullable = false, columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    private int qtdReplicas = 0;

    @Column(name = "dt_em_alteracao")
    private Timestamp dtEmAlteracao;

    // Timestamp próprio para EM_ALTERACAO -> ALTERADO, separado de dtAprovacao para não
    // sobrescrever o momento da aprovação original do fluxo normal (RASCUNHO->...->APROVADO).
    @Column(name = "dt_alterado")
    private Timestamp dtAlterado;

    @Column(name = "tx_portaria_referencia")
    private String portariaReferencia;

    @Column(name = "tx_bca_referencia")
    private String bcaReferencia;

    @Column(name = "dt_portaria_referencia")
    private Timestamp dtPortariaReferencia;

    @Column(name = "dt_bca_referencia")
    private Timestamp dtBcaReferencia;

    @Column(name = "nr_versao", nullable = false)
    @Version
    private Integer versao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "om_id", nullable = false)
    private OrganizacaoMilitar om;
}
