package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.PortariaPublicacaoResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.PortariaPublicacao;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoPortariaPublicacaoEnum;
import br.com.danielchipolesch.infrastructure.repositories.PortariaPublicacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PortariaPublicacaoService {

    @Autowired
    private PortariaPublicacaoRepository portariaPublicacaoRepository;

    public List<PortariaPublicacaoResponseDto> listar(Long documentoId) {
        return portariaPublicacaoRepository.findByDocumentoIdOrderByDtCriacaoAsc(documentoId).stream()
                .map(PortariaPublicacaoResponseDto::from)
                .toList();
    }

    // Chamado por DocumentoStatusService a cada transição que exige portaria
    // (publicar, republicar, revogar) -- numeroSequencial só é calculado para
    // ALTERACAO, já que EDICAO e REVOGACAO só acontecem uma vez por documento.
    public void registrar(Documento documento, TipoPortariaPublicacaoEnum tipo, String orgao, String setor,
                           String numeroPortaria, LocalDate dataPortaria, Integer numeroBca, LocalDate dataBca,
                           String urlPdf) {
        var portaria = new PortariaPublicacao();
        portaria.setDocumento(documento);
        portaria.setTipo(tipo);
        if (tipo == TipoPortariaPublicacaoEnum.ALTERACAO) {
            portaria.setNumeroSequencial(
                    portariaPublicacaoRepository.findMaxNumeroSequencialAlteracao(documento.getId()) + 1);
        }
        portaria.setOrgao(orgao);
        portaria.setSetor(setor);
        portaria.setNumeroPortaria(numeroPortaria);
        portaria.setDataPortaria(dataPortaria);
        portaria.setNumeroBca(numeroBca);
        portaria.setDataBca(dataBca);
        portaria.setUrlPdf(urlPdf);
        portariaPublicacaoRepository.save(portaria);
    }
}
