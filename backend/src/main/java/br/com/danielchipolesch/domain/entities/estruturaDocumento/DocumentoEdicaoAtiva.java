package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

// Heartbeat de presença: uma linha por (documento, usuário) enquanto o editor
// está aberto, atualizada periodicamente pelo frontend -- ver
// DocumentoPresencaService. Uma linha mais antiga que o limite de atividade é
// tratada como "não está mais editando" na hora da consulta; nada varre a
// tabela em segundo plano nesta fase.
@Data
@Entity
@Table(name = "t_documento_edicao_ativa")
public class DocumentoEdicaoAtiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_edicao_ativa")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "dt_ultimo_heartbeat", nullable = false)
    private Timestamp ultimoHeartbeat;
}
