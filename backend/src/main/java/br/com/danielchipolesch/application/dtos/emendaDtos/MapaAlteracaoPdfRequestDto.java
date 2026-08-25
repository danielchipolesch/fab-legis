package br.com.danielchipolesch.application.dtos.emendaDtos;

import java.util.List;

// Payload para a exportação em PDF do Quadro de Justificativas das Modificações
// Propostas (NSCA 5-3, Anexo XXIV). As linhas já vêm com a referência do elemento
// resolvida pelo frontend (que já tem a numeração corrente do documento em memória —
// reimplementar essa numeração no backend duplicaria a lógica que hoje só existe em
// DocumentoFoBuilder.Numbering). O backend só cuida da tipografia/paginação em FOP.
public record MapaAlteracaoPdfRequestDto(
        String docId,
        String ciclo,
        List<Item> itens
) {
    // Cadeia de contexto (ex.: "Art. 12., § 1º") e o elemento que de fato sofreu a
    // alteração, já separados pelo frontend — a coluna Referência dá destaque
    // visual só ao segundo, mantendo o primeiro em tom neutro.
    public record Item(
            String referenciaAncestrais,
            String referenciaAtual,
            String acao,
            String textoAnterior,
            String textoNovo,
            String justificativa
    ) {
    }
}
