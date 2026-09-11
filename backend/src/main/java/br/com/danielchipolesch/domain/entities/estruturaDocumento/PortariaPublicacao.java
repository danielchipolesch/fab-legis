package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Registro histórico e imutável de cada portaria do ciclo de vida de um
// documento (edição, sucessivas alterações, revogação) -- ver
// PortariaPublicacaoService. O PDF em si fica intacto no MinIO (url), nunca
// mesclado com o PDF gerado do documento (ver DocumentoPdfService).
@Entity
@Table(name = "t_portaria_publicacao")
@Data
@NoArgsConstructor
public class PortariaPublicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_portaria_publicacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(name = "sg_tipo", nullable = false, columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    private TipoPortariaPublicacaoEnum tipo;

    // Só preenchido para tipo == ALTERACAO (1ª, 2ª, 3ª... alteração). Nulo
    // para EDICAO/REVOGACAO, que só ocorrem uma vez por documento.
    @Column(name = "nr_sequencial")
    private Integer numeroSequencial;

    @Column(name = "tx_orgao", nullable = false)
    private String orgao;

    @Column(name = "tx_setor")
    private String setor;

    @Column(name = "tx_numero_portaria", nullable = false)
    private String numeroPortaria;

    @Column(name = "dt_portaria", nullable = false)
    private LocalDate dataPortaria;

    @Column(name = "nr_bca", nullable = false)
    private Integer numeroBca;

    @Column(name = "dt_bca", nullable = false)
    private LocalDate dataBca;

    @Column(name = "url_pdf", nullable = false)
    private String urlPdf;

    @CreationTimestamp
    @Column(name = "dt_criacao", updatable = false)
    private LocalDateTime dtCriacao;
}
