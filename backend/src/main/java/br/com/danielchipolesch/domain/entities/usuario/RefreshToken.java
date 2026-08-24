package br.com.danielchipolesch.domain.entities.usuario;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "t_refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_refresh_token")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tx_token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "dt_criacao", updatable = false)
    @CreationTimestamp
    private Timestamp dtCriacao;

    @Column(name = "dt_expiracao", nullable = false)
    private Timestamp dtExpiracao;

    @Column(name = "dt_revogacao")
    private Timestamp dtRevogacao;

    public boolean isValido() {
        return dtRevogacao == null && dtExpiracao.after(new Timestamp(System.currentTimeMillis()));
    }
}
