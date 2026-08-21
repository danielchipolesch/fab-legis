package br.com.danielchipolesch.application.dtos.emendaDtos;

import lombok.Data;

import java.util.List;

// Payload para a exportação em PDF do Quadro de Justificativas das Modificações
// Propostas (NSCA 5-3, Anexo XXIV). As linhas já vêm com a referência do elemento
// resolvida pelo frontend (que já tem a numeração corrente do documento em memória —
// reimplementar essa numeração no backend duplicaria a lógica que hoje só existe em
// DocumentoFoBuilder.Numbering). O backend só cuida da tipografia/paginação em FOP.
@Data
public class MapaAlteracaoPdfRequestDto {

    private String docId;
    private String ciclo;
    private List<Item> itens;

    @Data
    public static class Item {
        // Cadeia de contexto (ex.: "Art. 12., § 1º") e o elemento que de fato sofreu a
        // alteração, já separados pelo frontend — a coluna Referência dá destaque
        // visual só ao segundo, mantendo o primeiro em tom neutro.
        private String referenciaAncestrais;
        private String referenciaAtual;
        private String acao;
        private String textoAnterior;
        private String textoNovo;
        private String justificativa;
    }
}
