package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestUpdateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResumoResponseDto;
import br.com.danielchipolesch.domain.builders.DocumentoBuilder;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemPartePreliminar;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemParteFinal;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.AssuntoBasicoException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentoException;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.EspecieNormativaException;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Anexo;
import br.com.danielchipolesch.infrastructure.security.AutenticacaoUtil;
import br.com.danielchipolesch.infrastructure.repositories.AnexoRepository;
import br.com.danielchipolesch.infrastructure.repositories.AssuntoBasicoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.EspecieNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoHistoricoRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemParteFinalRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemPartePreliminarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class DocumentoService {

    @Autowired
    DocumentoRepository documentoRepository;

    @Autowired
    EspecieNormativaRepository especieNormativaRepository;

    @Autowired
    AssuntoBasicoRepository assuntoBasicoRepository;

    @Autowired
    ItemAnexoParteNormativaRepository itemAnexoParteNormativaRepository;

    @Autowired
    ItemPartePreliminarRepository itemPartePreliminarRepository;

    @Autowired
    ItemParteFinalRepository itemParteFinalRepository;

    @Autowired
    DocumentoHistoricoRepository documentoHistoricoRepository;

    @Autowired
    DocumentoHistoricoService documentoHistoricoService;

    @Autowired
    AnexoRepository anexoRepository;


    @Transactional
    public DocumentoResponseSemAnexoTextualDto create(DocumentoRequestCreateDto request) throws RuntimeException {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(request.idEspecieNormativa()).orElseThrow(() -> new ResourceNotFoundException(EspecieNormativaException.NOT_FOUND.getMessage()));
        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findById(request.idAssuntoBasico()).orElseThrow(() ->  new ResourceNotFoundException(AssuntoBasicoException.NOT_FOUND.getMessage()));

        var secondaryNumber = this.calculateSecondaryNumber(especieNormativa, assuntoBasico);
        var usuarioAtual = AutenticacaoUtil.usuarioAtual();

        Documento documento = new DocumentoBuilder()
                .especieNormativa(especieNormativa)
                .assuntoBasico(assuntoBasico)
                .numeroSecundario(secondaryNumber)
                .tituloDocumento(request.tituloDocumento())
                .documentoStatus(DocumentoStatusEnum.RASCUNHO)
                .autor(usuarioAtual)
                .om(usuarioAtual.getOm())
                .build();

        Documento salvo = documentoRepository.save(documento);
        documentoHistoricoService.registrar(salvo, TipoAlteracaoEnum.CRIACAO,
                "Documento criado", null, DocumentoStatusEnum.RASCUNHO);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(salvo);
    }

    public Documento getById(Long id) throws RuntimeException{

        return documentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));
    }

    public List<DocumentoResponseSemAnexoTextualDto> getByEspecieNormativaAndAssuntoBasico(Long especieNormativaId, Long assuntoBasicoId) throws ResourceNotFoundException {

        var especieNormativa = especieNormativaRepository.findById(especieNormativaId).orElseThrow(() -> new ResourceNotFoundException(EspecieNormativaException.NOT_FOUND.getMessage()));
        var assuntoBasico = assuntoBasicoRepository.findById(assuntoBasicoId).orElseThrow(() -> new ResourceNotFoundException(AssuntoBasicoException.NOT_FOUND.getMessage()));

        List<Documento> documents = documentoRepository.findByEspecieNormativaAndAssuntoBasico(especieNormativa, assuntoBasico);

        return documents.stream().map(DocumentoMapper::documentoToDocumentoSemAnexoTextualResponseDto).toList();
    }

    // Substitui o antigo getAll(Pageable) (que descartava a metadados de paginação,
    // devolvendo só List<Documento> -- olho no findAll(pageable) só pra bater com a
    // assinatura, sem nunca expor totalElements pro frontend). Agora devolve o Page
    // inteiro, e aceita os mesmos filtros que a HomePage já mostrava (mas calculava
    // no navegador, sobre um array carregado uma vez) -- ver DocumentoSpecifications.
    public Page<Documento> getAllPaginado(Long usuarioId, Long omId, String aba, String busca,
                                           String especieSigla, DocumentoStatusEnum status,
                                           Pageable pageable) {
        Specification<Documento> spec = DocumentoSpecifications.aba(aba, usuarioId, omId)
                .and(DocumentoSpecifications.busca(busca))
                .and(DocumentoSpecifications.especieSigla(especieSigla))
                .and(DocumentoSpecifications.status(status));
        return documentoRepository.findAll(spec, pageable);
    }

    // Contagens pros badges das 4 abas (porAba -- ignora busca/espécie/status de
    // propósito, é uma referência estável que não muda enquanto o usuário filtra, mesmo
    // comportamento de contagemAbas em HomePage.vue) e pros chips de situação da aba
    // ativa (porStatus, com aba/busca/espécie aplicados, um count() por valor de
    // DocumentoStatusEnum -- mesmo comportamento de statusSummary). Um
    // Specification.count() por número, sem GROUP BY: mantém tudo dentro do que
    // JpaSpecificationExecutor já oferece, sem query nativa.
    public DocumentoResumoResponseDto getResumo(Long usuarioId, Long omId, String aba, String busca, String especieSigla) {
        Map<String, Long> porAba = new LinkedHashMap<>();
        for (String nomeAba : List.of("meus", "minha_om", "outras_oms", "revogados")) {
            porAba.put(nomeAba, documentoRepository.count(DocumentoSpecifications.aba(nomeAba, usuarioId, omId)));
        }

        Specification<Documento> comAbaBuscaEEspecie = DocumentoSpecifications.aba(aba, usuarioId, omId)
                .and(DocumentoSpecifications.busca(busca))
                .and(DocumentoSpecifications.especieSigla(especieSigla));
        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (DocumentoStatusEnum statusEnum : DocumentoStatusEnum.values()) {
            long total = documentoRepository.count(comAbaBuscaEEspecie.and(DocumentoSpecifications.status(statusEnum)));
            if (total > 0) porStatus.put(statusEnum.name(), total);
        }

        return new DocumentoResumoResponseDto(porAba, porStatus);
    }

    @Transactional
    public DocumentoResponseSemAnexoTextualDto update(Long id, DocumentoRequestUpdateDto request) throws RuntimeException {

        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        if (documento.getDocumentoStatus() != DocumentoStatusEnum.RASCUNHO
                && documento.getDocumentoStatus() != DocumentoStatusEnum.MINUTA) {
            throw new StatusCannotBeUpdatedException(DocumentoException.CANNOT_BE_UPDATED.getMessage());
        }

        boolean tituloAlterado = !documento.getTituloDocumento().equals(request.tituloDocumento());
        documento.setTituloDocumento(request.tituloDocumento());
        if (request.numeroSecundario() != null) {
            documento.setNumeroSecundario(request.numeroSecundario());
        }
        // saveAndFlush, não save: o @Version só é incrementado no INSTANTE do
        // flush, que por padrão só aconteceria no commit da transação -- DEPOIS
        // deste método já ter retornado. Sem o flush explícito aqui, quando o
        // título/número realmente muda, o DTO de resposta carrega a versão
        // ANTIGA (pré-bump), e o próximo salvamento do editor usa essa versão
        // desatualizada como versaoEsperada -- gerando um 409 de "editado por
        // outra pessoa" mesmo sendo o mesmo usuário. Ver DocumentoConcorrenciaService.
        Documento atualizado = documentoRepository.saveAndFlush(documento);
        if (tituloAlterado) {
            documentoHistoricoService.registrar(atualizado, TipoAlteracaoEnum.ALTERACAO_METADADOS,
                    "Título atualizado", null, null);
        }
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(atualizado);
    }

    @Transactional
    public DocumentoResponseSemAnexoTextualDto delete(Long id) throws RuntimeException {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        DocumentoStatusEnum status = documento.getDocumentoStatus();
        if (status != DocumentoStatusEnum.RASCUNHO && status != DocumentoStatusEnum.MINUTA) {
            throw new StatusCannotBeUpdatedException(DocumentoException.CANNOT_BE_DELETED.getMessage());
        }

        documentoHistoricoRepository.deleteAllByDocumentoId(id);
        itemPartePreliminarRepository.deleteAllByDocumentoId(id);
        itemAnexoParteNormativaRepository.nullifyParentsForDocument(id);
        itemAnexoParteNormativaRepository.deleteAllByDocumentoId(id);
        itemParteFinalRepository.deleteAllByDocumentoId(id);
        anexoRepository.deleteAllByDocumentoId(id);
        documentoRepository.delete(documento);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(documento);
    }

    @Transactional
    public DocumentoResponseSemAnexoTextualDto clone(Long id) throws RuntimeException {

        Documento documentoAntigo = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        var secondaryNumber = this.calculateSecondaryNumber(documentoAntigo.getEspecieNormativa(), documentoAntigo.getAssuntoBasico());
        var usuarioAtual = AutenticacaoUtil.usuarioAtual();

        // O clone é um documento novo (ver clonarNormItem): quem clona vira o
        // autor, não quem criou o original -- mesma regra de "criar" no resto
        // do sistema.
        Documento documentoNovo = new DocumentoBuilder()
                .especieNormativa(documentoAntigo.getEspecieNormativa())
                .assuntoBasico(documentoAntigo.getAssuntoBasico())
                .numeroSecundario(secondaryNumber)
                .tituloDocumento(documentoAntigo.getTituloDocumento())
                .documentoStatus(DocumentoStatusEnum.RASCUNHO)
                .autor(usuarioAtual)
                .om(usuarioAtual.getOm())
                .build();

        documentoAntigo.setQtdReplicas(documentoAntigo.getQtdReplicas() + 1);
        documentoRepository.save(documentoAntigo);

        Documento clonado = documentoRepository.save(documentoNovo);

        for (ItemPartePreliminar orig : itemPartePreliminarRepository.findByDocumentoIdOrderByElementOrderAsc(id)) {
            ItemPartePreliminar copia = new ItemPartePreliminar();
            copia.setDocumento(clonado);
            copia.setTipo(orig.getTipo());
            copia.setElementOrder(orig.getElementOrder());
            copia.setTitulo(orig.getTitulo());
            copia.setConteudo(orig.getConteudo());
            copia.setFullTextContent(orig.getFullTextContent());
            itemPartePreliminarRepository.save(copia);
        }

        for (ItemAnexoParteNormativa root : itemAnexoParteNormativaRepository.findRootItemsByDocumentoId(id)) {
            clonarNormItem(root, clonado, null);
        }

        for (ItemParteFinal orig : itemParteFinalRepository.findByDocumentoIdOrderByElementOrderAsc(id)) {
            ItemParteFinal copia = new ItemParteFinal();
            copia.setDocumento(clonado);
            copia.setTipo(orig.getTipo());
            copia.setElementOrder(orig.getElementOrder());
            copia.setTitulo(orig.getTitulo());
            copia.setConteudo(orig.getConteudo());
            copia.setFullTextContent(orig.getFullTextContent());
            itemParteFinalRepository.save(copia);
        }

        for (Anexo orig : anexoRepository.findByDocumentoIdOrderByOrdemAsc(id)) {
            Anexo copia = new Anexo();
            copia.setDocumento(clonado);
            copia.setTitulo(orig.getTitulo());
            copia.setUrlImagem(orig.getUrlImagem());
            copia.setOrdem(orig.getOrdem());
            anexoRepository.save(copia);
        }

        documentoHistoricoService.registrar(clonado, TipoAlteracaoEnum.CLONAGEM,
                "Clonado do documento #" + id, null, DocumentoStatusEnum.RASCUNHO);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(clonado);
    }

    // O clone é um documento novo, sem histórico de emenda: carrega só a redação
    // vigente de cada elemento, nunca o histórico de alterações.
    private void clonarNormItem(ItemAnexoParteNormativa original, Documento novoDoc, ItemAnexoParteNormativa novoParent) {
        // Elemento revogado não faz mais parte do documento vigente -- nem ele nem
        // seus filhos (ex.: incisos de um artigo revogado) vão para o clone.
        if (original.getEmendaStatus() == ElementoEmendaStatusEnum.REVOGADO) {
            return;
        }

        ItemAnexoParteNormativa copia = new ItemAnexoParteNormativa();
        copia.setDocumento(novoDoc);
        copia.setParent(novoParent);
        copia.setTipo(original.getTipo());
        copia.setElementOrder(original.getElementOrder());

        // ALTERADO: conteudo/titulo guardam a redação ANTERIOR à emenda (usada para
        // riscar no PDF oficial — ver DocumentoFoCorpoBuilder); a vigente é
        // conteudoEmenda/tituloEmenda. Nos demais casos (INALTERADO, INCLUIDO já
        // consolidado ou ainda pendente) conteudo/titulo já são a redação atual.
        String titulo, conteudo;
        if (original.getEmendaStatus() == ElementoEmendaStatusEnum.ALTERADO) {
            titulo   = original.getTituloEmenda() != null ? original.getTituloEmenda() : original.getTitulo();
            conteudo = original.getConteudoEmenda();
        } else {
            titulo   = original.getTitulo();
            conteudo = original.getConteudo();
        }
        copia.setTitulo(titulo);
        copia.setConteudo(conteudo);
        copia.setFullTextContent(gerarFullTextContent(titulo, conteudo, null));

        ItemAnexoParteNormativa salva = itemAnexoParteNormativaRepository.save(copia);
        if (original.getChildren() != null) {
            for (ItemAnexoParteNormativa filho : original.getChildren()) {
                clonarNormItem(filho, novoDoc, salva);
            }
        }
    }

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

    private Integer calculateSecondaryNumber(EspecieNormativa especieNormativa, AssuntoBasico assuntoBasico){

        List<Documento> documents = documentoRepository.findByEspecieNormativaAndAssuntoBasico(especieNormativa, assuntoBasico);

        if (documents.isEmpty()) {
            return 1;
        }

        List<Integer> secondaryNumbers = documents.stream()
                .map(Documento::getNumeroSecundario)
                .sorted()
                .toList();

        for (int i = 1; i <= secondaryNumbers.size(); i++) {
            if (!secondaryNumbers.contains(i)) {
                return i;
            }
        }

        return secondaryNumbers.size() + 1;
    }
}
