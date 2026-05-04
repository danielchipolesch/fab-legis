package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestUpdateItemParteNormativaDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaRequestDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentException;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DocumentoParteNormativaService {

    @Autowired
    DocumentoRepository documentoRepository;

    @Autowired
    ItemAnexoParteNormativaRepository itemAnexoParteNormativaRepository;

    // Define as regras de hierarquia
    private static final List<ItemAnexoParteNormativaTipoEnum> TITULO_CHILDREN = Arrays.asList(ItemAnexoParteNormativaTipoEnum.CAPITULO);
    private static final List<ItemAnexoParteNormativaTipoEnum> CAPITULO_CHILDREN = Arrays.asList(ItemAnexoParteNormativaTipoEnum.SECAO, ItemAnexoParteNormativaTipoEnum.ARTIGO);
    private static final List<ItemAnexoParteNormativaTipoEnum> SECAO_CHILDREN = Arrays.asList(ItemAnexoParteNormativaTipoEnum.ARTIGO, ItemAnexoParteNormativaTipoEnum.SUBSECAO);
    private static final List<ItemAnexoParteNormativaTipoEnum> SUBSECAO_CHILDREN = Arrays.asList(ItemAnexoParteNormativaTipoEnum.SUBSECAO);
    private static final List<ItemAnexoParteNormativaTipoEnum> ARTIGO_CHILDREN = Arrays.asList(ItemAnexoParteNormativaTipoEnum.PARAGRAFO_NUMERADO, ItemAnexoParteNormativaTipoEnum.PARAGRAFO_UNICO, ItemAnexoParteNormativaTipoEnum.ITEM);
    private static final List<ItemAnexoParteNormativaTipoEnum> PARAGRAFO_NUMERADO_CHILDREN = Arrays.asList(ItemAnexoParteNormativaTipoEnum.INCISO);
    private static final List<ItemAnexoParteNormativaTipoEnum> PARAGRAFO_UNICO_CHILDREN = Arrays.asList(ItemAnexoParteNormativaTipoEnum.INCISO);
    private static final List<ItemAnexoParteNormativaTipoEnum> INCISO_CHILDREN = Arrays.asList(ItemAnexoParteNormativaTipoEnum.ALINEA);
    private static final List<ItemAnexoParteNormativaTipoEnum> ALINEA_CHILDREN = Arrays.asList(ItemAnexoParteNormativaTipoEnum.ITEM);
    private static final List<ItemAnexoParteNormativaTipoEnum> ITEM_CHILDREN = Arrays.asList();

    public List<ItemAnexoParteNormativa> getItensByDocumento(Long documentoId) {
        List<ItemAnexoParteNormativa> itensRaiz = itemAnexoParteNormativaRepository.findRootItemsByDocumentoId(documentoId);
        itensRaiz.forEach(this::carregarChildrenRecursivamente);
        return itensRaiz;
    }

    private void carregarChildrenRecursivamente(ItemAnexoParteNormativa item) {
        List<ItemAnexoParteNormativa> children = itemAnexoParteNormativaRepository.findByParent(item);
        item.setChildren(children);
        children.forEach(this::carregarChildrenRecursivamente);
    }

    public Documento getDocumentoComAnexoTextualById(Long documentoId, boolean carregarItens) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        if (carregarItens) {
            List<ItemAnexoParteNormativa> itens = this.getItensByDocumento(documentoId);
            documento.setItens(itens);
        }

        return documento;
    }

    public DocumentoResponseComAnexoTextualDto adicionarItemAoDocumento(Long idDocumento, ItemAnexoParteNormativaRequestDto dto) {
        Documento documento = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        ItemAnexoParteNormativa novoItem = new ItemAnexoParteNormativa();
        novoItem.setDocumento(documento);
        novoItem.setTipo(dto.getTipo());
        novoItem.setTitulo(dto.getTitulo());
        novoItem.setConteuto(dto.getConteuto());

        if (dto.getParentId() != null) {
            // Caso seja um item filho, vincula ao item pai
            ItemAnexoParteNormativa parent = itemAnexoParteNormativaRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Item pai não encontrado"));

            if (!parent.getDocumento().getId().equals(documento.getId())) {
                throw new RuntimeException("O item pai não pertence ao mesmo documento!");
            }

            novoItem.setParent(parent);
            parent.getChildren().add(novoItem);  // Atualiza a lista de filhos do pai
        }

        itemAnexoParteNormativaRepository.save(novoItem);

        documento.setItens(documento.getItens());
//        documento.getItens().stream().map(itemAnexoParteNormativa -> itemAnexoParteNormativa.setDocumento()).toList();
        return DocumentoMapper.documentoToDocumentoComAnexoTextualResponseDto(documento);
    }

//    @Transactional
//    public DocumentoResponseComAnexoTextualDto updateDocumentoParteNormativaItem(Long idDocumento, DocumentoRequestUpdateItemParteNormativaDto request) throws RuntimeException {
//
//        Documento documento = documentoRepository.findById(idDocumento).orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));
//
//        switch (documento.getDocumentoStatus()) {
//
//            case RASCUNHO, MINUTA -> {
//
//                if (documento.getItens().isEmpty() || request.getParentId() == null) {
//
//                    documento.getItens().add(DocumentoMapper.documentoRequestUpdateItemParteNormativaDtoToItemAnexoParteNormativa(request));
//                }
//
//                Optional<ItemAnexoParteNormativa> parentItem = findItemById(documento.getItens(), request.getParentId());
//
//                if (parentItem.isPresent()){
//                    if (!isValidChild(parentItem.get().getTipo(), request.getTipo())){
//                        throw new RuntimeException("Hierarquia inválida: " + request.getTipo() + " não pode ser filho de " + parentItem.get().getTipo());
//                    }
//                    addChild(parentItem.get(), DocumentoMapper.documentoRequestUpdateItemParteNormativaDtoToItemAnexoParteNormativa(request));
//
//                } else {
//                    throw new RuntimeException("Item pai não encontrado");
//                }
//
//                documentoRepository.save(documento);
//                return DocumentoMapper.documentoToDocumentoComAnexoTextualResponseDto(documento);
//
////                var documentAttachmentId = documento.getDocumentAttachment().getId();
////                var documentAttachment = documentAttachmentRepository.findById(documentAttachmentId).orElseThrow(() -> new ResourceNotFoundException(DocumentAttachmentException.NOT_FOUND.getMessage()));
////                documentAttachment.setTextAttachment(request.getTextAttachment().isBlank() ? documentAttachment.getTextAttachment() : request.getTextAttachment());
////                documentAttachmentRepository.save(documentAttachment);
////                document.setDocumentStatus(DocumentStatus.MINUTA);
////                documentRepository.save(document);
////                return DocumentMapper.documentToDocumentResponseDto(document);
//            }
//
//            default -> throw new StatusCannotBeUpdatedException(DocumentException.CANNOT_BE_UPDATED.getMessage());
//        }
//    }

    private boolean isValidChild(ItemAnexoParteNormativaTipoEnum parentType, ItemAnexoParteNormativaTipoEnum childType) {
        return switch (parentType) {
            case TITULO -> TITULO_CHILDREN.contains(childType);
            case CAPITULO -> CAPITULO_CHILDREN.contains(childType);
            case SECAO -> SECAO_CHILDREN.contains(childType);
            case SUBSECAO -> SUBSECAO_CHILDREN.contains(childType);
            case ARTIGO -> ARTIGO_CHILDREN.contains(childType);
            case PARAGRAFO_NUMERADO -> PARAGRAFO_NUMERADO_CHILDREN.contains(childType);
            case PARAGRAFO_UNICO -> PARAGRAFO_UNICO_CHILDREN.contains(childType);
            case INCISO -> INCISO_CHILDREN.contains(childType);
            case ALINEA -> ALINEA_CHILDREN.contains(childType);
            case ITEM -> ITEM_CHILDREN.contains(childType);
            default -> false;
        };
    }

    private Optional<ItemAnexoParteNormativa> findItemById(List<ItemAnexoParteNormativa> items, Long id) {
        if (items == null) return Optional.empty();
        for (ItemAnexoParteNormativa item : items) {
            if (item.getId().equals(id)) return Optional.of(item);
            Optional<ItemAnexoParteNormativa> found = findItemById(item.getChildren(), id);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    // Método auxiliar para adicionar um item filho sem modificar a entidade diretamente
    private void addChild(ItemAnexoParteNormativa parent, ItemAnexoParteNormativa child) {
        if (parent.getChildren() == null) {
            parent.setChildren(new ArrayList<>());
        }
        parent.getChildren().add(child);
    }
}
