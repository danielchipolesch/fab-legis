package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaRequestDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.ItemAnexoParteNormativaResponseDto;
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

    private void carregarChildrenRecursivamente(ItemAnexoParteNormativa item) {
        List<ItemAnexoParteNormativa> children = itemAnexoParteNormativaRepository.findByParentOrderByElementOrderAsc(item);
        item.setChildren(children);
        children.forEach(this::carregarChildrenRecursivamente);
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

        if (documento.getDocumentoStatus() == DocumentoStatusEnum.EM_ALTERACAO) {
            throw new IllegalStateException(
                    "Documento em alteração: utilize os endpoints de emenda para modificar elementos individualmente.");
        }

        if (request.getItens() == null) return;

        documentoHistoricoService.registrar(documento, TipoAlteracaoEnum.ALTERACAO_CONTEUDO,
                "Conteúdo do documento salvo", null, null);

        List<SecaoItemRequestDto> preliminares = request.getItens().stream()
                .filter(i -> i.getSecao() == SecaoDocumentoEnum.PARTE_PRELIMINAR)
                .toList();
        List<SecaoItemRequestDto> normativos = request.getItens().stream()
                .filter(i -> i.getSecao() == SecaoDocumentoEnum.PARTE_NORMATIVA)
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
            item.setTipo(dto.getTipo());
            item.setElementOrder(dto.getElementOrder() != null ? dto.getElementOrder() : i + 1);
            item.setTitulo(dto.getTitulo());
            item.setConteudo(dto.getConteudo());
            item.setFullTextContent(gerarFullTextContent(dto.getTitulo(), dto.getConteudo(), dto.getFullTextContent()));
            itemPartePreliminarRepository.save(item);
        }
    }

    private void salvarItensNormativos(Documento documento, List<SecaoItemRequestDto> dtos) {
        itemAnexoParteNormativaRepository.nullifyParentsForDocument(documento.getId());
        itemAnexoParteNormativaRepository.deleteAllByDocumentoId(documento.getId());
        for (int i = 0; i < dtos.size(); i++) {
            SecaoItemRequestDto dto = dtos.get(i);
            if (dto.getElementOrder() == null) dto.setElementOrder(i + 1);
            salvarItemNormativoRecursivo(documento, dto, null);
        }
    }

    private void salvarItemNormativoRecursivo(Documento documento, SecaoItemRequestDto dto, ItemAnexoParteNormativa parent) {
        ItemAnexoParteNormativa item = new ItemAnexoParteNormativa();
        item.setDocumento(documento);
        item.setTipo(dto.getTipo());
        item.setElementOrder(dto.getElementOrder());
        item.setTitulo(dto.getTitulo());
        item.setConteudo(dto.getConteudo());
        item.setFullTextContent(gerarFullTextContent(dto.getTitulo(), dto.getConteudo(), dto.getFullTextContent()));
        item.setParent(parent);
        itemAnexoParteNormativaRepository.save(item);

        List<SecaoItemRequestDto> filhos = dto.getFilhos();
        if (filhos != null) {
            for (int i = 0; i < filhos.size(); i++) {
                SecaoItemRequestDto filho = filhos.get(i);
                if (filho.getElementOrder() == null) filho.setElementOrder(i + 1);
                salvarItemNormativoRecursivo(documento, filho, item);
            }
        }
    }

    private void salvarItensFinais(Documento documento, List<SecaoItemRequestDto> dtos) {
        itemParteFinalRepository.deleteAllByDocumentoId(documento.getId());
        for (int i = 0; i < dtos.size(); i++) {
            SecaoItemRequestDto dto = dtos.get(i);
            ItemParteFinal item = new ItemParteFinal();
            item.setDocumento(documento);
            item.setTipo(dto.getTipo());
            item.setElementOrder(dto.getElementOrder() != null ? dto.getElementOrder() : i + 1);
            item.setTitulo(dto.getTitulo());
            item.setConteudo(dto.getConteudo());
            item.setFullTextContent(gerarFullTextContent(dto.getTitulo(), dto.getConteudo(), dto.getFullTextContent()));
            itemParteFinalRepository.save(item);
        }
    }

    // ─── Adicionar item individual ────────────────────────────────────────────────

    public DocumentoResponseComAnexoTextualDto adicionarItemAoDocumento(Long idDocumento, ItemAnexoParteNormativaRequestDto dto) {
        Documento documento = documentoRepository.findById(idDocumento)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        ItemAnexoParteNormativa novoItem = new ItemAnexoParteNormativa();
        novoItem.setDocumento(documento);
        novoItem.setTipo(dto.getTipo());
        novoItem.setTitulo(dto.getTitulo());
        novoItem.setConteudo(dto.getConteudo());

        if (dto.getParentId() != null) {
            ItemAnexoParteNormativa parent = itemAnexoParteNormativaRepository.findById(dto.getParentId())
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
