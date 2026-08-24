package br.com.danielchipolesch.domain.entities.notificacao;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

// documentoId/documentoDescricao são um snapshot, não uma FK real para
// t_documento -- mesmo raciocínio do LogAuditoria: o documento pode ser
// excluído depois e a notificação (inclusive já lida, no histórico) tem
// que continuar fazendo sentido.
@Data
@Entity
@Table(name = "t_notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Column(name = "sg_tipo", nullable = false, columnDefinition = "VARCHAR(30)")
    @Enumerated(EnumType.STRING)
    private TipoNotificacaoEnum tipo;

    @Column(name = "tx_mensagem", nullable = false)
    private String mensagem;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @Column(name = "documento_descricao", nullable = false)
    private String documentoDescricao;

    @Column(name = "fl_lida", nullable = false)
    private boolean lida = false;

    @Column(name = "dt_criacao", updatable = false)
    @CreationTimestamp
    private Timestamp dtCriacao;

    @Column(name = "dt_leitura")
    private Timestamp dtLeitura;
}
