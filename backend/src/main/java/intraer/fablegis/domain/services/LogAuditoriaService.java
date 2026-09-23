package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.auditoriaDtos.LogAuditoriaResponseDto;
import intraer.fablegis.domain.entities.auditoria.AcaoAuditoriaEnum;
import intraer.fablegis.domain.entities.auditoria.LogAuditoria;
import intraer.fablegis.infrastructure.repositories.LogAuditoriaRepository;
import intraer.fablegis.infrastructure.security.AutenticacaoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(LogAuditoriaService.class);

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    // Nunca lança de verdade: a ação de negócio já foi commitada (própria
    // transação, separada desta) antes de cada chamador chegar aqui -- se a
    // gravação da auditoria falhar (ex.: banco fora do ar num instante ruim),
    // o request não pode virar 500 pro usuário só porque o REGISTRO da ação
    // falhou, quando a ação em si já aconteceu de verdade. Só loga o erro pra
    // não desaparecer silenciosamente.
    @Transactional
    public void registrar(Long documentoId, String documentoDescricao, AcaoAuditoriaEnum acao, String detalhe) {
        try {
            var registro = new LogAuditoria();
            registro.setUsuario(AutenticacaoUtil.usuarioAtual());
            registro.setDocumentoId(documentoId);
            registro.setDocumentoDescricao(documentoDescricao);
            registro.setAcao(acao);
            registro.setDetalhe(detalhe);
            logAuditoriaRepository.save(registro);
        } catch (Exception e) {
            log.error("Falha ao registrar auditoria (documentoId={}, acao={}): {}", documentoId, acao, e.getMessage(), e);
        }
    }

    public Page<LogAuditoriaResponseDto> filtrar(
            Long documentoId, Long usuarioId, AcaoAuditoriaEnum acao,
            Timestamp dataInicio, Timestamp dataFim, Pageable pageable) {
        // Predicate nulo == "sem restrição" pro JPA Criteria API -- substitui
        // Specification.where(null), removido no Spring Data JPA 4/Boot 4.
        Specification<LogAuditoria> spec = (root, query, cb) -> null;
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
