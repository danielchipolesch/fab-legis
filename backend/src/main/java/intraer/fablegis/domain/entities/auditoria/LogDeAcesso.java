package intraer.fablegis.domain.entities.auditoria;

import intraer.fablegis.domain.entities.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

// Trilha de auditoria dos ACESSOS (quem concedeu/retirou papel, criou ou desativou usuário...). O alvo é um snapshot, não uma
// FK: o registro tem de continuar fiel ao que aconteceu mesmo que o cadastro mude depois.
@Data
@Entity
@Table(name = "t_log_acesso")
public class LogDeAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log_acesso")
    private Long id;

    // Quem fez a mudança; nulo quando foi o próprio sistema (administrador padrão do primeiro boot).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ator_id")
    private Usuario ator;

    @Column(name = "alvo_id", nullable = false)
    private Long alvoId;

    @Column(name = "alvo_cpf", nullable = false)
    private String alvoCpf;

    @Column(name = "alvo_nome", nullable = false)
    private String alvoNome;

    @Column(name = "om_sigla")
    private String omSigla;

    @Column(name = "sg_acao", nullable = false, columnDefinition = "VARCHAR(30)")
    @Enumerated(EnumType.STRING)
    private AcaoDeAcessoEnum acao;

    @Column(name = "tx_detalhe")
    private String detalhe;

    @Column(name = "dt_ocorrencia", updatable = false)
    @CreationTimestamp
    private Timestamp dtOcorrencia;
}
