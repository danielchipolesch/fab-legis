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
@Table(name = "t_portaria")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemPartePreliminar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_portaria")
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

    @CreationTimestamp
    @Column(name = "dt_criacao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "dt_atualizacao")
    private LocalDateTime updatedAt;
}
