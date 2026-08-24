package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.NumeracaoElementoResponseDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.SecaoItemRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.SecoesSaveRequestDto;
import br.com.danielchipolesch.application.dtos.itemParteFinalDtos.ItemParteFinalResponseDto;
import br.com.danielchipolesch.application.dtos.itemPartePreliminarDtos.ItemPartePreliminarResponseDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.*;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemParteFinalRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemPartePreliminarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentoParteNormativaService {

    @Autowired
    DocumentoRepository documentoRepository;

    @Autowired
    ItemAnexoParteNormativaRepository itemAnexoParteNormativaRepository;

    @Autowired
    ItemPartePreliminarRepository itemPartePreliminarRepository;

    @Autowired
    ItemParteFinalRepository itemParteFinalRepository;

    @Autowired
    DocumentoHistoricoService documentoHistoricoService;

    @Autowired
    NumeracaoService numeracaoService;

    @Autowired
    DocumentoConcorrenciaService concorrenciaService;

    // ─── Carregamento ────────────────────────────────────────────────────────────

    public List<ItemPartePreliminar> getItensPreliminaresByDocumento(Long documentoId) {
        return itemPartePreliminarRepository.findByDocumentoIdOrderByElementOrderAsc(documentoId);
    }

    public List<ItemAnexoParteNormativa> getItensNormativosByDocumento(Long documentoId) {
        List<ItemAnexoParteNormativa> raiz = itemAnexoParteNormativaRepository.findRootItemsByDocumentoId(documentoId);
        raiz.forEach(this::carregarChildrenRecursivamente);
        return raiz;
    }

    public List<ItemParteFinal> getItensFinaisByDocumento(Long documentoId) {
        return itemParteFinalRepository.findByDocumentoIdOrderByElementOrderAsc(documentoId);
    }

    // NUNCA usar item.setChildren(novaLista) aqui: children é
    // @OneToMany(orphanRemoval=true), então substituir a coleção gerenciada
    // do Hibernate por uma lista nova e desconectada faz o flush seguinte
    // (disparado por QUALQUER escrita na mesma sessão -- ver open-in-view)
    // falhar com "A collection with orphan deletion was no longer
    // referenced by the owning entity instance". clear()+addAll() atualiza
    // o conteúdo mantendo a mesma instância de coleção que o Hibernate
    // já está rastreando.
    private void carregarChildrenRecursivamente(ItemAnexoParteNormativa item) {
        List<ItemAnexoParteNormativa> children = itemAnexoParteNormativaRepository.findByParentOrderByElementOrderAsc(item);
        item.getChildren().clear();
        item.getChildren().addAll(children);
        children.forEach(this::carregarChildrenRecursivamente);
    }

    // Numeração calculada da parte normativa (capítulo/seção/subseção/artigo) —
    // mesmo cálculo usado no PDF oficial (ver NumeracaoService), exposto para
    // qualquer consumidor via API.
    public List<NumeracaoElementoResponseDto> listarNumeracao(Long documentoId) {
        List<ItemAnexoParteNormativaResponseDto> normativos = getItensNormativosByDocumento(documentoId)
                .stream().map(ItemAnexoParteNormativaResponseDto::from).toList();
        return numeracaoService.calcular(normativos).entrySet().stream()
                .map(e -> NumeracaoElementoResponseDto.from(e.getKey(), e.getValue()))
                .toList();
    }

    // ─── Consulta completa ────────────────────────────────────────────────────────

    public DocumentoResponseComAnexoTextualDto getDocumentoComAnexoTextualDtoById(Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        List<ItemPartePreliminarResponseDto> preliminares = getItensPreliminaresByDocumento(documentoId)
                .stream().map(ItemPartePreliminarResponseDto::from).toList();

        List<ItemAnexoParteNormativaResponseDto> normativos = getItensNormativosByDocumento(documentoId)
                .stream().map(ItemAnexoParteNormativaResponseDto::from).toList();

        List<ItemParteFinalResponseDto> finais = getItensFinaisByDocumento(documentoId)
                .stream().map(ItemParteFinalResponseDto::from).toList();

        return DocumentoMapper.documentoToDocumentoComAnexoTextualResponseDto(documento, preliminares, normativos, finais);
    }

    // ─── Salvar seções ────────────────────────────────────────────────────────────

    @Transactional
    public void salvarSecoes(Long documentoId, SecoesSaveRequestDto request) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        if (documento.getDocumentoStatus() == DocumentoStatusEnum.EM_ALTERACAO
                || documento.getDocumentoStatus() == DocumentoStatusEnum.ALTERADO) {
            throw new IllegalStateException(
                    "Documento em alteração (ou aguardando republicação): utilize os endpoints de emenda "
                    + "para modificar elementos individualmente, nunca o salvamento em massa.");
        }

        if (request.itens() == null) return;

        concorrenciaService.checarEAtualizarVersao(documento, request.versaoEsperada());

        documentoHistoricoService.registrar(documento, TipoAlteracaoEnum.ALTERACAO_CONTEUDO,
                "Conteúdo do documento salvo", null, null);

        List<SecaoItemRequestDto> preliminares = request.itens().stream()
                .filter(i -> i.secao() == SecaoDocumentoEnum.PARTE_PRELIMINAR)
                .toList();
        List<SecaoItemRequestDto> normativos = request.itens().stream()
                .filter(i -> i.secao() == SecaoDocumentoEnum.PARTE_NORMATIVA)
                .toList();

        if (!preliminares.isEmpty()) salvarItensPreliminares(documento, preliminares);
        if (!normativos.isEmpty()) salvarItensNormativos(documento, normativos);
    }

    private void salvarItensPreliminares(Documento documento, List<SecaoItemRequestDto> dtos) {
        itemPartePreliminarRepository.deleteAllByDocumentoId(documento.getId());
        for (int i = 0; i < dtos.size(); i++) {
            SecaoItemRequestDto dto = dtos.get(i);
            ItemPartePreliminar item = new ItemPartePreliminar();
            item.setDocumento(documento);
            item.setTipo(dto.tipo());
            item.setElementOrder(dto.elementOrder() != null ? dto.elementOrder() : i + 1);
            item.setTitulo(dto.titulo());
            item.setConteudo(dto.conteudo());
            item.setFullTextContent(gerarFullTextContent(dto.titulo(), dto.conteudo(), dto.fullTextContent()));
            itemPartePreliminarRepository.save(item);
        }
    }

    private void salvarItensNormativos(Documento documento, List<SecaoItemRequestDto> dtos) {
        itemAnexoParteNormativaRepository.nullifyParentsForDocument(documento.getId());
        itemAnexoParteNormativaRepository.deleteAllByDocumentoId(documento.getId());
        for (int i = 0; i < dtos.size(); i++) {
            SecaoItemRequestDto dto = dtos.get(i);
            int elementOrder = dto.elementOrder() != null ? dto.elementOrder() : i + 1;
            salvarItemNormativoRecursivo(documento, dto, elementOrder, null);
        }
    }

    private void salvarItemNormativoRecursivo(Documento documento, SecaoItemRequestDto dto, int elementOrder, ItemAnexoParteNormativa parent) {
        ItemAnexoParteNormativa item = new ItemAnexoParteNormativa();
        item.setDocumento(documento);
        item.setTipo(dto.tipo());
        item.setElementOrder(elementOrder);
        item.setTitulo(dto.titulo());
        item.setConteudo(dto.conteudo());
        item.setFullTextContent(gerarFullTextContent(dto.titulo(), dto.conteudo(), dto.fullTextContent()));
        item.setParent(parent);
        itemAnexoParteNormativaRepository.save(item);

        List<SecaoItemRequestDto> filhos = dto.filhos();
        if (filhos != null) {
            for (int i = 0; i < filhos.size(); i++) {
                SecaoItemRequestDto filho = filhos.get(i);
                int filhoOrder = filho.elementOrder() != null ? filho.elementOrder() : i + 1;
                salvarItemNormativoRecursivo(documento, filho, filhoOrder, item);
            }
        }
    }

    private void salvarItensFinais(Documento documento, List<SecaoItemRequestDto> dtos) {
        itemParteFinalRepository.deleteAllByDocumentoId(documento.getId());
        for (int i = 0; i < dtos.size(); i++) {
            SecaoItemRequestDto dto = dtos.get(i);
            ItemParteFinal item = new ItemParteFinal();
            item.setDocumento(documento);
            item.setTipo(dto.tipo());
            item.setElementOrder(dto.elementOrder() != null ? dto.elementOrder() : i + 1);
            item.setTitulo(dto.titulo());
            item.setConteudo(dto.conteudo());
            item.setFullTextContent(gerarFullTextContent(dto.titulo(), dto.conteudo(), dto.fullTextContent()));
            itemParteFinalRepository.save(item);
        }
    }

    // ─── Adicionar item individual ────────────────────────────────────────────────

    public DocumentoResponseComAnexoTextualDto adicionarItemAoDocumento(Long idDocumento, ItemAnexoParteNormativaRequestDto dto) {
        Documento documento = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        ItemAnexoParteNormativa novoItem = new ItemAnexoParteNormativa();
        novoItem.setDocumento(documento);
        novoItem.setTipo(dto.tipo());
        novoItem.setTitulo(dto.titulo());
        novoItem.setConteudo(dto.conteudo());

        if (dto.parentId() != null) {
            ItemAnexoParteNormativa parent = itemAnexoParteNormativaRepository.findById(dto.parentId())
                    .orElseThrow(() -> new RuntimeException("Item pai não encontrado"));
            if (!parent.getDocumento().getId().equals(documento.getId())) {
                throw new RuntimeException("O item pai não pertence ao mesmo documento!");
            }
            novoItem.setParent(parent);
        }

        itemAnexoParteNormativaRepository.save(novoItem);
        return getDocumentoComAnexoTextualDtoById(idDocumento);
    }

    // ─── Utilitários ──────────────────────────────────────────────────────────────

    private String gerarFullTextContent(String titulo, String conteudo, String fullTextContentEnviado) {
        if (fullTextContentEnviado != null && !fullTextContentEnviado.isBlank()) return fullTextContentEnviado;
        StringBuilder sb = new StringBuilder();
        if (titulo != null && !titulo.isBlank()) sb.append(titulo);
        if (conteudo != null && !conteudo.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(conteudo);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

}
