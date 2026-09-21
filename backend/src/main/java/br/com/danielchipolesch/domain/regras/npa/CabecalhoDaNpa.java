package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.npaDtos.AssinaturaDaNpaDto;
import br.com.danielchipolesch.application.dtos.npaDtos.CamposDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

// Os textos do cabeçalho e do fecho de uma NPA, já resolvidos, para PDF, HTML e prévia mostrarem exatamente o mesmo
// (cada formato só decide como desenhá-los -- ver "Consistência entre formatos" no CLAUDE.md).
//
//   linhasDeCima : Comando, OM e setor emissor (centralizadas, em negrito)
//   identificacao: texto livre informado na criação (fica abaixo do DOM)
//   emissao      : data da aprovação no formato militar ("08 NOV 2026") -- espaços em branco enquanto não aprovada
//   efetivacao   : duas linhas, como no modelo: "BIO 15" e a data do Boletim ("02 ABR 2026") -- em branco enquanto
//                  não publicada
//   distribuicao : sempre OSTENSIVA
//   assunto      : o título do documento
//   anexos       : uma linha por anexo ("A - Título;", "B - Título; e", "C - Título."), gerada dos próprios anexos
//   assinaturas  : "Elaborado por" (autor e coautores), os blocos escritos pelo autor (Visto, Proposto por...) e
//                  "Aprovado por" (quem aprova) -- nessa ordem; os dois de pontas saem do documento, não são digitados
//   localEData   : "Local, dd de mês de aaaa" do fecho (data da aprovação)
//   publicadaNo  : "(Publicada no Boletim Interno Ostensivo nº __, de __ de ____)" -- só depois de publicada; senão null
//   revogadaNo   : "(Revogada pelo Boletim Interno Ostensivo nº __, de __ de ____)" -- só depois de revogada; senão null
public record CabecalhoDaNpa(
        List<String> linhasDeCima,
        String identificacao,
        String emissao,
        List<String> efetivacao,
        String distribuicao,
        String assunto,
        List<String> anexos,
        String localEData,
        List<AssinaturaDaNpaDto> assinaturas,
        String publicadaNo,
        String revogadaNo) {

    static final String COMANDO = "COMANDO DA AERONÁUTICA";
    static final String DISTRIBUICAO = "OSTENSIVA";
    static final String SEM_ANEXOS = "NÃO HÁ";
    static final String DATA_EM_BRANCO = "__ ___ ____";
    static final String ELABORADO_POR = "Elaborado por";
    static final String APROVADO_POR = "Aprovado por";
    static final String ELABORADOR_EM_BRANCO = "[Nome completo, posto e função]";
    static final String APROVADOR_EM_BRANCO = "[POSTO] FULANO DE TAL";

    // O número do Boletim dentro da referência gravada por PublicacaoDeNpa ("Boletim Interno Ostensivo nº 15, de ...").
    private static final Pattern NUMERO_DO_BOLETIM = Pattern.compile("nº\\s*(\\d+)");

    private static final String[] MESES_ABREVIADOS = {"JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"};

    private static final String[] MESES = {"janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};

    public static CabecalhoDaNpa de(Documento doc, CamposDaNpaDto campos, List<AnexoResponseDto> anexos) {
        boolean publicada = doc.getSituacaoBca() != SituacaoBcaEnum.NAO_PUBLICADO;
        String boletim = boletim(doc);
        return new CabecalhoDaNpa(
                List.of(COMANDO, doc.getOm().getNome().toUpperCase(), campos.setorEmissor()),
                doc.getIdentificacao(),
                doc.getDtAprovacao() != null ? dataMilitar(doc.getDtAprovacao()) : DATA_EM_BRANCO,
                efetivacao(doc, publicada),
                DISTRIBUICAO,
                doc.getTituloDocumento(),
                listaDeAnexos(anexos),
                campos.local() + ", " + (doc.getDtAprovacao() != null
                        ? dataPorExtenso(doc.getDtAprovacao()) : "___ de __________ de ____"),
                assinaturas(campos),
                publicada ? "(Publicada no " + boletim + ")" : null,
                campos.boletimDaRevogacao() != null && !campos.boletimDaRevogacao().isBlank()
                        ? "(Revogada pelo " + campos.boletimDaRevogacao().strip() + ")" : null);
    }

    // "Boletim Interno Ostensivo nº X, de dd de mês de aaaa": a referência oficial da publicação, gravada pronta no campo
    // que um ato normativo usa para o BCA (ver PublicacaoDeNpa). Em branco só se a NPA foi publicada sem ela.
    private static String boletim(Documento doc) {
        return doc.getBcaReferencia() != null && !doc.getBcaReferencia().isBlank()
                ? doc.getBcaReferencia().strip()
                : "Boletim Interno Ostensivo nº __, de __ de ______ de ____";
    }

    // Elaborado por (todos os autores), os blocos escritos, Aprovado por (quem aprova). Sem ninguém ainda (documento não
    // enviado para revisão), a linha de orientação entre colchetes, como nos demais campos que ainda não têm valor.
    static List<AssinaturaDaNpaDto> assinaturas(CamposDaNpaDto campos) {
        var todas = new ArrayList<AssinaturaDaNpaDto>();
        todas.add(new AssinaturaDaNpaDto(ELABORADO_POR, comValor(campos.elaboradoPor(), ELABORADOR_EM_BRANCO)));
        if (campos.assinaturas() != null) todas.addAll(campos.assinaturas());
        todas.add(new AssinaturaDaNpaDto(APROVADO_POR, comValor(campos.aprovadoPor(), APROVADOR_EM_BRANCO)));
        return comDoisPontos(todas);
    }

    private static List<String> comValor(List<String> linhas, String orientacao) {
        return linhas == null || linhas.isEmpty() ? List.of(orientacao) : linhas;
    }

    // "Elaborado por" e "Aprovado por" (com ou sem dois-pontos, em qualquer caixa) são dos blocos automáticos.
    static boolean rotuloReservado(String rotulo) {
        if (rotulo == null) return false;
        String r = rotulo.strip().replaceAll(":$", "").strip();
        return r.equalsIgnoreCase(ELABORADO_POR) || r.equalsIgnoreCase(APROVADO_POR);
    }

    // O rótulo do bloco de assinatura leva dois-pontos ("Elaborado por:"), como no modelo, mesmo que o autor não os tenha digitado.
    static List<AssinaturaDaNpaDto> comDoisPontos(List<AssinaturaDaNpaDto> assinaturas) {
        if (assinaturas == null) return List.of();
        return assinaturas.stream()
                .map(a -> a.rotulo().stripTrailing().endsWith(":") ? a
                        : new AssinaturaDaNpaDto(a.rotulo().stripTrailing() + ":", a.linhas()))
                .toList();
    }

    // As duas linhas da célula EFETIVAÇÃO: "BIO 15" e "02 ABR 2026" (só depois de publicada; senão, em branco).
    private static List<String> efetivacao(Documento doc, boolean publicada) {
        if (!publicada) return List.of("BIO __", DATA_EM_BRANCO);
        var m = doc.getBcaReferencia() != null ? NUMERO_DO_BOLETIM.matcher(doc.getBcaReferencia()) : null;
        String numero = m != null && m.find() ? m.group(1) : "__";
        return List.of("BIO " + numero, doc.getDtBcaReferencia() != null ? dataMilitar(doc.getDtBcaReferencia()) : DATA_EM_BRANCO);
    }

    // Uma linha por anexo, na ordem: "A - X;", "B - Y; e", "C - Z." (um só: "A - X."). Sem anexos: "NÃO HÁ".
    static List<String> listaDeAnexos(List<AnexoResponseDto> anexos) {
        if (anexos == null || anexos.isEmpty()) return List.of(SEM_ANEXOS);
        var ordenados = anexos.stream().sorted(Comparator.comparing(AnexoResponseDto::ordem)).toList();
        var linhas = new ArrayList<String>();
        for (int i = 0; i < ordenados.size(); i++) {
            var a = ordenados.get(i);
            String fim = i == ordenados.size() - 1 ? "." : (i == ordenados.size() - 2 ? "; e" : ";");
            linhas.add(RotuloDeAnexoDeNpa.letra(a.ordem()) + " - " + a.titulo() + fim);
        }
        return linhas;
    }

    // Formato militar da data: "08 NOV 2026".
    static String dataMilitar(Timestamp ts) {
        LocalDate d = ts.toLocalDateTime().toLocalDate();
        return String.format("%02d %s %d", d.getDayOfMonth(), MESES_ABREVIADOS[d.getMonthValue() - 1], d.getYear());
    }

    static String dataPorExtenso(Timestamp ts) {
        LocalDate d = ts.toLocalDateTime().toLocalDate();
        return d.getDayOfMonth() + " de " + MESES[d.getMonthValue() - 1] + " de " + d.getYear();
    }
}
