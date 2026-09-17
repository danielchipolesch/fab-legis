package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

// Comentário em linha sobre um elemento específico (artigo, parágrafo, inciso...) --
// não é edição de conteúdo, é anotação de revisão por fora, deliberadamente
// separada do Yjs/Hocuspocus (ver V2__comentario_elemento.sql).
@Data
@Entity
@Table(name = "t_comentario_elemento")
public class ComentarioElemento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comentario")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    // Sem FK -- pode ser um id de t_item_parte_normativa, t_portaria ou
    // t_item_parte_final, dependendo de "secao" (mesmo padrão de
    // EmendaHistorico.elementoId).
    @Column(name = "elemento_id", nullable = false)
    private Long elementoId;

    @Column(name = "secao", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private SecaoDocumentoEnum secao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "tx_texto", nullable = false, columnDefinition = "TEXT")
    private String texto;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ComentarioElemento parent;

    @Column(name = "fl_resolvido", nullable = false)
    private boolean resolvido = false;

    @CreationTimestamp
    @Column(name = "dt_criacao", updatable = false)
    private Timestamp dtCriacao;

    @Column(name = "dt_resolucao")
    private Timestamp dtResolucao;
}
