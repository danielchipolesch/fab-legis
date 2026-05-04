package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_documento_aprovado")
@Data
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_aprovado")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_documento")
    private Documento document;

    //TODO Criar demais atributos
}
