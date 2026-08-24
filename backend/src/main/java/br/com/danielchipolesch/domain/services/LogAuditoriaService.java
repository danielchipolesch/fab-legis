package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.auditoriaDtos.LogAuditoriaResponseDto;
import br.com.danielchipolesch.domain.entities.auditoria.AcaoAuditoriaEnum;
import br.com.danielchipolesch.domain.entities.auditoria.LogAuditoria;
import br.com.danielchipolesch.infrastructure.repositories.LogAuditoriaRepository;
import br.com.danielchipolesch.infrastructure.security.AutenticacaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

// Ponto único de escrita da trilha de auditoria -- chamado pelos
// controllers logo após cada ação bem-sucedida (ver DocumentoController,
// UsuarioController) para não acoplar o log ao detalhe interno dos
// services. Nunca lança: uma falha ao gravar auditoria não pode derrubar a
// ação de negócio que está sendo auditada.
@Service
public class LogAuditoriaService {

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Transactional
    public void registrar(Long documentoId, String documentoDescricao, AcaoAuditoriaEnum acao, String detalhe) {
        var log = new LogAuditoria();
        log.setUsuario(AutenticacaoUtil.usuarioAtual());
        log.setDocumentoId(documentoId);
        log.setDocumentoDescricao(documentoDescricao);
        log.setAcao(acao);
        log.setDetalhe(detalhe);
        logAuditoriaRepository.save(log);
    }

    public Page<LogAuditoriaResponseDto> filtrar(
            Long documentoId, Long usuarioId, AcaoAuditoriaEnum acao,
            Timestamp dataInicio, Timestamp dataFim, Pageable pageable) {
        Specification<LogAuditoria> spec = Specification.where(null);
        if (documentoId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("documentoId"), documentoId));
        }
        if (usuarioId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId));
        }
        if (acao != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("acao"), acao));
        }
        if (dataInicio != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dtOcorrencia"), dataInicio));
        }
        if (dataFim != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dtOcorrencia"), dataFim));
        }
        return logAuditoriaRepository.findAll(spec, pageable).map(LogAuditoriaResponseDto::from);
    }
}
