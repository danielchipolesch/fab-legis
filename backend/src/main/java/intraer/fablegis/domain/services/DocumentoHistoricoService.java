package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.documentoDtos.DocumentoHistoricoResponseDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.DocumentoHistorico;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import intraer.fablegis.infrastructure.repositories.DocumentoHistoricoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentoHistoricoService {

    @Autowired
    private DocumentoHistoricoRepository historicoRepository;

    @Transactional
    public void registrar(Documento documento,
                          TipoAlteracaoEnum tipo,
                          String descricao,
                          SituacaoLocalEnum statusAnterior,
                          SituacaoLocalEnum statusNovo) {
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
