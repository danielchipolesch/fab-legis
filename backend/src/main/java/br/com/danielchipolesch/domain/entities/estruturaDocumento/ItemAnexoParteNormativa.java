package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "t_item_parte_normativa")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemAnexoParteNormativa {

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

    @Column(name = "tx_conteudo_original", columnDefinition = "TEXT")
    private String conteudoOriginal;

    @Column(name = "tx_titulo_original", columnDefinition = "TEXT")
    private String tituloOriginal;

    @Column(name = "tx_justificativa_emenda", columnDefinition = "TEXT")
    private String justificativaEmenda;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private ItemAnexoParteNormativa parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemAnexoParteNormativa> children;

    @CreationTimestamp
    @Column(name = "dt_criacao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "dt_atualizacao")
    private LocalDateTime updatedAt;
}
