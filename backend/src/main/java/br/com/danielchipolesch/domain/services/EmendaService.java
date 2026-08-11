package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.emendaDtos.EmendaAcaoEnum;
import br.com.danielchipolesch.application.dtos.emendaDtos.EmendaElementoRequestDto;
import br.com.danielchipolesch.application.dtos.emendaDtos.EmendaIncluirRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.*;
import br.com.danielchipolesch.infrastructure.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmendaService {

    private static final String DOC_NAO_ENCONTRADO   = "Documento não encontrado";
    private static final String ELEM_NAO_ENCONTRADO  = "Elemento não encontrado";
    private static final String DOC_NAO_EM_ALTERACAO =
            "O documento não está em status EM_ALTERACAO. Inicie uma alteração antes de emendar.";
    private static final String JUSTIFICATIVA_REQUERIDA =
            "Justificativa é obrigatória para alterar ou revogar um elemento.";

    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private ItemAnexoParteNormativaRepository normativaRepository;
    @Autowired private ItemPartePreliminarRepository preliminarRepository;
    @Autowired private ItemParteFinalRepository finalRepository;
    @Autowired private EmendaHistoricoRepository historicoRepository;

    // ─── Emendar elemento existente ───────────────────────────────────────────────

    @Transactional
    public void emendar(Long docId, String secao, Long elementoId, EmendaElementoRequestDto req) {
        carregarEmAlteracao(docId);
        SecaoDocumentoEnum secaoEnum = SecaoDocumentoEnum.valueOf(secao.toUpperCase());

        switch (secaoEnum) {
            case PARTE_PRELIMINAR -> emendar(docId, secaoEnum, elementoId, req,
                    preliminarRepository.findById(elementoId)
                            .filter(e -> e.getDocumento().getId().equals(docId))
                            .orElseThrow(() -> new RuntimeException(ELEM_NAO_ENCONTRADO)));
            case PARTE_NORMATIVA  -> emendar(docId, secaoEnum, elementoId, req,
                    normativaRepository.findById(elementoId)
                            .filter(e -> e.getDocumento().getId().equals(docId))
                            .orElseThrow(() -> new RuntimeException(ELEM_NAO_ENCONTRADO)));
            case PARTE_FINAL      -> emendar(docId, secaoEnum, elementoId, req,
                    finalRepository.findById(elementoId)
                            .filter(e -> e.getDocumento().getId().equals(docId))
                            .orElseThrow(() -> new RuntimeException(ELEM_NAO_ENCONTRADO)));
        }
    }

    private void emendar(Long docId, SecaoDocumentoEnum secao, Long elementoId,
                          EmendaElementoRequestDto req, ItemPartePreliminar item) {
        EmendaAcaoEnum acao = req.getAcao();

        if (acao == EmendaAcaoEnum.DESFAZER) {
            if (item.getEmendaStatus() == ElementoEmendaStatusEnum.INCLUIDO) {
                registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.DESFAZER,
                        item.getConteudo(), null, item.getTitulo(), null, null);
                preliminarRepository.delete(item);
                return;
            }
            registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.DESFAZER,
                    item.getConteudoEmenda(), null, item.getTituloEmenda(), null, null);
            item.setConteudoEmenda(null);
            item.setTituloEmenda(null);
            item.setJustificativaEmenda(null);
            item.setEmendaStatus(ElementoEmendaStatusEnum.INALTERADO);
            preliminarRepository.save(item);
            return;
        }

        validarJustificativa(req.getJustificativa());

        if (acao == EmendaAcaoEnum.ALTERAR) {
            registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.ALTERAR,
                    item.getConteudo(), req.getNovoConteudo(),
                    item.getTitulo(), req.getNovoTitulo(), req.getJustificativa());
            item.setConteudoEmenda(req.getNovoConteudo());
            if (req.getNovoTitulo() != null) item.setTituloEmenda(req.getNovoTitulo());
            item.setEmendaStatus(ElementoEmendaStatusEnum.ALTERADO);
        } else {
            registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.REVOGAR,
                    item.getConteudo(), null, item.getTitulo(), null, req.getJustificativa());
            item.setEmendaStatus(ElementoEmendaStatusEnum.REVOGADO);
        }
        item.setJustificativaEmenda(req.getJustificativa());
        preliminarRepository.save(item);
    }

    private void emendar(Long docId, SecaoDocumentoEnum secao, Long elementoId,
                          EmendaElementoRequestDto req, ItemAnexoParteNormativa item) {
        EmendaAcaoEnum acao = req.getAcao();

        if (acao == EmendaAcaoEnum.DESFAZER) {
            if (item.getEmendaStatus() == ElementoEmendaStatusEnum.INCLUIDO) {
                registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.DESFAZER,
                        item.getConteudo(), null, item.getTitulo(), null, null);
                normativaRepository.delete(item);
                return;
            }
            registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.DESFAZER,
                    item.getConteudoEmenda(), null, item.getTituloEmenda(), null, null);
            item.setConteudoEmenda(null);
            item.setTituloEmenda(null);
            item.setJustificativaEmenda(null);
            item.setEmendaStatus(ElementoEmendaStatusEnum.INALTERADO);
            normativaRepository.save(item);
            return;
        }

        validarJustificativa(req.getJustificativa());

        if (acao == EmendaAcaoEnum.ALTERAR) {
            registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.ALTERAR,
                    item.getConteudo(), req.getNovoConteudo(),
                    item.getTitulo(), req.getNovoTitulo(), req.getJustificativa());
            item.setConteudoEmenda(req.getNovoConteudo());
            if (req.getNovoTitulo() != null) item.setTituloEmenda(req.getNovoTitulo());
            item.setEmendaStatus(ElementoEmendaStatusEnum.ALTERADO);
        } else {
            registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.REVOGAR,
                    item.getConteudo(), null, item.getTitulo(), null, req.getJustificativa());
            item.setEmendaStatus(ElementoEmendaStatusEnum.REVOGADO);
        }
        item.setJustificativaEmenda(req.getJustificativa());
        normativaRepository.save(item);
    }

    private void emendar(Long docId, SecaoDocumentoEnum secao, Long elementoId,
                          EmendaElementoRequestDto req, ItemParteFinal item) {
        EmendaAcaoEnum acao = req.getAcao();

        if (acao == EmendaAcaoEnum.DESFAZER) {
            if (item.getEmendaStatus() == ElementoEmendaStatusEnum.INCLUIDO) {
                registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.DESFAZER,
                        item.getConteudo(), null, item.getTitulo(), null, null);
                finalRepository.delete(item);
                return;
            }
            registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.DESFAZER,
                    item.getConteudoEmenda(), null, item.getTituloEmenda(), null, null);
            item.setConteudoEmenda(null);
            item.setTituloEmenda(null);
            item.setJustificativaEmenda(null);
            item.setEmendaStatus(ElementoEmendaStatusEnum.INALTERADO);
            finalRepository.save(item);
            return;
        }

        validarJustificativa(req.getJustificativa());

        if (acao == EmendaAcaoEnum.ALTERAR) {
            registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.ALTERAR,
                    item.getConteudo(), req.getNovoConteudo(),
                    item.getTitulo(), req.getNovoTitulo(), req.getJustificativa());
            item.setConteudoEmenda(req.getNovoConteudo());
            if (req.getNovoTitulo() != null) item.setTituloEmenda(req.getNovoTitulo());
            item.setEmendaStatus(ElementoEmendaStatusEnum.ALTERADO);
        } else {
            registrarHistorico(docId, secao, elementoId, EmendaAcaoEnum.REVOGAR,
                    item.getConteudo(), null, item.getTitulo(), null, req.getJustificativa());
            item.setEmendaStatus(ElementoEmendaStatusEnum.REVOGADO);
        }
        item.setJustificativaEmenda(req.getJustificativa());
        finalRepository.save(item);
    }

    // ─── Incluir novo elemento ────────────────────────────────────────────────────

    @Transactional
    public void incluir(Long docId, String secao, EmendaIncluirRequestDto req) {
        carregarEmAlteracao(docId);
        if (req.getJustificativa() == null || req.getJustificativa().isBlank()) {
            throw new IllegalArgumentException(JUSTIFICATIVA_REQUERIDA);
        }

        SecaoDocumentoEnum secaoEnum = SecaoDocumentoEnum.valueOf(secao.toUpperCase());
        switch (secaoEnum) {
            case PARTE_PRELIMINAR -> incluirPreliminar(docId, secaoEnum, req);
            case PARTE_NORMATIVA  -> incluirNormativo(docId, secaoEnum, req);
            case PARTE_FINAL      -> incluirFinal(docId, secaoEnum, req);
        }
    }

    private void incluirPreliminar(Long docId, SecaoDocumentoEnum secao, EmendaIncluirRequestDto req) {
        ItemPartePreliminar item = new ItemPartePreliminar();
        item.setDocumento(documentoRepository.getReferenceById(docId));
        item.setTipo(req.getTipo());
        item.setTitulo(req.getTitulo());
        item.setConteudo(req.getConteudo());
        item.setElementOrder(req.getElementOrder());
        item.setEmendaStatus(ElementoEmendaStatusEnum.INCLUIDO);
        item.setJustificativaEmenda(req.getJustificativa());
        ItemPartePreliminar saved = preliminarRepository.save(item);
        registrarHistorico(docId, secao, saved.getId(), EmendaAcaoEnum.INCLUIR,
                null, req.getConteudo(), null, req.getTitulo(), req.getJustificativa());
    }

    private void incluirNormativo(Long docId, SecaoDocumentoEnum secao, EmendaIncluirRequestDto req) {
        ItemAnexoParteNormativa item = new ItemAnexoParteNormativa();
        item.setDocumento(documentoRepository.getReferenceById(docId));
        item.setTipo(req.getTipo());
        item.setTitulo(req.getTitulo());
        item.setConteudo(req.getConteudo());
        item.setElementOrder(req.getElementOrder());
        item.setEmendaStatus(ElementoEmendaStatusEnum.INCLUIDO);
        item.setJustificativaEmenda(req.getJustificativa());
        if (req.getParentId() != null) {
            ItemAnexoParteNormativa parent = normativaRepository.findById(req.getParentId())
                    .filter(p -> p.getDocumento().getId().equals(docId))
                    .orElseThrow(() -> new RuntimeException("Elemento pai não encontrado"));
            item.setParent(parent);
        }
        ItemAnexoParteNormativa saved = normativaRepository.save(item);
        registrarHistorico(docId, secao, saved.getId(), EmendaAcaoEnum.INCLUIR,
                null, req.getConteudo(), null, req.getTitulo(), req.getJustificativa());
    }

    private void incluirFinal(Long docId, SecaoDocumentoEnum secao, EmendaIncluirRequestDto req) {
        ItemParteFinal item = new ItemParteFinal();
        item.setDocumento(documentoRepository.getReferenceById(docId));
        item.setTipo(req.getTipo());
        item.setTitulo(req.getTitulo());
        item.setConteudo(req.getConteudo());
        item.setElementOrder(req.getElementOrder());
        item.setEmendaStatus(ElementoEmendaStatusEnum.INCLUIDO);
        item.setJustificativaEmenda(req.getJustificativa());
        ItemParteFinal saved = finalRepository.save(item);
        registrarHistorico(docId, secao, saved.getId(), EmendaAcaoEnum.INCLUIR,
                null, req.getConteudo(), null, req.getTitulo(), req.getJustificativa());
    }

    // ─── Histórico ────────────────────────────────────────────────────────────────

    private void registrarHistorico(Long docId, SecaoDocumentoEnum secao, Long elementoId,
                                     EmendaAcaoEnum acao,
                                     String conteudoAnterior, String conteudoNovo,
                                     String tituloAnterior, String tituloNovo,
                                     String justificativa) {
        historicoRepository.save(EmendaHistorico.builder()
                .documentoId(docId)
                .secao(secao)
                .elementoId(elementoId)
                .acao(acao)
                .conteudoAnterior(conteudoAnterior)
                .conteudoNovo(conteudoNovo)
                .tituloAnterior(tituloAnterior)
                .tituloNovo(tituloNovo)
                .justificativa(justificativa)
                .build());
    }

    // ─── Utilitários ──────────────────────────────────────────────────────────────

    private Documento carregarEmAlteracao(Long docId) {
        Documento doc = documentoRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException(DOC_NAO_ENCONTRADO));
        if (doc.getDocumentoStatus() != DocumentoStatusEnum.EM_ALTERACAO) {
            throw new IllegalStateException(DOC_NAO_EM_ALTERACAO);
        }
        return doc;
    }

    private void validarJustificativa(String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException(JUSTIFICATIVA_REQUERIDA);
        }
    }
}
