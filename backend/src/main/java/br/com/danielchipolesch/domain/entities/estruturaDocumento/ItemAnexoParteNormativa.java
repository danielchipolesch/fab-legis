package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Entity
@Table(name = "t_item_parte_normativa")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ItemAnexoParteNormativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "documento_id", nullable = false)
    @JsonIgnore
    private Documento documento;

//    @Column(name = "parent_id")
//    private Long parentItem;

    @Column(name = "sg_tipo_item", nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemAnexoParteNormativaTipoEnum tipo;  // Ex: "CAPÍTULO", "SEÇÃO", "ARTIGO"

    @Column(name = "tx_titulo_item")
    private String titulo; // Nome do item normativo

    @Column(name = "tx_conteudo_item")
    private String conteuto; // Texto do item

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private ItemAnexoParteNormativa parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn(name = "item_anexo_parte_normativa_id")
    private List<ItemAnexoParteNormativa> children; // Lista de elementos filhos
}
