package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.Escopo;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.Tempos;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.regras.EscopoDoPainel;
import intraer.fablegis.domain.regras.RegrasDasEspecies;
import intraer.fablegis.domain.regras.TipoDeEspecie;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.PainelGerencialRepository;
import intraer.fablegis.infrastructure.repositories.PainelGerencialRepository.Filtro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

// Painel Gerencial (docs/funcionalidades.md): indicadores da gestão normativa, calculados na hora a partir do que o sistema já registra
// (documentos, datas de cada etapa e histórico). Quem vê o quê é EscopoDoPainel: o Painel Gerencial Geral, todas as OMs com filtro; o Painel Gerencial da
// OM, só a própria, sempre.
@Service
@RequiredArgsConstructor
public class PainelGerencialService {

    static final int GARGALOS_EXIBIDOS = 10;
    static final int OMS_EXIBIDAS = 15;
    static final int DIAS_DO_PERIODO_PADRAO = 365;

    private final PainelGerencialRepository repositorio;
    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;
    private final RegrasDasEspecies regras;

    // Sem datas, os últimos 365 dias até hoje. O fim é inclusivo (o dia inteiro).
    @Transactional(readOnly = true)
    public PainelGerencialResponseDto indicadores(Usuario usuario, Long omPedida, Long especieId, TipoDeEspecie tipoPedido,
                                                  LocalDate dataInicio, LocalDate dataFim) {
        Long omId = EscopoDoPainel.omDosIndicadores(usuario, omPedida);
        Set<TipoDeEspecie> tipos = EscopoDoPainel.tiposDosIndicadores(usuario, tipoPedido, regras.tipos(true));
        var filtro = new Filtro(omId, especieId, tipos.stream().map(Enum::name).toList());
        LocalDate fimDoPeriodo = dataFim != null ? dataFim : LocalDate.now();
        LocalDate inicioDoPeriodo = dataInicio != null ? dataInicio : fimDoPeriodo.minusDays(DIAS_DO_PERIODO_PADRAO);
        if (inicioDoPeriodo.isAfter(fimDoPeriodo)) {
            throw new InvalidInputException("A data inicial não pode ser depois da final.");
        }
        Timestamp inicio = Timestamp.valueOf(inicioDoPeriodo.atStartOfDay());
        Timestamp fim = Timestamp.valueOf(fimDoPeriodo.plusDays(1).atTime(LocalTime.MIN));

        String sigla = omId == null ? null : organizacaoMilitarRepository.findById(omId).map(om -> om.getSigla()).orElse(null);
        boolean geral = EscopoDoPainel.ehGeral(usuario);
        var escopo = new Escopo(omId, sigla, geral, !geral, tipoPedido == null ? null : tipoPedido.name(), inicioDoPeriodo, fimDoPeriodo);
        var tempos = new Tempos(
                repositorio.tempo("dt_criacao", "dt_em_revisao", filtro, inicio, fim),
                repositorio.tempo("dt_em_revisao", "dt_aprovacao", filtro, inicio, fim),
                repositorio.tempo("dt_em_publicacao", "dt_publicacao", filtro, inicio, fim));
        return new PainelGerencialResponseDto(
                escopo,
                repositorio.totais(filtro, inicio, fim),
                repositorio.porEspecie(filtro, inicio, fim),
                repositorio.porMes(filtro, inicio, fim),
                repositorio.situacaoAtual(filtro),
                tempos,
                repositorio.filas(filtro),
                repositorio.gargalos(filtro, GARGALOS_EXIBIDOS),
                repositorio.porOm(filtro, inicio, fim, OMS_EXIBIDAS));
    }
}
