package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.VersaoDocumentoEnum;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;

// Regras de qual versão (vigente x em tramitação) um documento tem e qual é servida por padrão --
// compartilhadas pela exportação em PDF e em HTML, que precisam decidir igual.
public final class VersoesDocumento {

    private VersoesDocumento() {
    }

    // A versão vigente existe desde a primeira publicação (a situação BCA deixa de ser
    // NAO_PUBLICADO) e nunca é substituída por uma etapa interna.
    public static boolean temVersaoVigente(Documento doc) {
        return doc.getSituacaoBca() != SituacaoBcaEnum.NAO_PUBLICADO;
    }

    // Existe versão em tramitação enquanto houver uma etapa local em curso.
    public static boolean temVersaoEmTramitacao(Documento doc) {
        return doc.getSituacaoLocal() != SituacaoLocalEnum.SEM_ETAPA;
    }

    // Só EM_PUBLICACAO e EM_REVOGACAO têm a versão em tramitação armazenada (o texto está
    // congelado); nas demais etapas ela é gerada sob demanda.
    public static boolean emTramitacaoArmazenada(Documento doc) {
        return doc.getSituacaoLocal() == SituacaoLocalEnum.EM_PUBLICACAO
                || doc.getSituacaoLocal() == SituacaoLocalEnum.EM_REVOGACAO;
    }

    // A Portaria (epígrafe, ementa, preâmbulo, fecho e assinatura) só existe a partir da primeira
    // publicação -- é coletada nela (ver DocumentoStatusService) e é perene. Um documento ainda
    // NAO_PUBLICADO (rascunho, minuta, em revisão, aguardando a 1ª publicação) NÃO a exibe, nem na
    // prévia, nem no PDF, nem no HTML; as alterações posteriores aparecem por cláusula em cada
    // elemento, nunca por uma nova portaria na parte preliminar.
    public static boolean exibePortaria(Documento doc) {
        return doc.getSituacaoBca() != SituacaoBcaEnum.NAO_PUBLICADO;
    }

    // Revogação TOTAL: o documento não tem seus elementos tachados -- só ganha o selo vermelho
    // "REVOGADO" no canto superior direito da página da parte preliminar (PDF, HTML e prévia).
    // O selo aparece quando a Situação BCA já é REVOGADO (versão vigente de um ato revogado) e
    // também na versão em tramitação congelada ao aprovar a revogação (EM_REVOGACAO), para o
    // publicador ver exatamente o que vai ser publicado. A revogação PARCIAL é outra coisa: é por
    // elemento, via emenda, e aparece como tachado + cláusula.
    public static boolean exibeSeloRevogado(Documento doc) {
        return doc.getSituacaoBca() == SituacaoBcaEnum.REVOGADO
                || doc.getSituacaoLocal() == SituacaoLocalEnum.EM_REVOGACAO;
    }

    // Padrão: a versão em tramitação, se houver (é o que quem trabalha no documento espera ver);
    // senão, a vigente. Pedir uma versão que o documento não tem é um erro.
    public static VersaoDocumentoEnum resolver(Documento doc, VersaoDocumentoEnum pedida) {
        if (pedida == null) {
            return temVersaoEmTramitacao(doc) ? VersaoDocumentoEnum.TRAMITACAO : VersaoDocumentoEnum.VIGENTE;
        }
        if (pedida == VersaoDocumentoEnum.VIGENTE && !temVersaoVigente(doc)) {
            throw new ResourceNotFoundException("O documento ainda não foi publicado: não há versão vigente.");
        }
        if (pedida == VersaoDocumentoEnum.TRAMITACAO && !temVersaoEmTramitacao(doc)) {
            throw new ResourceNotFoundException("O documento não tem nenhuma etapa em curso: não há versão em tramitação.");
        }
        return pedida;
    }
}
