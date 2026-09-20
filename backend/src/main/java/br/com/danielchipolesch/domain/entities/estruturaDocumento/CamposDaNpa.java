package br.com.danielchipolesch.domain.entities.estruturaDocumento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Campos que só a NPA tem, numa linha por documento (ver CamposEspecificosDaEspecie). O que as demais espécies
// guardam em itens da parte preliminar (epígrafe, ementa...) a NPA não tem: seu cabeçalho é uma tabela de campos.
@Entity
@Table(name = "t_documento_npa")
@Data
@NoArgsConstructor
public class CamposDaNpa {

    @Id
    @Column(name = "documento_id")
    private Long documentoId;

    // Setor que emite a NPA -- linha do cabeçalho, abaixo da OM.
    @Column(name = "tx_setor_emissor", nullable = false)
    private String setorEmissor;

    // "Local" do "Local, dd de mês de aaaa" do fecho (a data é a da aprovação).
    @Column(name = "tx_local", nullable = false, length = 120)
    private String local;

    // JSON: [{"rotulo": "Elaborado por", "linhas": ["Nome", "Posto"]}] -- blocos de assinatura em texto livre.
    @Column(name = "tx_assinaturas", nullable = false, columnDefinition = "TEXT")
    private String assinaturas;
}
