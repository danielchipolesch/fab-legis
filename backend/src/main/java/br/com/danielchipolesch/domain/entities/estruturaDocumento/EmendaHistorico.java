package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import br.com.danielchipolesch.application.dtos.emendaDtos.EmendaAcaoEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_emenda_historico")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmendaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_emenda")
    private Long id;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @Column(name = "secao", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private SecaoDocumentoEnum secao;

    @Column(name = "elemento_id", nullable = false)
    private Long elementoId;

    @Column(name = "acao", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EmendaAcaoEnum acao;

    @Column(name = "tx_conteudo_anterior", columnDefinition = "TEXT")
    private String conteudoAnterior;

    @Column(name = "tx_conteudo_novo", columnDefinition = "TEXT")
    private String conteudoNovo;

    @Column(name = "tx_titulo_anterior")
    private String tituloAnterior;

    @Column(name = "tx_titulo_novo")
    private String tituloNovo;

    @Column(name = "tx_justificativa", columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "dt_emenda", updatable = false)
    @CreationTimestamp
    private LocalDateTime dtEmenda;
}
