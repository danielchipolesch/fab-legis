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
    @JoinColumn(name = "assunto_basico_id", nullable = true)
    private AssuntoBasico assuntoBasico;

    @Column(name = "nr_numero_secundario", nullable = true)
    private Integer numeroSecundario;

    @Column(name = "nm_titulo_documento", nullable = false)
    private String tituloDocumento;

    // Como o documento se chama em toda tela e no PDF ("DCA 11-3"): gravada na criação pelas regras da espécie
    // (RegrasDeCriacaoDoDocumento) -- nos atos normativos é SIGLA + assunto básico + sequencial; numa NPA, texto livre.
    // Nunca é recalculada depois.
    @Column(name = "tx_identificacao", nullable = false, length = 120)
    private String identificacao;

    // Situação BCA: a situação REAL, que espelha o repositório oficial -- só muda quando
    // portaria + BCA são registrados (ver SituacaoBcaEnum e DocumentoStatusService).
    @Column(name = "st_situacao_bca", nullable = false, columnDefinition = "VARCHAR(30)")
    @Enumerated(EnumType.STRING)
    private SituacaoBcaEnum situacaoBca = SituacaoBcaEnum.NAO_PUBLICADO;

    // Situação Local: a etapa INTERNA em curso (ver SituacaoLocalEnum). Nunca substitui a
    // situação BCA -- os dois aparecem juntos.
    @Column(name = "st_situacao_local", nullable = false, columnDefinition = "VARCHAR(30)")
    @Enumerated(EnumType.STRING)
    private SituacaoLocalEnum situacaoLocal;

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

    @Column(name = "dt_revogacao")
    private Timestamp dtRevogacao;

    @Column(name = "dt_cancelamento")
    private Timestamp dtCancelamento;

    // Versão VIGENTE (a que a situação BCA descreve): só é gravada/substituída quando uma
    // portaria + BCA são registrados -- nunca por uma etapa interna. Vazia enquanto o
    // documento é NAO_PUBLICADO.
    @Column(name = "url_pdf")
    private String urlPdf;

    // Mesmas transições de status geram/armazenam os dois (ver DocumentoStatusService) --
    // HTML e PDF são sempre regenerados juntos, nunca um sem o outro.
    @Column(name = "url_html")
    private String urlHtml;

    // Versão EM TRAMITAÇÃO congelada (EM_PUBLICACAO/EM_REVOGACAO), para o publicador ver
    // exatamente o que será publicado. Descartada ao publicar, devolver ou cancelar. Nas
    // etapas em que o texto ainda muda (RASCUNHO/MINUTA/EM_ALTERACAO/EM_REVISAO) a versão
    // em tramitação é gerada em tempo de execução, nunca armazenada.
    @Column(name = "url_pdf_tramitacao")
    private String urlPdfTramitacao;

    @Column(name = "url_html_tramitacao")
    private String urlHtmlTramitacao;

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

    // Pessoa específica (papel APROV) escolhida por quem enviou para revisão -- só ela
    // pode agir no documento enquanto ele estiver em EM_REVISAO/ANALISE_REVOGACAO (ver
    // DocumentoAcessoService.podeMudarStatus). Reaproveitado nas duas etapas de revisão
    // (fluxo normal e revogação), sempre sobrescrito a cada novo envio.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "revisor_atribuido_id")
    private Usuario revisorAtribuido;

    // Pessoa específica (papel PUBLIC) escolhida pelo APROV ao aprovar -- só ela pode
    // agir no documento enquanto ele estiver em EM_PUBLICACAO/EM_REVOGACAO. Mesmo
    // padrão de revisorAtribuido, reaproveitado nas duas etapas de publicação.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "publicador_atribuido_id")
    private Usuario publicadorAtribuido;

    @Column(name = "dt_em_revisao")
    private Timestamp dtEmRevisao;

    @Column(name = "dt_em_publicacao")
    private Timestamp dtEmPublicacao;

    @Column(name = "dt_analise_revogacao")
    private Timestamp dtAnaliseRevogacao;

    @Column(name = "dt_em_revogacao")
    private Timestamp dtEmRevogacao;
}
