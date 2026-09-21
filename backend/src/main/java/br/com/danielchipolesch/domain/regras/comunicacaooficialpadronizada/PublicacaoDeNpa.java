package br.com.danielchipolesch.domain.regras.comunicacaooficialpadronizada;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.regras.AcaoDeEtapa;
import br.com.danielchipolesch.domain.regras.RegrasDeRegistroDaPublicacao;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;

// A NPA é publicada e revogada no BOLETIM INTERNO da OM: basta informar o número e a data -- não há portaria, BCA, parte
// preliminar nem PDF de portaria.
//   Publicar: a referência ("Boletim Interno Ostensivo nº 15, de 2 de abril de 2026") vai para os campos de
//             referência da publicação do documento (os mesmos que um ato normativo usa para o BCA), de onde o
//             cabeçalho (EFETIVAÇÃO) e a linha "(Publicada no ...)" a leem.
//   Revogar:  a referência da revogação vai para os campos da NPA (CamposDeNpa) e a da publicação NÃO é sobrescrita: a
//             NPA revogada continua mostrando onde foi publicada.
@Component
public class PublicacaoDeNpa implements RegrasDeRegistroDaPublicacao {

    static final int NUMERO_MAXIMO_DO_BOLETIM = 9999;

    private static final String[] MESES = {"janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};

    private final CamposDeNpa camposDeNpa;

    public PublicacaoDeNpa(CamposDeNpa camposDeNpa) {
        this.camposDeNpa = camposDeNpa;
    }

    @Override
    public void registrar(Documento documento, DocumentoStatusRequestDto pedido, AcaoDeEtapa acao,
                          SituacaoBcaEnum bcaAnterior) {
        Integer numero = pedido.numeroBoletimInterno();
        LocalDate data = pedido.dataBoletimInterno();
        if (numero == null || data == null) {
            throw new StatusCannotBeUpdatedException(
                    "É obrigatório informar o número e a data do Boletim Interno.");
        }
        if (numero < 1 || numero > NUMERO_MAXIMO_DO_BOLETIM) {
            throw new StatusCannotBeUpdatedException(
                    "O número do Boletim Interno deve estar entre 1 e " + NUMERO_MAXIMO_DO_BOLETIM + ".");
        }
        if (acao == AcaoDeEtapa.REVOGAR && documento.getDtBcaReferencia() != null
                && data.isBefore(documento.getDtBcaReferencia().toLocalDateTime().toLocalDate())) {
            throw new StatusCannotBeUpdatedException(
                    "A data do Boletim Interno da revogação não pode ser anterior à da publicação.");
        }

        String referencia = "Boletim Interno Ostensivo nº " + numero + ", de "
                + data.getDayOfMonth() + " de " + MESES[data.getMonthValue() - 1] + " de " + data.getYear();
        if (acao == AcaoDeEtapa.REVOGAR) {
            camposDeNpa.registrarRevogacao(documento.getId(), referencia);
        } else {
            documento.setBcaReferencia(referencia);
            documento.setDtBcaReferencia(Timestamp.valueOf(data.atStartOfDay()));
        }
    }
}
