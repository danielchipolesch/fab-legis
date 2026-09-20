package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.application.dtos.anexoDtos.AnexoResponseDto;
import br.com.danielchipolesch.application.dtos.npaDtos.AssinaturaDaNpaDto;
import br.com.danielchipolesch.application.dtos.npaDtos.CamposDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

// Os textos do cabeçalho e do fecho de uma NPA, já resolvidos, para PDF, HTML e prévia mostrarem exatamente o mesmo
// (cada formato só decide como desenhá-los -- ver "Consistência entre formatos" no CLAUDE.md).
//
//   linhasDeCima : Comando, OM e setor emissor (centralizadas, em negrito)
//   identificacao: texto livre informado na criação (fica abaixo do DOM)
//   emissao      : data da aprovação (dd/mm/aaaa) -- espaços em branco enquanto não aprovada
//   efetivacao   : Boletim Interno nº e data da publicação -- só depois de publicada
//   distribuicao : sempre OSTENSIVA
//   assunto      : o título do documento
//   anexos       : "A - Título; B - Título; e C - Título", gerado dos próprios anexos
//   localEData   : "Local, dd de mês de aaaa" do fecho (data da aprovação)
//   publicadaNo  : "(Publicada no Boletim Interno Ostensivo nº __, de __ de ____)" -- só depois de publicada; senão null
public record CabecalhoDaNpa(
        List<String> linhasDeCima,
        String identificacao,
        String emissao,
        String efetivacao,
        String distribuicao,
        String assunto,
        String anexos,
        String localEData,
        List<AssinaturaDaNpaDto> assinaturas,
        String publicadaNo) {

    static final String COMANDO = "COMANDO DA AERONÁUTICA";
    static final String DISTRIBUICAO = "OSTENSIVA";
    static final String SEM_ANEXOS = "NÃO HÁ";
    static final String DATA_EM_BRANCO = "__/__/____";
    static final String EFETIVACAO_PENDENTE = "A ser preenchida na publicação";

    private static final String[] MESES = {"janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};

    public static CabecalhoDaNpa de(Documento doc, CamposDaNpaDto campos, List<AnexoResponseDto> anexos) {
        boolean publicada = doc.getSituacaoBca() != SituacaoBcaEnum.NAO_PUBLICADO;
        String boletim = boletim(doc);
        return new CabecalhoDaNpa(
                List.of(COMANDO, doc.getOm().getNome().toUpperCase(), campos.setorEmissor()),
                doc.getIdentificacao(),
                doc.getDtAprovacao() != null ? dataCurta(doc.getDtAprovacao()) : DATA_EM_BRANCO,
                publicada ? boletim : EFETIVACAO_PENDENTE,
                DISTRIBUICAO,
                doc.getTituloDocumento(),
                listaDeAnexos(anexos),
                campos.local() + ", " + (doc.getDtAprovacao() != null
                        ? dataPorExtenso(doc.getDtAprovacao()) : "___ de __________ de ____"),
                campos.assinaturas() != null ? campos.assinaturas() : List.of(),
                publicada ? "(Publicada no " + boletim + ")" : null);
    }

    // "Boletim Interno Ostensivo nº X, de dd de mês de aaaa". O número e a data ficam nos campos de referência da
    // publicação do documento (os mesmos que um ato normativo usa para o BCA).
    private static String boletim(Documento doc) {
        String numero = doc.getBcaReferencia() != null && !doc.getBcaReferencia().isBlank()
                ? doc.getBcaReferencia().strip() : "__";
        String data = doc.getDtBcaReferencia() != null ? dataPorExtenso(doc.getDtBcaReferencia()) : "__ de ______ de ____";
        return "Boletim Interno Ostensivo nº " + numero + ", de " + data;
    }

    // "A - X; B - Y; e C - Z". Um só anexo: "A - X". Dois: "A - X; e B - Y".
    static String listaDeAnexos(List<AnexoResponseDto> anexos) {
        if (anexos == null || anexos.isEmpty()) return SEM_ANEXOS;
        var ordenados = anexos.stream().sorted(Comparator.comparing(AnexoResponseDto::ordem)).toList();
        var itens = ordenados.stream()
                .map(a -> RotuloDeAnexoDeNpa.letra(a.ordem()) + " - " + a.titulo())
                .toList();
        if (itens.size() == 1) return itens.get(0);
        return String.join("; ", itens.subList(0, itens.size() - 1)) + "; e " + itens.get(itens.size() - 1);
    }

    static String dataCurta(Timestamp ts) {
        LocalDate d = ts.toLocalDateTime().toLocalDate();
        return String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    static String dataPorExtenso(Timestamp ts) {
        LocalDate d = ts.toLocalDateTime().toLocalDate();
        return d.getDayOfMonth() + " de " + MESES[d.getMonthValue() - 1] + " de " + d.getYear();
    }
}
