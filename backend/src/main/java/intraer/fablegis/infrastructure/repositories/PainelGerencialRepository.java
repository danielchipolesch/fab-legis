package intraer.fablegis.infrastructure.repositories;

import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.Fila;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.Gargalo;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.PorEspecie;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.PorMes;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.PorOm;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.PorSituacao;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.Tempo;
import intraer.fablegis.application.dtos.painelGerencialDtos.PainelGerencialResponseDto.Totais;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

// As consultas agregadas do Painel Gerencial (PainelGerencialService), em SQL: contagens e percentis no banco, nunca em laço em Java.
// Todas aceitam o filtro por OM e por espécie (nulo = todas); as de período usam [inicio, fim) -- o fim já vem como o início do dia seguinte.
@Repository
@RequiredArgsConstructor
public class PainelGerencialRepository {

    // Etapas em que o documento ainda está em trabalho (nunca SEM_ETAPA nem CANCELADO) -- as mesmas de DocumentoSpecifications.
    private static final String EM_TRAMITACAO =
            "'RASCUNHO','MINUTA','EM_ALTERACAO','EM_REVISAO','EM_PUBLICACAO','ANALISE_REVOGACAO','EM_REVOGACAO'";

    private final NamedParameterJdbcTemplate jdbc;

    // Os filtros de OM e espécie, como trecho de WHERE (d = t_documento).
    private static String filtro(Long omId, Long especieId) {
        return (omId != null ? " AND d.om_id = :omId" : "") + (especieId != null ? " AND d.especie_normativa_id = :especieId" : "");
    }

    private static MapSqlParameterSource params(Long omId, Long especieId, Timestamp inicio, Timestamp fim) {
        var p = new MapSqlParameterSource();
        if (omId != null) p.addValue("omId", omId);
        if (especieId != null) p.addValue("especieId", especieId);
        if (inicio != null) p.addValue("inicio", inicio);
        if (fim != null) p.addValue("fim", fim);
        return p;
    }

    private long contar(String sql, MapSqlParameterSource p) {
        Long n = jdbc.queryForObject(sql, p, Long.class);
        return n == null ? 0 : n;
    }

    public Totais totais(Long omId, Long especieId, Timestamp inicio, Timestamp fim) {
        var p = params(omId, especieId, inicio, fim);
        String f = filtro(omId, especieId);
        String base = "SELECT count(*) FROM t_documento d WHERE 1=1" + f;
        long criados = contar(base + " AND d.dt_criacao >= :inicio AND d.dt_criacao < :fim", p);
        long publicados = contar(base + " AND d.dt_publicacao >= :inicio AND d.dt_publicacao < :fim", p);
        long revogados = contar(base + " AND d.dt_revogacao >= :inicio AND d.dt_revogacao < :fim", p);
        // Devolução: voltar para a minuta/alteração a partir da aprovação ou da publicação, no período.
        long devolvidos = contar("SELECT count(*) FROM t_historico_documento h JOIN t_documento d ON d.id_documento = h.documento_id"
                + " WHERE h.sg_tipo_alteracao = 'ALTERACAO_STATUS' AND h.sg_status_anterior IN ('EM_REVISAO','EM_PUBLICACAO')"
                + " AND h.sg_status_novo IN ('MINUTA','EM_ALTERACAO') AND h.dt_registro >= :inicio AND h.dt_registro < :fim" + f, p);
        long emTramitacao = contar(base + " AND d.st_situacao_local IN (" + EM_TRAMITACAO + ")", p);
        long vigentes = contar(base + " AND d.st_situacao_bca = 'PUBLICADO'", p);
        return new Totais(criados, publicados, revogados, devolvidos, emTramitacao, vigentes);
    }

    public List<PorEspecie> porEspecie(Long omId, Long especieId, Timestamp inicio, Timestamp fim) {
        return jdbc.query("SELECT e.sg_especie_normativa AS sigla, count(*) AS n FROM t_documento d"
                        + " JOIN t_especie_normativa e ON e.id_especie_normativa = d.especie_normativa_id"
                        + " WHERE d.dt_criacao >= :inicio AND d.dt_criacao < :fim" + filtro(omId, especieId)
                        + " GROUP BY e.sg_especie_normativa ORDER BY n DESC, sigla", params(omId, especieId, inicio, fim),
                (rs, i) -> new PorEspecie(rs.getString("sigla"), rs.getLong("n")));
    }

    // Criados e publicados por mês, de cada mês do período (inclusive os de zero).
    public List<PorMes> porMes(Long omId, Long especieId, Timestamp inicio, Timestamp fim) {
        String f = filtro(omId, especieId);
        return jdbc.query("SELECT to_char(m.mes, 'YYYY-MM') AS mes,"
                        + " (SELECT count(*) FROM t_documento d WHERE d.dt_criacao >= m.mes AND d.dt_criacao < m.mes + interval '1 month'" + f + ") AS criados,"
                        + " (SELECT count(*) FROM t_documento d WHERE d.dt_publicacao >= m.mes AND d.dt_publicacao < m.mes + interval '1 month'" + f + ") AS publicados"
                        + " FROM generate_series(date_trunc('month', CAST(:inicio AS timestamp)), CAST(:fim AS timestamp) - interval '1 second', interval '1 month') AS m(mes)"
                        + " ORDER BY m.mes", params(omId, especieId, inicio, fim),
                (rs, i) -> new PorMes(rs.getString("mes"), rs.getLong("criados"), rs.getLong("publicados")));
    }

    public List<PorSituacao> situacaoAtual(Long omId, Long especieId) {
        return jdbc.query("SELECT d.st_situacao_local AS situacao, count(*) AS n FROM t_documento d WHERE 1=1" + filtro(omId, especieId)
                        + " GROUP BY d.st_situacao_local ORDER BY n DESC, situacao", params(omId, especieId, null, null),
                (rs, i) -> new PorSituacao(rs.getString("situacao"), rs.getLong("n")));
    }

    // Mediana e percentil 90 (em dias) do intervalo entre duas datas de t_documento, nos documentos criados no período em que as duas existem.
    // `de` e `ate` são nomes de coluna fixos do código (nunca vêm do usuário).
    public Tempo tempo(String de, String ate, Long omId, Long especieId, Timestamp inicio, Timestamp fim) {
        String dias = "EXTRACT(EPOCH FROM (d." + ate + " - d." + de + ")) / 86400.0";
        return jdbc.queryForObject("SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY x.dias) AS mediana,"
                        + " percentile_cont(0.9) WITHIN GROUP (ORDER BY x.dias) AS p90, count(*) AS n FROM"
                        + " (SELECT " + dias + " AS dias FROM t_documento d WHERE d." + de + " IS NOT NULL AND d." + ate + " IS NOT NULL"
                        + " AND d." + ate + " >= d." + de + " AND d.dt_criacao >= :inicio AND d.dt_criacao < :fim" + filtro(omId, especieId) + ") x",
                params(omId, especieId, inicio, fim),
                (rs, i) -> new Tempo(arredondar(rs.getObject("mediana")), arredondar(rs.getObject("p90")), rs.getLong("n")));
    }

    private static Double arredondar(Object valor) {
        return valor == null ? null : Math.round(((Number) valor).doubleValue() * 10.0) / 10.0;
    }

    public Fila filas(Long omId, Long especieId) {
        var p = params(omId, especieId, null, null);
        String base = "SELECT count(*) FROM t_documento d WHERE 1=1" + filtro(omId, especieId);
        return new Fila(contar(base + " AND d.st_situacao_local IN ('EM_REVISAO','ANALISE_REVOGACAO')", p),
                contar(base + " AND d.st_situacao_local IN ('EM_PUBLICACAO','EM_REVOGACAO')", p));
    }

    // Os `limite` documentos parados há mais tempo na aprovação ou na publicação (desde a entrada na etapa).
    public List<Gargalo> gargalos(Long omId, Long especieId, int limite) {
        var p = params(omId, especieId, null, null);
        p.addValue("limite", limite);
        return jdbc.query("SELECT d.id_documento AS id, d.tx_identificacao AS identificacao, o.sg_organizacao_militar AS om,"
                        + " CASE WHEN d.st_situacao_local IN ('EM_REVISAO','ANALISE_REVOGACAO') THEN 'APROVACAO' ELSE 'PUBLICACAO' END AS etapa,"
                        + " CAST(floor(EXTRACT(EPOCH FROM (now() - COALESCE(CASE d.st_situacao_local WHEN 'EM_REVISAO' THEN d.dt_em_revisao"
                        + " WHEN 'ANALISE_REVOGACAO' THEN d.dt_analise_revogacao WHEN 'EM_PUBLICACAO' THEN d.dt_em_publicacao"
                        + " WHEN 'EM_REVOGACAO' THEN d.dt_em_revogacao END, d.dt_alteracao, d.dt_criacao))) / 86400) AS bigint) AS dias"
                        + " FROM t_documento d JOIN t_organizacao_militar o ON o.id_organizacao_militar = d.om_id"
                        + " WHERE d.st_situacao_local IN ('EM_REVISAO','ANALISE_REVOGACAO','EM_PUBLICACAO','EM_REVOGACAO')" + filtro(omId, especieId)
                        + " ORDER BY dias DESC, d.id_documento LIMIT :limite", p,
                (rs, i) -> new Gargalo(rs.getLong("id"), rs.getString("identificacao"), rs.getString("om"), rs.getString("etapa"), rs.getLong("dias")));
    }

    // Por OM (a visão geral): criados e publicados no período e o que está em tramitação agora. Só as OMs com algum movimento.
    public List<PorOm> porOm(Long omId, Long especieId, Timestamp inicio, Timestamp fim, int limite) {
        var p = params(omId, especieId, inicio, fim);
        p.addValue("limite", limite);
        String criados = "count(*) FILTER (WHERE d.dt_criacao >= :inicio AND d.dt_criacao < :fim)";
        String emTramitacao = "count(*) FILTER (WHERE d.st_situacao_local IN (" + EM_TRAMITACAO + "))";
        return jdbc.query("SELECT o.id_organizacao_militar AS id, o.sg_organizacao_militar AS sigla,"
                        + " " + criados + " AS criados,"
                        + " count(*) FILTER (WHERE d.dt_publicacao >= :inicio AND d.dt_publicacao < :fim) AS publicados,"
                        + " " + emTramitacao + " AS em_tramitacao"
                        + " FROM t_documento d JOIN t_organizacao_militar o ON o.id_organizacao_militar = d.om_id WHERE 1=1" + filtro(omId, especieId)
                        + " GROUP BY o.id_organizacao_militar, o.sg_organizacao_militar"
                        + " HAVING " + criados + " > 0 OR " + emTramitacao + " > 0"
                        + " ORDER BY criados DESC, sigla LIMIT :limite", p,
                (rs, i) -> new PorOm(rs.getLong("id"), rs.getString("sigla"), rs.getLong("criados"), rs.getLong("publicados"), rs.getLong("em_tramitacao")));
    }
}
