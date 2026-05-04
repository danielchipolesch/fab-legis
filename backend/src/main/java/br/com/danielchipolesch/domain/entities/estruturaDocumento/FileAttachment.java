package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_anexo_arquivo") // Table name must be created
@Data
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anexo_arquivo")
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_documento", nullable = true)
    private Documento documento;

    @Column(name = "nm_anexo_arquivo", columnDefinition = "TEXT")
    private String fileName;

    @Lob
    @Column(name = "bt_conteudo_anexo_arquivo")
//    @Basic(fetch = FetchType.EAGER)
    private byte[] data;

}
