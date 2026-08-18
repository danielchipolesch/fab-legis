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

    @Column(name = "tx_conteudo_emenda", columnDefinition = "TEXT")
    private String conteudoEmenda;

    @Column(name = "tx_titulo_emenda", columnDefinition = "TEXT")
    private String tituloEmenda;

    @Column(name = "tx_justificativa_emenda", columnDefinition = "TEXT")
    private String justificativaEmenda;

    // Texto da cláusula (ex.: "(incluído pela Portaria X, publicada no BCA Y)")
    // congelado no momento da (re)publicação — permanece mesmo depois que emendaStatus volta a INALTERADO no próximo ciclo. Distinto do cálculo ao vivo usado enquanto a emenda ainda está pendente (não publicada).
    @Column(name = "tx_clausula_emenda", columnDefinition = "TEXT")
    private String clausulaEmenda;

    // Cláusula da redação ANTERIOR a esta emenda (ex.: "incluído pela Portaria X"),
    // preenchida só quando uma nova emenda começa sobre um elemento já publicado — ver
    // EmendaService. Mostrada riscada ao lado do texto que ela descreve (LC 95/1998),
    // diferente de clausulaEmenda (a atual, nunca riscada).
    @Column(name = "tx_clausula_emenda_anterior", columnDefinition = "TEXT")
    private String clausulaEmendaAnterior;

    // Permanente, independente de emendaStatus — usada só pela numeração com sufixo de
    // letra (ex.: "Art. 5-A"). emendaStatus é livre para evoluir (INCLUIDO -> ALTERADO
    // -> REVOGADO) sem nunca perder a marca de inclusão, senão a numeração sequencial
    // seguinte seria indevidamente deslocada. Ver V5__incluido_por_emenda_permanente.sql.
    @Column(name = "fl_incluido_emenda", nullable = false)
    private boolean incluidoPorEmenda = false;

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
