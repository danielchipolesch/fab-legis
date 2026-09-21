package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.SecaoItemRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SecaoDocumentoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoPortariaPublicacaoEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.regras.AcaoDeEtapa;
import br.com.danielchipolesch.domain.regras.RegrasDeRegistroDaPublicacao;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

// O registro da publicação (ou revogação) de um ATO NORMATIVO: portaria + BCA, e na primeira publicação a parte
// preliminar (epígrafe, ementa, preâmbulo, fecho e assinatura). Fica em domain.services porque grava a parte
// preliminar por um método de DocumentoParteNormativaService que só este pacote enxerga.
@Component
public class PublicacaoDeEspecieConvencional implements RegrasDeRegistroDaPublicacao {

    private final PortariaPublicacaoService portariaPublicacaoService;
    private final DocumentoParteNormativaService documentoParteNormativaService;
    private final EmendaService emendaService;

    // @Lazy: DocumentoParteNormativaService depende de RegrasDasEspecies, que depende (por aqui) deste registro --
    // sem o proxy preguiçoso os beans formam um ciclo e a aplicação não sobe.
    public PublicacaoDeEspecieConvencional(PortariaPublicacaoService portariaPublicacaoService,
                                    @Lazy DocumentoParteNormativaService documentoParteNormativaService,
                                    @Lazy EmendaService emendaService) {
        this.portariaPublicacaoService = portariaPublicacaoService;
        this.documentoParteNormativaService = documentoParteNormativaService;
        this.emendaService = emendaService;
    }

    @Override
    public void registrar(Documento documento, DocumentoStatusRequestDto pedido, AcaoDeEtapa acao,
                          SituacaoBcaEnum bcaAnterior) {
        boolean primeiraPublicacao = acao == AcaoDeEtapa.PUBLICAR && bcaAnterior == SituacaoBcaEnum.NAO_PUBLICADO;
        boolean alterando = acao == AcaoDeEtapa.PUBLICAR && bcaAnterior == SituacaoBcaEnum.PUBLICADO;
        boolean revogando = acao == AcaoDeEtapa.REVOGAR;
        registrarPortariaEBca(documento, pedido, primeiraPublicacao, alterando, revogando);
    }

    // Valida e registra a portaria + BCA (publicação ou revogação). A parte preliminar
    // (epígrafe/ementa/preâmbulo/fecho/assinatura) é coletada e gravada SÓ na primeira
    // publicação: a portaria de publicação é a única que aparece nela e nunca é substituída.
    // Alteração e revogação exigem apenas portaria (órgão, setor, número, data), BCA e o PDF, e
    // aparecem por cláusula nos elementos (alteração) ou pelo selo REVOGADO (revogação total).
    private void registrarPortariaEBca(Documento documento, DocumentoStatusRequestDto request,
                                       boolean primeiraPublicacao, boolean alterando, boolean revogando) {
        String orgaoPortaria = request.orgaoPortaria();
        String setorPortaria = request.setorPortaria();
        String numeroPortaria = request.numeroPortaria();
        LocalDate dataPortaria = request.dataPortaria();
        Integer numeroBca = request.numeroBca();
        LocalDate dataBca = request.dataBca();

        if (isBlank(orgaoPortaria) || isBlank(setorPortaria) || isBlank(numeroPortaria) || dataPortaria == null
                || numeroBca == null || dataBca == null || isBlank(request.portariaPdfUrl())) {
            throw new StatusCannotBeUpdatedException(
                    "É obrigatório informar a portaria, o BCA de referência e o PDF da portaria.");
        }
        if (primeiraPublicacao && (isBlank(request.epigrafe()) || isBlank(request.ementa()) || isBlank(request.preambulo())
                || isBlank(request.fecho()) || isBlank(request.assinatura()))) {
            throw new StatusCannotBeUpdatedException(
                    "Para publicar um documento pela primeira vez é obrigatório informar epígrafe, ementa, "
                    + "preâmbulo, fecho e assinatura.");
        }
        // O BCA é publicado apenas em dias úteis, então nunca passa de 366 (dias do ano).
        if (numeroBca < 1 || numeroBca > 366) {
            throw new StatusCannotBeUpdatedException("O número do BCA deve estar entre 1 e 366.");
        }
        if (documento.getDtPortariaReferencia() != null
                && dataPortaria.isBefore(documento.getDtPortariaReferencia().toLocalDateTime().toLocalDate())) {
            throw new StatusCannotBeUpdatedException(
                    "A data da portaria não pode ser anterior à da alteração anterior.");
        }
        if (documento.getDtBcaReferencia() != null
                && dataBca.isBefore(documento.getDtBcaReferencia().toLocalDateTime().toLocalDate())) {
            throw new StatusCannotBeUpdatedException(
                    "A data do BCA não pode ser anterior à da alteração anterior.");
        }

        String orgaoSetor = orgaoPortaria.strip() + "/" + setorPortaria.strip();
        documento.setPortariaReferencia("Portaria " + orgaoSetor + " n° " + numeroPortaria.strip()
                + ", de " + formatarDataPorExtenso(dataPortaria));
        documento.setBcaReferencia("BCA n° " + numeroBca + ", de " + formatarDataPorExtenso(dataBca));
        documento.setDtPortariaReferencia(Timestamp.valueOf(dataPortaria.atStartOfDay()));
        documento.setDtBcaReferencia(Timestamp.valueOf(dataBca.atStartOfDay()));

        // Tipo da portaria: revogação é sempre REVOGACAO; publicar um documento já PUBLICADO é
        // uma alteração (numerada automaticamente); publicar pela primeira vez é a edição original.
        TipoPortariaPublicacaoEnum tipoPortaria = revogando ? TipoPortariaPublicacaoEnum.REVOGACAO
                : (alterando ? TipoPortariaPublicacaoEnum.ALTERACAO : TipoPortariaPublicacaoEnum.EDICAO);
        portariaPublicacaoService.registrar(documento, tipoPortaria, orgaoPortaria, setorPortaria,
                numeroPortaria, dataPortaria, numeroBca, dataBca, request.portariaPdfUrl());

        if (primeiraPublicacao) {
            // A parte preliminar só passa a existir de fato com a primeira publicação -- por isso
            // é coletada aqui, não durante a edição (ver Documento.java). Mesma lógica de "apaga
            // tudo e recria" de DocumentoParteNormativaService.salvarSecoes, só que agora só roda
            // aqui, uma única vez.
            documentoParteNormativaService.salvarItensPreliminares(documento, List.of(
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.EPIGRAFE, 1, null, request.epigrafe(), null, null),
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.EMENTA, 2, null, request.ementa(), null, null),
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.PREAMBULO, 3, null, request.preambulo(), null, null),
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.FECHO, 4, null, request.fecho(), null, null),
                    new SecaoItemRequestDto(null, SecaoDocumentoEnum.PARTE_PRELIMINAR, ItemAnexoParteNormativaTipoEnum.ASSINATURA, 5, null, request.assinatura(), null, null)
            ));
        }
        if (alterando) {
            // Só há emendas pendentes a consolidar numa ALTERAÇÃO publicada; a primeira publicação
            // nunca passou por EM_ALTERACAO e a revogação não consolida nada.
            emendaService.consolidarPublicacao(documento.getId(), documento.getPortariaReferencia(), documento.getBcaReferencia());
        }
    }


    private static final String[] MESES = {
            "janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    private static String formatarDataPorExtenso(LocalDate data) {
        return data.getDayOfMonth() + " de " + MESES[data.getMonthValue() - 1] + " de " + data.getYear();
    }

    private static boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
