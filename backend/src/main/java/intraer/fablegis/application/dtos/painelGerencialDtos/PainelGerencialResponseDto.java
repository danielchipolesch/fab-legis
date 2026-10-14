package intraer.fablegis.application.dtos.painelGerencialDtos;

import java.time.LocalDate;
import java.util.List;

// Os indicadores do Painel Gerencial (docs/funcionalidades.md). `escopo` diz a OM pela qual tudo foi filtrado (nulo = todas) e se quem
// pergunta pode trocar de OM (`podeFiltrarPorOm`: só o Gestor Geral). Tempos em dias.
public record PainelGerencialResponseDto(
        Escopo escopo,
        Totais totais,
        List<PorEspecie> porEspecie,
        List<PorMes> porMes,
        List<PorSituacao> situacaoAtual,
        Tempos tempos,
        Fila filas,
        List<Gargalo> gargalos,
        List<PorOm> porOm
) {
    public record Escopo(Long omId, String omSigla, boolean podeFiltrarPorOm, LocalDate dataInicio, LocalDate dataFim) {}

    // criados / publicados / revogados / devolvidos: no período; emTramitacao e vigentes: a situação de agora.
    public record Totais(long criados, long publicados, long revogados, long devolvidos, long emTramitacao, long vigentes) {}

    public record PorEspecie(String sigla, long criados) {}

    public record PorMes(String mes, long criados, long publicados) {}

    public record PorSituacao(String situacao, long quantidade) {}

    // Mediana e percentil 90 do tempo de cada etapa (dias), entre os documentos criados no período que a cumpriram; `n` = quantos.
    public record Tempos(Tempo elaboracao, Tempo aprovacao, Tempo publicacao) {}

    public record Tempo(Double mediana, Double p90, long n) {}

    // Quantos esperam agora, por etapa.
    public record Fila(long aguardandoAprovacao, long aguardandoPublicacao) {}

    // O que está parado há mais tempo, na aprovação ou na publicação.
    public record Gargalo(Long documentoId, String identificacao, String omSigla, String etapa, long dias) {}

    public record PorOm(Long omId, String sigla, long criados, long publicados, long emTramitacao) {}
}
