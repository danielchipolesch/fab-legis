package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos.EventoEstruturaDto;
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
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.infrastructure.notificacao.DocumentoPresencaEmitterRegistry;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemParteFinalRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemPartePreliminarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Autowired
    DocumentoPresencaEmitterRegistry presencaEmitterRegistry;

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
    public void salvarSecoes(Long documentoId, SecoesSaveRequestDto request, String clientId) {
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
        if (!normativos.isEmpty()) salvarItensNormativos(documento, normativos, clientId);
    }

    // Pacote (não private): reutilizado por DocumentoStatusService para
    // gravar a parte preliminar (epígrafe/ementa/preâmbulo/fecho/assinatura)
    // no momento da publicação -- ver changeStatus().
    void salvarItensPreliminares(Documento documento, List<SecaoItemRequestDto> dtos) {
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

    // Diff contra o que já está persistido (casando por id), em vez de apagar a árvore
    // inteira e reinserir -- ver ADR na seção "Isolamento entre reordenação estrutural
    // e conteúdo ao vivo" do plano de colaboração em tempo real. Crucial: o campo
    // `conteudo` NUNCA é escrito aqui para um item já existente (só na criação) --
    // conteúdo de elemento já persistido é responsabilidade exclusiva de
    // atualizarConteudoElemento(), para não sobrescrever silenciosamente o que outra
    // pessoa esteja editando ao vivo no mesmo instante.
    private void salvarItensNormativos(Documento documento, List<SecaoItemRequestDto> dtos, String clientId) {
        List<ItemAnexoParteNormativa> existentes = itemAnexoParteNormativaRepository.findAllByDocumentoId(documento.getId());
        Map<Long, ItemAnexoParteNormativa> existentesPorId = existentes.stream()
                .collect(Collectors.toMap(ItemAnexoParteNormativa::getId, i -> i));
        Set<Long> vistos = new HashSet<>();
        // Acumula só as mudanças estruturais REAIS (não um evento por autosave) para
        // transmitir via SSE (event: estrutura) a quem mais estiver com o documento
        // aberto -- ver DocumentoPresencaEmitterRegistry.transmitirEstrutura.
        List<EventoEstruturaDto> eventos = new ArrayList<>();

        for (int i = 0; i < dtos.size(); i++) {
            SecaoItemRequestDto dto = dtos.get(i);
            int elementOrder = dto.elementOrder() != null ? dto.elementOrder() : i + 1;
            aplicarItemNormativoRecursivo(documento, dto, elementOrder, null, existentesPorId, vistos, eventos);
        }

        // Remove o que não veio na requisição. Ordenado do mais profundo para o mais
        // raso: a FK parent_id não tem ON DELETE CASCADE, então excluir um pai antes
        // dos próprios filhos (que também estão nesta lista de órfãos, já que a
        // travessia acima nunca visita descendente de nó ausente na árvore recebida)
        // quebraria a constraint.
        existentes.stream()
                .filter(item -> !vistos.contains(item.getId()))
                .sorted(Comparator.comparingInt(this::calcularProfundidade).reversed())
                .forEach(item -> {
                    itemAnexoParteNormativaRepository.delete(item);
                    eventos.add(EventoEstruturaDto.excluido(item.getId()));
                });

        presencaEmitterRegistry.transmitirEstrutura(documento.getId(), clientId, eventos);
    }

    private int calcularProfundidade(ItemAnexoParteNormativa item) {
        int profundidade = 0;
        for (ItemAnexoParteNormativa atual = item.getParent(); atual != null; atual = atual.getParent()) {
            profundidade++;
        }
        return profundidade;
    }

    private void aplicarItemNormativoRecursivo(Documento documento, SecaoItemRequestDto dto, int elementOrder,
                                                ItemAnexoParteNormativa parent,
                                                Map<Long, ItemAnexoParteNormativa> existentesPorId, Set<Long> vistos,
                                                List<EventoEstruturaDto> eventos) {
        ItemAnexoParteNormativa item = dto.id() != null ? existentesPorId.get(dto.id()) : null;
        boolean novo = item == null;
        Long parentIdAntigo = null;
        Integer orderAntigo = null;
        String tituloAntigo = null;
        if (novo) {
            item = new ItemAnexoParteNormativa();
            item.setDocumento(documento);
            // children começa null (sem inicializador no campo); precisa de uma lista
            // real antes do save, senão uma releitura no mesmo contexto de persistência
            // (open-in-view devolve a MESMA instância gerenciada em vez de re-hidratar)
            // quebra em carregarChildrenRecursivamente() -> getChildren().clear().
            item.setChildren(new ArrayList<>());
            item.setConteudo(dto.conteudo());
            item.setFullTextContent(gerarFullTextContent(dto.titulo(), dto.conteudo(), dto.fullTextContent()));
        } else {
            vistos.add(item.getId());
            parentIdAntigo = item.getParent() != null ? item.getParent().getId() : null;
            orderAntigo = item.getElementOrder();
            tituloAntigo = item.getTitulo();
        }
        item.setTipo(dto.tipo());
        item.setElementOrder(elementOrder);
        item.setTitulo(dto.titulo());
        item.setParent(parent);
        itemAnexoParteNormativaRepository.save(item);

        // Só entra no evento se algo estrutural de fato mudou -- um autosave que só
        // regravou o mesmo parent/ordem/titulo (ex.: digitação de conteúdo, que nem
        // passa por aqui, ou um save disparado por outro motivo) não deve gerar ruído
        // para quem mais está com o documento aberto.
        Long parentIdNovo = parent != null ? parent.getId() : null;
        if (novo) {
            eventos.add(EventoEstruturaDto.criado(item.getId(), parentIdNovo, item.getTipo(), item.getElementOrder(), item.getTitulo()));
        } else if (!Objects.equals(parentIdAntigo, parentIdNovo)
                || !Objects.equals(orderAntigo, elementOrder)
                || !Objects.equals(tituloAntigo, dto.titulo())) {
            eventos.add(EventoEstruturaDto.atualizado(item.getId(), parentIdNovo, item.getTipo(), item.getElementOrder(), item.getTitulo()));
        }

        List<SecaoItemRequestDto> filhos = dto.filhos();
        if (filhos != null) {
            for (int i = 0; i < filhos.size(); i++) {
                SecaoItemRequestDto filho = filhos.get(i);
                int filhoOrder = filho.elementOrder() != null ? filho.elementOrder() : i + 1;
                aplicarItemNormativoRecursivo(documento, filho, filhoOrder, item, existentesPorId, vistos, eventos);
            }
        }
    }

    // ─── Conteúdo por elemento (colaboração ao vivo) ───────────────────────────────

    // Único ponto de escrita do campo `conteudo` para um elemento já existente --
    // chamado pelo serviço de colaboração (Hocuspocus) a cada persistência do Y.Doc,
    // nunca pelo salvamento em massa acima. Ver plano de colaboração em tempo real.
    @Transactional
    public void atualizarConteudoElemento(Long documentoId, Long elementoId, String conteudo) {
        ItemAnexoParteNormativa item = itemAnexoParteNormativaRepository.findById(elementoId)
                .orElseThrow(() -> new ResourceNotFoundException("Elemento não encontrado."));
        if (!item.getDocumento().getId().equals(documentoId)) {
            throw new ResourceNotFoundException("Elemento não encontrado.");
        }
        item.setConteudo(conteudo);
        item.setFullTextContent(gerarFullTextContent(item.getTitulo(), conteudo, null));
        itemAnexoParteNormativaRepository.save(item);
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
