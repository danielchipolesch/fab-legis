package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.emendaDtos.EmendaAcaoEnum;
import br.com.danielchipolesch.application.dtos.emendaDtos.EmendaElementoRequestDto;
import br.com.danielchipolesch.application.dtos.emendaDtos.EmendaIncluirRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.*;
import br.com.danielchipolesch.infrastructure.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmendaService {

    private static final String DOC_NAO_ENCONTRADO   = "Documento não encontrado";
    private static final String ELEM_NAO_ENCONTRADO  = "Elemento não encontrado";
    private static final String DOC_NAO_EM_ALTERACAO =
            "O documento não está em status EM_ALTERACAO. Inicie uma alteração antes de emendar.";
    private static final String JUSTIFICATIVA_REQUERIDA =
            "Justificativa é obrigatória para alterar ou revogar um elemento.";
    private static final String ELEMENTO_REVOGADO_PERMANENTE =
            "Elemento revogado por uma publicação anterior; a revogação é permanente e não pode ser desfeita ou alterada.";
    private static final String ELEMENTO_JA_PUBLICADO_PERMANENTE =
            "Elemento já publicado; não é possível desfazer a emenda. Use alterar ou revogar para uma nova emenda.";

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

        // Revogação consolidada numa publicação anterior é permanente — a legislação
        // não retrocede, nenhuma ação (nem desfazer, nem alterar, nem revogar de novo)
        // é mais possível. clausulaEmenda só é preenchido em consolidarPublicacao, então
        // sua presença aqui distingue "já publicada" de "ainda pendente, desfazível".
        if (item.getEmendaStatus() == ElementoEmendaStatusEnum.REVOGADO && item.getClausulaEmenda() != null) {
            throw new IllegalStateException(ELEMENTO_REVOGADO_PERMANENTE);
        }
        // Para INCLUIDO/ALTERADO já publicados, só o desfazer cru é vedado — alterar ou
        // revogar de novo continuam permitidos: é assim que o ciclo de emendas se repete.
        if (acao == EmendaAcaoEnum.DESFAZER && item.getClausulaEmenda() != null) {
            throw new IllegalStateException(ELEMENTO_JA_PUBLICADO_PERMANENTE);
        }

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

        // Elemento INCLUIDO ainda pendente nesta alteração (não publicado): edição livre,
        // sem cláusula. Uma vez publicado (clausulaEmenda != null), alterar de novo segue
        // o fluxo normal abaixo — exige justificativa e gera uma nova cláusula, exatamente
        // como uma alteração comum, pois "o ciclo se repete" também para inclusões.
        if (acao == EmendaAcaoEnum.ALTERAR && item.getEmendaStatus() == ElementoEmendaStatusEnum.INCLUIDO
                && item.getClausulaEmenda() == null) {
            if (req.getNovoConteudo() != null) item.setConteudo(req.getNovoConteudo());
            if (req.getNovoTitulo()   != null) item.setTitulo(req.getNovoTitulo());
            preliminarRepository.save(item);
            return;
        }
        // Só bloqueia revogar um INCLUIDO ainda pendente (não publicado) — use excluir
        // para descartar uma inclusão que nunca chegou a ser publicada. Uma vez
        // publicado, revogar é o caminho correto (gera cláusula de revogação, como
        // qualquer outro elemento).
        if (acao == EmendaAcaoEnum.REVOGAR && item.getEmendaStatus() == ElementoEmendaStatusEnum.INCLUIDO
                && item.getClausulaEmenda() == null) {
            throw new IllegalArgumentException(
                    "Elemento incluído por emenda ainda não publicado não pode ser revogado. Use a opção de excluir.");
        }

        validarJustificativa(req.getJustificativa());

        // Elemento com emenda já publicada antes (ALTERADO ou INCLUIDO — REVOGADO é
        // permanente e nem chega aqui, ver guarda acima): uma nova emenda sobre ele
        // precisa partir da redação vigente. Para ALTERADO, a vigente está em
        // conteudoEmenda, não em conteudo (que ainda guarda o texto anterior a ESSA
        // alteração — LC 95/1998 exige mostrar a redação imediatamente anterior
        // tachada), então promove-a antes de aceitar a nova mudança. Para INCLUIDO,
        // conteudo já é a vigente (nunca usou conteudoEmenda), nada a promover. A
        // cláusula antiga é descartada aqui — uma cláusula NOVA é gerada na próxima
        // publicação, em clausulaEmenda.
        if (item.getClausulaEmenda() != null) {
            if (item.getEmendaStatus() == ElementoEmendaStatusEnum.ALTERADO) {
                if (item.getConteudoEmenda() != null) item.setConteudo(item.getConteudoEmenda());
                if (item.getTituloEmenda() != null) item.setTitulo(item.getTituloEmenda());
            }
            item.setClausulaEmenda(null);
        }

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

        // Revogação consolidada numa publicação anterior é permanente — a legislação
        // não retrocede, nenhuma ação (nem desfazer, nem alterar, nem revogar de novo)
        // é mais possível. clausulaEmenda só é preenchido em consolidarPublicacao, então
        // sua presença aqui distingue "já publicada" de "ainda pendente, desfazível".
        if (item.getEmendaStatus() == ElementoEmendaStatusEnum.REVOGADO && item.getClausulaEmenda() != null) {
            throw new IllegalStateException(ELEMENTO_REVOGADO_PERMANENTE);
        }
        // Para INCLUIDO/ALTERADO já publicados, só o desfazer cru é vedado — alterar ou
        // revogar de novo continuam permitidos: é assim que o ciclo de emendas se repete.
        if (acao == EmendaAcaoEnum.DESFAZER && item.getClausulaEmenda() != null) {
            throw new IllegalStateException(ELEMENTO_JA_PUBLICADO_PERMANENTE);
        }

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

        // Elemento INCLUIDO ainda pendente nesta alteração (não publicado): edição livre,
        // sem cláusula. Uma vez publicado (clausulaEmenda != null), alterar de novo segue
        // o fluxo normal abaixo — exige justificativa e gera uma nova cláusula, exatamente
        // como uma alteração comum, pois "o ciclo se repete" também para inclusões.
        if (acao == EmendaAcaoEnum.ALTERAR && item.getEmendaStatus() == ElementoEmendaStatusEnum.INCLUIDO
                && item.getClausulaEmenda() == null) {
            if (req.getNovoConteudo() != null) item.setConteudo(req.getNovoConteudo());
            if (req.getNovoTitulo()   != null) item.setTitulo(req.getNovoTitulo());
            normativaRepository.save(item);
            return;
        }
        // Só bloqueia revogar um INCLUIDO ainda pendente (não publicado) — use excluir
        // para descartar uma inclusão que nunca chegou a ser publicada. Uma vez
        // publicado, revogar é o caminho correto (gera cláusula de revogação, como
        // qualquer outro elemento).
        if (acao == EmendaAcaoEnum.REVOGAR && item.getEmendaStatus() == ElementoEmendaStatusEnum.INCLUIDO
                && item.getClausulaEmenda() == null) {
            throw new IllegalArgumentException(
                    "Elemento incluído por emenda ainda não publicado não pode ser revogado. Use a opção de excluir.");
        }

        validarJustificativa(req.getJustificativa());

        // Elemento com emenda já publicada antes (ALTERADO ou INCLUIDO — REVOGADO é
        // permanente e nem chega aqui, ver guarda acima): uma nova emenda sobre ele
        // precisa partir da redação vigente. Para ALTERADO, a vigente está em
        // conteudoEmenda, não em conteudo (que ainda guarda o texto anterior a ESSA
        // alteração — LC 95/1998 exige mostrar a redação imediatamente anterior
        // tachada), então promove-a antes de aceitar a nova mudança. Para INCLUIDO,
        // conteudo já é a vigente (nunca usou conteudoEmenda), nada a promover. A
        // cláusula antiga é descartada aqui — uma cláusula NOVA é gerada na próxima
        // publicação, em clausulaEmenda.
        if (item.getClausulaEmenda() != null) {
            if (item.getEmendaStatus() == ElementoEmendaStatusEnum.ALTERADO) {
                if (item.getConteudoEmenda() != null) item.setConteudo(item.getConteudoEmenda());
                if (item.getTituloEmenda() != null) item.setTitulo(item.getTituloEmenda());
            }
            item.setClausulaEmenda(null);
        }

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

        // Revogação consolidada numa publicação anterior é permanente — a legislação
        // não retrocede, nenhuma ação (nem desfazer, nem alterar, nem revogar de novo)
        // é mais possível. clausulaEmenda só é preenchido em consolidarPublicacao, então
        // sua presença aqui distingue "já publicada" de "ainda pendente, desfazível".
        if (item.getEmendaStatus() == ElementoEmendaStatusEnum.REVOGADO && item.getClausulaEmenda() != null) {
            throw new IllegalStateException(ELEMENTO_REVOGADO_PERMANENTE);
        }
        // Para INCLUIDO/ALTERADO já publicados, só o desfazer cru é vedado — alterar ou
        // revogar de novo continuam permitidos: é assim que o ciclo de emendas se repete.
        if (acao == EmendaAcaoEnum.DESFAZER && item.getClausulaEmenda() != null) {
            throw new IllegalStateException(ELEMENTO_JA_PUBLICADO_PERMANENTE);
        }

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

        // Elemento INCLUIDO ainda pendente nesta alteração (não publicado): edição livre,
        // sem cláusula. Uma vez publicado (clausulaEmenda != null), alterar de novo segue
        // o fluxo normal abaixo — exige justificativa e gera uma nova cláusula, exatamente
        // como uma alteração comum, pois "o ciclo se repete" também para inclusões.
        if (acao == EmendaAcaoEnum.ALTERAR && item.getEmendaStatus() == ElementoEmendaStatusEnum.INCLUIDO
                && item.getClausulaEmenda() == null) {
            if (req.getNovoConteudo() != null) item.setConteudo(req.getNovoConteudo());
            if (req.getNovoTitulo()   != null) item.setTitulo(req.getNovoTitulo());
            finalRepository.save(item);
            return;
        }
        // Só bloqueia revogar um INCLUIDO ainda pendente (não publicado) — use excluir
        // para descartar uma inclusão que nunca chegou a ser publicada. Uma vez
        // publicado, revogar é o caminho correto (gera cláusula de revogação, como
        // qualquer outro elemento).
        if (acao == EmendaAcaoEnum.REVOGAR && item.getEmendaStatus() == ElementoEmendaStatusEnum.INCLUIDO
                && item.getClausulaEmenda() == null) {
            throw new IllegalArgumentException(
                    "Elemento incluído por emenda ainda não publicado não pode ser revogado. Use a opção de excluir.");
        }

        validarJustificativa(req.getJustificativa());

        // Elemento com emenda já publicada antes (ALTERADO ou INCLUIDO — REVOGADO é
        // permanente e nem chega aqui, ver guarda acima): uma nova emenda sobre ele
        // precisa partir da redação vigente. Para ALTERADO, a vigente está em
        // conteudoEmenda, não em conteudo (que ainda guarda o texto anterior a ESSA
        // alteração — LC 95/1998 exige mostrar a redação imediatamente anterior
        // tachada), então promove-a antes de aceitar a nova mudança. Para INCLUIDO,
        // conteudo já é a vigente (nunca usou conteudoEmenda), nada a promover. A
        // cláusula antiga é descartada aqui — uma cláusula NOVA é gerada na próxima
        // publicação, em clausulaEmenda.
        if (item.getClausulaEmenda() != null) {
            if (item.getEmendaStatus() == ElementoEmendaStatusEnum.ALTERADO) {
                if (item.getConteudoEmenda() != null) item.setConteudo(item.getConteudoEmenda());
                if (item.getTituloEmenda() != null) item.setTitulo(item.getTituloEmenda());
            }
            item.setClausulaEmenda(null);
        }

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

    // ─── Reordenar artigo incluído por emenda ──────────────────────────────────────
    //
    // Regra (LGCP): é vedada a renumeração de artigos e unidades superiores ao artigo
    // — cada um mantém sua posição fixa, identificada por número-base + sufixo de
    // letra quando inserido por emenda. A ÚNICA exceção é entre artigos ainda em modo
    // de inclusão (INCLUIDO, ainda não aprovados): como nenhum deles tem posição
    // definitiva até a aprovação, podem ser livremente reordenados ENTRE SI. Nunca é
    // permitido trocar de posição com um artigo original (INALTERADO/ALTERADO/
    // REVOGADO), pois isso equivaleria a renumerá-lo.

    @Transactional
    public void reordenarIncluido(Long docId, String secao, Long elementoId, String direcao) {
        carregarEmAlteracao(docId);
        SecaoDocumentoEnum secaoEnum = SecaoDocumentoEnum.valueOf(secao.toUpperCase());
        if (secaoEnum != SecaoDocumentoEnum.PARTE_NORMATIVA) {
            throw new IllegalArgumentException("Reordenação só é permitida na parte normativa.");
        }

        ItemAnexoParteNormativa item = normativaRepository.findById(elementoId)
                .filter(e -> e.getDocumento().getId().equals(docId))
                .orElseThrow(() -> new RuntimeException(ELEM_NAO_ENCONTRADO));

        if (item.getTipo() != ItemAnexoParteNormativaTipoEnum.ARTIGO
                || item.getEmendaStatus() != ElementoEmendaStatusEnum.INCLUIDO
                || item.getClausulaEmenda() != null) {
            // clausulaEmenda != null significa que este artigo já foi consolidado numa
            // publicação anterior: sua posição (e portanto sua numeração/letra) já é
            // definitiva e reordená-lo mudaria essa identidade — vedado.
            throw new IllegalArgumentException(
                    "Só é possível reordenar artigos incluídos por emenda e ainda não publicados.");
        }

        List<ItemAnexoParteNormativa> siblings = item.getParent() != null
                ? normativaRepository.findByParentOrderByElementOrderAsc(item.getParent())
                : normativaRepository.findRootItemsByDocumentoId(docId);

        int idx = siblings.indexOf(item);
        boolean cima = "CIMA".equalsIgnoreCase(direcao);
        int targetIdx = idx + (cima ? -1 : 1);
        if (idx < 0 || targetIdx < 0 || targetIdx >= siblings.size()) {
            throw new IllegalArgumentException("Não é possível mover o elemento nessa direção.");
        }

        ItemAnexoParteNormativa vizinho = siblings.get(targetIdx);
        if (vizinho.getTipo() != ItemAnexoParteNormativaTipoEnum.ARTIGO
                || vizinho.getEmendaStatus() != ElementoEmendaStatusEnum.INCLUIDO
                || vizinho.getClausulaEmenda() != null) {
            throw new IllegalArgumentException(
                    "Só é possível trocar de posição com outro artigo incluído por emenda e ainda não publicado.");
        }

        Integer ordemItem = item.getElementOrder();
        item.setElementOrder(vizinho.getElementOrder());
        vizinho.setElementOrder(ordemItem);
        normativaRepository.save(item);
        normativaRepository.save(vizinho);
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
        item.setIncluidoPorEmenda(true);
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

    // ─── Consolidação na (re)publicação ────────────────────────────────────────────
    //
    // A legislação não retrocede: uma vez publicada, uma emenda vira parte definitiva
    // e permanente do documento. INCLUIDO/ALTERADO/REVOGADO NUNCA voltam a INALTERADO
    // nem têm conteudo/conteudoEmenda tocados aqui — só a cláusula é congelada em
    // clausulaEmenda, fixando para sempre a portaria/BCA DESTA publicação (que não é
    // mais recalculada a partir de tx_portaria_referencia/tx_bca_referencia do
    // documento, sobrescritos a cada nova republicação). Isso preserva a dupla redação
    // (anterior tachada + vigente) exigida pela LC 95/1998 mesmo depois de publicado.
    //
    // Uma NOVA emenda sobre um elemento já publicado (alterar de novo, ou revogar) é
    // tratada em emendar(): promove conteudoEmenda -> conteudo e limpa clausulaEmenda
    // antes de aceitar a mudança, para que este método a veja como pendente de novo.
    @Transactional
    public void consolidarPublicacao(Long docId, String portariaReferencia, String bcaReferencia) {
        for (var item : normativaRepository.findAllByDocumentoId(docId)) {
            if (!precisaConsolidar(item.getEmendaStatus(), item.getClausulaEmenda())) continue;
            item.setClausulaEmenda(buildClausula(item.getEmendaStatus(), portariaReferencia, bcaReferencia));
            normativaRepository.save(item);
        }
        for (var item : preliminarRepository.findByDocumentoIdOrderByElementOrderAsc(docId)) {
            if (!precisaConsolidar(item.getEmendaStatus(), item.getClausulaEmenda())) continue;
            item.setClausulaEmenda(buildClausula(item.getEmendaStatus(), portariaReferencia, bcaReferencia));
            preliminarRepository.save(item);
        }
        for (var item : finalRepository.findByDocumentoIdOrderByElementOrderAsc(docId)) {
            if (!precisaConsolidar(item.getEmendaStatus(), item.getClausulaEmenda())) continue;
            item.setClausulaEmenda(buildClausula(item.getEmendaStatus(), portariaReferencia, bcaReferencia));
            finalRepository.save(item);
        }
    }

    // Retorna false quando não há nada pendente (INALTERADO) ou quando o elemento já
    // foi consolidado numa publicação anterior (clausulaEmenda já preenchida — nesse
    // caso, se uma nova emenda tiver sido feita sobre ele, emendar() já limpou
    // clausulaEmenda para sinalizar que há algo pendente de novo).
    private boolean precisaConsolidar(ElementoEmendaStatusEnum status, String clausulaExistente) {
        if (status == null || status == ElementoEmendaStatusEnum.INALTERADO) return false;
        return clausulaExistente == null;
    }

    private String buildClausula(ElementoEmendaStatusEnum status, String portariaReferencia, String bcaReferencia) {
        String acao = switch (status) {
            case ALTERADO -> "alterado";
            case REVOGADO -> "revogado";
            case INCLUIDO -> "incluído";
            default -> "modificado";
        };
        return "(" + acao + " pela " + portariaReferencia + ", publicada no " + bcaReferencia + ")";
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
