package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_item_parte_final")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemParteFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "documento_id", nullable = false)
    @JsonIgnore
    private Documento documento;

    @Column(name = "sg_tipo_item", nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemAnexoParteNormativaTipoEnum tipo;

    @Column(name = "nr_ordem")
    private Integer elementOrder;

    @Column(name = "tx_titulo_item", columnDefinition = "TEXT")
    private String titulo;

    @Column(name = "tx_conteudo_item", columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "tx_conteudo_completo", columnDefinition = "TEXT")
    private String fullTextContent;

    @Column(name = "st_emenda", length = 20)
    @Enumerated(EnumType.STRING)
    private ElementoEmendaStatusEnum emendaStatus = ElementoEmendaStatusEnum.INALTERADO;

    @Column(name = "tx_conteudo_emenda", columnDefinition = "TEXT")
    private String conteudoEmenda;

    @Column(name = "tx_titulo_emenda", columnDefinition = "TEXT")
    private String tituloEmenda;

    @Column(name = "tx_justificativa_emenda", columnDefinition = "TEXT")
    private String justificativaEmenda;

    @Column(name = "tx_clausula_emenda", columnDefinition = "TEXT")
    private String clausulaEmenda;

    @CreationTimestamp
    @Column(name = "dt_criacao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "dt_atualizacao")
    private LocalDateTime updatedAt;
}
