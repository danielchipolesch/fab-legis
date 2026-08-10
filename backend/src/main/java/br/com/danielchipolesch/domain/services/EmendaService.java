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

    // ─── Emendar elemento existente ───────────────────────────────────────────────

    @Transactional
    public void emendar(Long docId, String secao, Long elementoId, EmendaElementoRequestDto req) {
        Documento doc = carregarEmAlteracao(docId);

        SecaoDocumentoEnum secaoEnum = SecaoDocumentoEnum.valueOf(secao.toUpperCase());

        switch (secaoEnum) {
            case PARTE_PRELIMINAR -> emendar(doc, elementoId, req,
                    preliminarRepository.findById(elementoId)
                            .filter(e -> e.getDocumento().getId().equals(docId))
                            .orElseThrow(() -> new RuntimeException(ELEM_NAO_ENCONTRADO)));
            case PARTE_NORMATIVA  -> emendar(doc, elementoId, req,
                    normativaRepository.findById(elementoId)
                            .filter(e -> e.getDocumento().getId().equals(docId))
                            .orElseThrow(() -> new RuntimeException(ELEM_NAO_ENCONTRADO)));
            case PARTE_FINAL      -> emendar(doc, elementoId, req,
                    finalRepository.findById(elementoId)
                            .filter(e -> e.getDocumento().getId().equals(docId))
                            .orElseThrow(() -> new RuntimeException(ELEM_NAO_ENCONTRADO)));
        }
    }

    private void emendar(Documento doc, Long elementoId, EmendaElementoRequestDto req, ItemPartePreliminar item) {
        EmendaAcaoEnum acao = req.getAcao();
        if (acao == EmendaAcaoEnum.DESFAZER) {
            item.setConteudo(item.getConteudoOriginal());
            item.setTitulo(item.getTituloOriginal() != null ? item.getTituloOriginal() : item.getTitulo());
            item.setConteudoOriginal(null);
            item.setTituloOriginal(null);
            item.setJustificativaEmenda(null);
            item.setEmendaStatus(ElementoEmendaStatusEnum.INALTERADO);
            preliminarRepository.save(item);
            return;
        }
        validarJustificativa(req.getJustificativa());
        if (item.getEmendaStatus() == ElementoEmendaStatusEnum.INALTERADO) {
            item.setConteudoOriginal(item.getConteudo());
            item.setTituloOriginal(item.getTitulo());
        }
        if (acao == EmendaAcaoEnum.ALTERAR) {
            item.setConteudo(req.getNovoConteudo());
            if (req.getNovoTitulo() != null) item.setTitulo(req.getNovoTitulo());
            item.setEmendaStatus(ElementoEmendaStatusEnum.ALTERADO);
        } else {
            item.setConteudo(null);
            item.setEmendaStatus(ElementoEmendaStatusEnum.REVOGADO);
        }
        item.setJustificativaEmenda(req.getJustificativa());
        preliminarRepository.save(item);
    }

    private void emendar(Documento doc, Long elementoId, EmendaElementoRequestDto req, ItemAnexoParteNormativa item) {
        EmendaAcaoEnum acao = req.getAcao();
        if (acao == EmendaAcaoEnum.DESFAZER) {
            item.setConteudo(item.getConteudoOriginal());
            item.setTitulo(item.getTituloOriginal() != null ? item.getTituloOriginal() : item.getTitulo());
            item.setConteudoOriginal(null);
            item.setTituloOriginal(null);
            item.setJustificativaEmenda(null);
            item.setEmendaStatus(ElementoEmendaStatusEnum.INALTERADO);
            normativaRepository.save(item);
            return;
        }
        validarJustificativa(req.getJustificativa());
        if (item.getEmendaStatus() == ElementoEmendaStatusEnum.INALTERADO) {
            item.setConteudoOriginal(item.getConteudo());
            item.setTituloOriginal(item.getTitulo());
        }
        if (acao == EmendaAcaoEnum.ALTERAR) {
            item.setConteudo(req.getNovoConteudo());
            if (req.getNovoTitulo() != null) item.setTitulo(req.getNovoTitulo());
            item.setEmendaStatus(ElementoEmendaStatusEnum.ALTERADO);
        } else {
            item.setConteudo(null);
            item.setEmendaStatus(ElementoEmendaStatusEnum.REVOGADO);
        }
        item.setJustificativaEmenda(req.getJustificativa());
        normativaRepository.save(item);
    }

    private void emendar(Documento doc, Long elementoId, EmendaElementoRequestDto req, ItemParteFinal item) {
        EmendaAcaoEnum acao = req.getAcao();
        if (acao == EmendaAcaoEnum.DESFAZER) {
            item.setConteudo(item.getConteudoOriginal());
            item.setTitulo(item.getTituloOriginal() != null ? item.getTituloOriginal() : item.getTitulo());
            item.setConteudoOriginal(null);
            item.setTituloOriginal(null);
            item.setJustificativaEmenda(null);
            item.setEmendaStatus(ElementoEmendaStatusEnum.INALTERADO);
            finalRepository.save(item);
            return;
        }
        validarJustificativa(req.getJustificativa());
        if (item.getEmendaStatus() == ElementoEmendaStatusEnum.INALTERADO) {
            item.setConteudoOriginal(item.getConteudo());
            item.setTituloOriginal(item.getTitulo());
        }
        if (acao == EmendaAcaoEnum.ALTERAR) {
            item.setConteudo(req.getNovoConteudo());
            if (req.getNovoTitulo() != null) item.setTitulo(req.getNovoTitulo());
            item.setEmendaStatus(ElementoEmendaStatusEnum.ALTERADO);
        } else {
            item.setConteudo(null);
            item.setEmendaStatus(ElementoEmendaStatusEnum.REVOGADO);
        }
        item.setJustificativaEmenda(req.getJustificativa());
        finalRepository.save(item);
    }

    // ─── Incluir novo elemento ────────────────────────────────────────────────────

    @Transactional
    public void incluir(Long docId, String secao, EmendaIncluirRequestDto req) {
        Documento doc = carregarEmAlteracao(docId);
        if (req.getJustificativa() == null || req.getJustificativa().isBlank()) {
            throw new IllegalArgumentException(JUSTIFICATIVA_REQUERIDA);
        }

        SecaoDocumentoEnum secaoEnum = SecaoDocumentoEnum.valueOf(secao.toUpperCase());
        switch (secaoEnum) {
            case PARTE_PRELIMINAR -> incluirPreliminar(doc, req);
            case PARTE_NORMATIVA  -> incluirNormativo(doc, req);
            case PARTE_FINAL      -> incluirFinal(doc, req);
        }
    }

    private void incluirPreliminar(Documento doc, EmendaIncluirRequestDto req) {
        ItemPartePreliminar item = new ItemPartePreliminar();
        item.setDocumento(doc);
        item.setTipo(req.getTipo());
        item.setTitulo(req.getTitulo());
        item.setConteudo(req.getConteudo());
        item.setElementOrder(req.getElementOrder());
        item.setEmendaStatus(ElementoEmendaStatusEnum.INCLUIDO);
        item.setJustificativaEmenda(req.getJustificativa());
        preliminarRepository.save(item);
    }

    private void incluirNormativo(Documento doc, EmendaIncluirRequestDto req) {
        ItemAnexoParteNormativa item = new ItemAnexoParteNormativa();
        item.setDocumento(doc);
        item.setTipo(req.getTipo());
        item.setTitulo(req.getTitulo());
        item.setConteudo(req.getConteudo());
        item.setElementOrder(req.getElementOrder());
        item.setEmendaStatus(ElementoEmendaStatusEnum.INCLUIDO);
        item.setJustificativaEmenda(req.getJustificativa());
        if (req.getParentId() != null) {
            ItemAnexoParteNormativa parent = normativaRepository.findById(req.getParentId())
                    .filter(p -> p.getDocumento().getId().equals(doc.getId()))
                    .orElseThrow(() -> new RuntimeException("Elemento pai não encontrado"));
            item.setParent(parent);
        }
        normativaRepository.save(item);
    }

    private void incluirFinal(Documento doc, EmendaIncluirRequestDto req) {
        ItemParteFinal item = new ItemParteFinal();
        item.setDocumento(doc);
        item.setTipo(req.getTipo());
        item.setTitulo(req.getTitulo());
        item.setConteudo(req.getConteudo());
        item.setElementOrder(req.getElementOrder());
        item.setEmendaStatus(ElementoEmendaStatusEnum.INCLUIDO);
        item.setJustificativaEmenda(req.getJustificativa());
        finalRepository.save(item);
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
