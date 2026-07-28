package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoHistoricoResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoHistorico;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoHistoricoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentoHistoricoService {

    @Autowired
    private DocumentoHistoricoRepository historicoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Documento documento,
                          TipoAlteracaoEnum tipo,
                          String descricao,
                          DocumentoStatusEnum statusAnterior,
                          DocumentoStatusEnum statusNovo) {
        DocumentoHistorico h = new DocumentoHistorico();
        h.setDocumento(documento);
        h.setTipoAlteracao(tipo);
        h.setDescricao(descricao);
        h.setStatusAnterior(statusAnterior);
        h.setStatusNovo(statusNovo);
        historicoRepository.save(h);
    }

    public List<DocumentoHistoricoResponseDto> listarPorDocumento(Long documentoId) {
        return historicoRepository.findByDocumentoIdOrderByDtRegistroDesc(documentoId)
                .stream()
                .map(h -> new DocumentoHistoricoResponseDto(
                        h.getId(),
                        h.getTipoAlteracao(),
                        h.getDescricao(),
                        h.getStatusAnterior(),
                        h.getStatusNovo(),
                        h.getUsuario(),
                        h.getDtRegistro()
                ))
                .toList();
    }
}
