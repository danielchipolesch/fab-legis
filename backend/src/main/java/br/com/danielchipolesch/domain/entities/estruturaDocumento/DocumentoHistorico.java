package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_historico_documento")
@Data
@NoArgsConstructor
public class DocumentoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historico")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(name = "sg_tipo_alteracao", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoAlteracaoEnum tipoAlteracao;

    @Column(name = "tx_descricao", nullable = false)
    private String descricao;

    @Column(name = "sg_status_anterior", columnDefinition = "VARCHAR(30)")
    @Enumerated(EnumType.STRING)
    private DocumentoStatusEnum statusAnterior;

    @Column(name = "sg_status_novo", columnDefinition = "VARCHAR(30)")
    @Enumerated(EnumType.STRING)
    private DocumentoStatusEnum statusNovo;

    @Column(name = "nm_usuario")
    private String usuario;

    @CreationTimestamp
    @Column(name = "dt_registro", updatable = false)
    private LocalDateTime dtRegistro;
}
