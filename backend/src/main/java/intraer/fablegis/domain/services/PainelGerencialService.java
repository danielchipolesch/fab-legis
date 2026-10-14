package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.Escopo;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.Tempos;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.regras.EscopoDoPainel;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import intraer.fablegis.infrastructure.repositories.PainelGerencialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;

// Painel Gerencial (docs/funcionalidades.md): indicadores da gestão normativa, calculados na hora a partir do que o sistema já registra
// (documentos, datas de cada etapa e histórico). Quem vê o quê é EscopoDoPainel: o Gestor Geral, todas as OMs com filtro; o Gestor da
// OM, só a própria, sempre.
@Service
@RequiredArgsConstructor
public class PainelGerencialService {

    static final int GARGALOS_EXIBIDOS = 10;
    static final int OMS_EXIBIDAS = 15;
    static final int DIAS_DO_PERIODO_PADRAO = 365;

    private final PainelGerencialRepository repositorio;
    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;

    // Sem datas, os últimos 365 dias até hoje. O fim é inclusivo (o dia inteiro).
    @Transactional(readOnly = true)
    public PainelGerencialResponseDto indicadores(Usuario usuario, Long omPedida, Long especieId, LocalDate dataInicio, LocalDate dataFim) {
        Long omId = EscopoDoPainel.omDosIndicadores(usuario, omPedida);
        LocalDate fimDoPeriodo = dataFim != null ? dataFim : LocalDate.now();
        LocalDate inicioDoPeriodo = dataInicio != null ? dataInicio : fimDoPeriodo.minusDays(DIAS_DO_PERIODO_PADRAO);
        if (inicioDoPeriodo.isAfter(fimDoPeriodo)) {
            throw new InvalidInputException("A data inicial não pode ser depois da final.");
        }
        Timestamp inicio = Timestamp.valueOf(inicioDoPeriodo.atStartOfDay());
        Timestamp fim = Timestamp.valueOf(fimDoPeriodo.plusDays(1).atTime(LocalTime.MIN));

        String sigla = omId == null ? null : organizacaoMilitarRepository.findById(omId).map(om -> om.getSigla()).orElse(null);
        var escopo = new Escopo(omId, sigla, EscopoDoPainel.ehGeral(usuario), inicioDoPeriodo, fimDoPeriodo);
        var tempos = new Tempos(
                repositorio.tempo("dt_criacao", "dt_em_revisao", omId, especieId, inicio, fim),
                repositorio.tempo("dt_em_revisao", "dt_aprovacao", omId, especieId, inicio, fim),
                repositorio.tempo("dt_em_publicacao", "dt_publicacao", omId, especieId, inicio, fim));
        return new PainelGerencialResponseDto(
                escopo,
                repositorio.totais(omId, especieId, inicio, fim),
                repositorio.porEspecie(omId, especieId, inicio, fim),
                repositorio.porMes(omId, especieId, inicio, fim),
                repositorio.situacaoAtual(omId, especieId),
                tempos,
                repositorio.filas(omId, especieId),
                repositorio.gargalos(omId, especieId, GARGALOS_EXIBIDOS),
                repositorio.porOm(omId, especieId, inicio, fim, OMS_EXIBIDAS));
    }
}
