package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

// Coautoria de um documento -- dá direito de editar e, enquanto o documento
// estiver em RASCUNHO/MINUTA, de excluir (ver DocumentoAcessoService). Só o
// autor do documento pode criar ou remover uma linha aqui.
@Data
@Entity
@Table(name = "t_documento_compartilhamento")
public class DocumentoCompartilhamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compartilhamento")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "dt_compartilhamento", updatable = false)
    @CreationTimestamp
    private Timestamp dtCompartilhamento;
}
