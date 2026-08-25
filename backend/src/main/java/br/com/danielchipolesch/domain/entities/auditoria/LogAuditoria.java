package br.com.danielchipolesch.domain.entities.auditoria;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

// Log de acesso/ações a nível de documento (quem viu, quem editou, quando --
// ver LogAuditoriaService). documentoId/documentoDescricao são um snapshot
// denormalizado, não uma FK real para t_documento: documento pode ser
// excluído (ver DocumentoAcessoService.podeExcluir) e a trilha de auditoria
// tem que sobreviver a isso, então nunca referenciamos a linha viva.
@Data
@Entity
@Table(name = "t_log_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log_auditoria")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @Column(name = "documento_descricao", nullable = false)
    private String documentoDescricao;

    @Column(name = "sg_acao", nullable = false, columnDefinition = "VARCHAR(30)")
    @Enumerated(EnumType.STRING)
    private AcaoAuditoriaEnum acao;

    @Column(name = "tx_detalhe")
    private String detalhe;

    @Column(name = "dt_ocorrencia", updatable = false)
    @CreationTimestamp
    private Timestamp dtOcorrencia;
}
