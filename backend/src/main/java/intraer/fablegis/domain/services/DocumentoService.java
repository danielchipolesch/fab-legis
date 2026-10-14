package intraer.fablegis.domain.services;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.dtos.documentoDtos.DocumentoFilaResponseDto;
import intraer.fablegis.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import intraer.fablegis.application.dtos.documentoDtos.DocumentoRequestUpdateDto;
import intraer.fablegis.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import intraer.fablegis.application.dtos.documentoDtos.DocumentoResumoResponseDto;
import intraer.fablegis.domain.builders.DocumentoBuilder;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.regras.TipoDeEspecie;
import intraer.fablegis.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.ElementoEmendaStatusEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemAnexoParteNormativa;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemPartePreliminar;
import intraer.fablegis.domain.entities.estruturaDocumento.ItemParteFinal;
import intraer.fablegis.domain.entities.numeracaoDocumento.AssuntoBasico;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.handlers.exceptions.enums.AssuntoBasicoException;
import intraer.fablegis.domain.handlers.exceptions.enums.DocumentoException;
import intraer.fablegis.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import intraer.fablegis.application.dtos.documentoDtos.SubstituicaoDeDocumentoDto;
import intraer.fablegis.domain.handlers.exceptions.enums.EspecieNormativaException;
import intraer.fablegis.domain.mappers.DocumentoMapper;
import intraer.fablegis.domain.regras.RegrasDasEspecies;
import intraer.fablegis.domain.entities.estruturaDocumento.Anexo;
import intraer.fablegis.infrastructure.security.AutenticacaoUtil;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.AssuntoBasicoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import intraer.fablegis.infrastructure.repositories.EspecieNormativaRepository;
import intraer.fablegis.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoHistoricoRepository;
import intraer.fablegis.infrastructure.repositories.ItemParteFinalRepository;
import intraer.fablegis.infrastructure.repositories.ItemPartePreliminarRepository;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class DocumentoService {

    final DocumentoRepository documentoRepository;

    final DocumentoCompartilhamentoRepository documentoCompartilhamentoRepository;

    final EspecieNormativaRepository especieNormativaRepository;

    final AssuntoBasicoRepository assuntoBasicoRepository;

    final ItemAnexoParteNormativaRepository itemAnexoParteNormativaRepository;

    final ItemPartePreliminarRepository itemPartePreliminarRepository;

    final ItemParteFinalRepository itemParteFinalRepository;

    final DocumentoHistoricoRepository documentoHistoricoRepository;

    final DocumentoHistoricoService documentoHistoricoService;

    final RegrasDasEspecies regras;

    final AnexoRepository anexoRepository;

    final OrganizacaoMilitarRepository organizacaoMilitarRepository;

    final HabilitacaoDeCriacaoService habilitacaoDeCriacaoService;

    final CriacaoComNumeracaoUnica numeracaoUnica;

    private static Long omDoUsuarioAtual() {
        var om = AutenticacaoUtil.usuarioAtual().getOm();
        return om == null ? null : om.getId();
    }

    // O número do documento é único no banco; se outra criação simultânea tomou o mesmo, refaz com o seguinte (CriacaoComNumeracaoUnica).
    public DocumentoResponseSemAnexoTextualDto create(DocumentoRequestCreateDto request) {
        return numeracaoUnica.executar(() -> criar(request));
    }

    private DocumentoResponseSemAnexoTextualDto criar(DocumentoRequestCreateDto request) {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(request.idEspecieNormativa()).orElseThrow(() -> new ResourceNotFoundException(EspecieNormativaException.NOT_FOUND.getMessage()));

        // Só a OM habilitada cria espécie convencional (a NPA, qualquer OM): ver HabilitacaoDeCriacaoService.
        habilitacaoDeCriacaoService.exigirPodeCriar(omDoUsuarioAtual(), especieNormativa);

        // O que a espécie exige para criar (assunto básico, identificação...) e como o documento se identifica
        // é regra da espécie.
        Documento documento = regras.para(especieNormativa).criacao()
                .montar(request, especieNormativa, AutenticacaoUtil.usuarioAtual());

        Documento salvo = documentoRepository.save(documento);
        // A estrutura inicial é regra da espécie (atos normativos: capítulos padronizados da NSCA 5-3).
        regras.para(especieNormativa).estruturaInicial().criarEm(salvo);
        regras.para(especieNormativa).camposEspecificos().criarPara(salvo);
        documentoHistoricoService.registrar(salvo, TipoAlteracaoEnum.CRIACAO,
                "Documento criado", null, SituacaoLocalEnum.RASCUNHO);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(salvo);
    }

    public Documento getById(Long id) {

        return documentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));
    }

    // Fila da tela de Revisão (RevisaoPage.vue): o que aguarda aprovação na OM de quem pergunta -- no fluxo normal (EM_REVISAO) e na
    // análise de revogação (ANALISE_REVOGACAO). Só quem tem o papel APROV vê a fila; qualquer aprovador da OM age.
    public List<DocumentoFilaResponseDto> getFilaDeRevisao(Usuario usuario) {
        if (!usuario.getPapeis().contains(PapelEnum.APR_OM)) return List.of();
        return documentoRepository.findByOmIdAndSituacaoLocalIn(usuario.getOm().getId(),
                        List.copyOf(DocumentoSpecifications.SITUACOES_DE_REVISAO))
                .stream().map(this::toFilaResponseDto).toList();
    }

    // Fila da tela de Publicação (PublicacaoPage.vue): o que aguarda publicação, de TODAS as OMs -- a publicação é do CENDOC (o papel
    // PUBLIC só existe lá) -- no fluxo normal (EM_PUBLICACAO) e na revogação (EM_REVOGACAO).
    public List<DocumentoFilaResponseDto> getFilaDePublicacao(Usuario usuario) {
        if (!usuario.getPapeis().contains(PapelEnum.PUB_BCA)) return List.of();
        return documentoRepository.findBySituacaoLocalIn(List.copyOf(DocumentoSpecifications.SITUACOES_DE_PUBLICACAO))
                .stream().map(this::toFilaResponseDto).toList();
    }

    private DocumentoFilaResponseDto toFilaResponseDto(Documento documento) {
        List<String> autores = new ArrayList<>();
        autores.add(documento.getAutor().getNome());
        documentoCompartilhamentoRepository.findByDocumentoId(documento.getId())
                .forEach(c -> autores.add(c.getUsuario().getNome()));
        return new DocumentoFilaResponseDto(
                documento.getId(),
                documento.getIdentificacao(),
                documento.getTituloDocumento(),
                documento.getSituacaoBca(),
                documento.getSituacaoLocal(),
                autores
        );
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
                                           TipoDeEspecie tipoDeEspecie, String especieSigla, SituacaoBcaEnum situacaoBca,
                                           SituacaoLocalEnum situacaoLocal, Pageable pageable) {
        Specification<Documento> spec = DocumentoSpecifications.aba(aba, usuarioId, omId)
                .and(DocumentoSpecifications.tipoDeEspecie(tipoDeEspecie))
                .and(DocumentoSpecifications.busca(busca))
                .and(DocumentoSpecifications.especieSigla(especieSigla))
                .and(DocumentoSpecifications.situacaoBca(situacaoBca))
                .and(DocumentoSpecifications.situacaoLocal(situacaoLocal));
        return documentoRepository.findAll(spec, pageable);
    }

    // Os três cards do hub (tela inicial), de qualquer módulo -- ver DocumentoSpecifications.
    public Page<Documento> getPainelMinhasEmTramitacao(Long usuarioId, Pageable pageable) {
        return documentoRepository.findAll(DocumentoSpecifications.minhasEmTramitacao(usuarioId), pageable);
    }

    public Page<Documento> getPainelEmTramitacaoDeOutros(Long usuarioId, Pageable pageable) {
        return documentoRepository.findAll(DocumentoSpecifications.emTramitacaoDeOutros(usuarioId), pageable);
    }

    public Page<Documento> getPainelAguardandoAcao(Usuario usuario, Pageable pageable) {
        return documentoRepository.findAll(DocumentoSpecifications.aguardandoAcaoDe(usuario), pageable);
    }

    public Page<Documento> getPainelPublicadosERevogados(Pageable pageable) {
        return documentoRepository.findAll(DocumentoSpecifications.publicadosERevogados(), pageable);
    }

    // Contagens pros badges das 4 abas (porAba -- ignora busca/espécie/situação de
    // propósito, é uma referência estável que não muda enquanto o usuário filtra, mesmo
    // comportamento de contagemAbas em HomePage.vue) e pros chips de situação da aba
    // ativa (porSituacaoBca e porSituacaoLocal, com aba/busca/espécie aplicados, um count()
    // por valor de cada enum -- mesmo comportamento de statusSummary). Um
    // Specification.count() por número, sem GROUP BY: mantém tudo dentro do que
    // JpaSpecificationExecutor já oferece, sem query nativa.
    public DocumentoResumoResponseDto getResumo(Long usuarioId, Long omId, String aba, String busca,
                                                TipoDeEspecie tipoDeEspecie, String especieSigla) {
        Map<String, Long> porAba = new LinkedHashMap<>();
        for (String nomeAba : List.of("meus", "minha_om", "outras_oms", "revogados")) {
            porAba.put(nomeAba, documentoRepository.count(DocumentoSpecifications.aba(nomeAba, usuarioId, omId)
                    .and(DocumentoSpecifications.tipoDeEspecie(tipoDeEspecie))));
        }

        Specification<Documento> comAbaBuscaEEspecie = DocumentoSpecifications.aba(aba, usuarioId, omId)
                .and(DocumentoSpecifications.tipoDeEspecie(tipoDeEspecie))
                .and(DocumentoSpecifications.busca(busca))
                .and(DocumentoSpecifications.especieSigla(especieSigla));
        Map<String, Long> porSituacaoBca = new LinkedHashMap<>();
        for (SituacaoBcaEnum situacao : SituacaoBcaEnum.values()) {
            long total = documentoRepository.count(comAbaBuscaEEspecie.and(DocumentoSpecifications.situacaoBca(situacao)));
            if (total > 0) porSituacaoBca.put(situacao.name(), total);
        }
        Map<String, Long> porSituacaoLocal = new LinkedHashMap<>();
        for (SituacaoLocalEnum situacao : SituacaoLocalEnum.values()) {
            long total = documentoRepository.count(comAbaBuscaEEspecie.and(DocumentoSpecifications.situacaoLocal(situacao)));
            if (total > 0) porSituacaoLocal.put(situacao.name(), total);
        }

        return new DocumentoResumoResponseDto(porAba, porSituacaoBca, porSituacaoLocal);
    }

    @Transactional
    public DocumentoResponseSemAnexoTextualDto update(Long id, DocumentoRequestUpdateDto request) {

        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        // Segunda barreira, da etapa: o @PreAuthorize podeEditar do controller já decide QUEM pode chamar isto e em
        // QUE etapa (DocumentoAcessoService.podeEditar); esta garante que a etapa seja barrada mesmo se o serviço for
        // chamado por outro caminho. É a mesma regra (SituacaoLocalEnum.aceitaEdicao), que bate com o isReadonly do
        // editor (DocumentoEditorPage.vue).
        if (!documento.getSituacaoLocal().aceitaEdicao()) {
            throw new StatusCannotBeUpdatedException(DocumentoException.CANNOT_BE_UPDATED.getMessage());
        }

        // A identificação (o "código") só muda se a espécie admite -- a NPA, sim; as convencionais têm a gerada. Ver
        // RegrasDeCriacaoDoDocumento.novaIdentificacao (que também barra pelo status).
        var novaIdentificacao = regras.para(documento.getEspecieNormativa()).criacao()
                .novaIdentificacao(documento, request.identificacao());

        boolean tituloAlterado = !documento.getTituloDocumento().equals(request.tituloDocumento());
        novaIdentificacao.ifPresent(documento::setIdentificacao);
        documento.setTituloDocumento(request.tituloDocumento());
        if (request.numeroSecundario() != null) {
            documento.setNumeroSecundario(request.numeroSecundario());
        }

        // OM impressa na capa (NSCA 5-3, Art. 17, II) -- por padrão a do autor no
        // momento da criação (ver create() abaixo), mas alterável enquanto o
        // documento ainda não avançou no fluxo. Mais restrito que
        // statusPermiteAtualizacao acima de propósito: uma vez em EM_ALTERACAO/
        // EM_REVISAO o documento já saiu das mãos de quem só edita conteúdo, não
        // faz sentido trocar a OM que assina o ato nesse ponto. Só age quando o
        // valor difere do atual -- o autosave estrutural do editor também passa
        // por aqui reenviando o documento inteiro (om_id incluso), mas sempre
        // com o valor já vigente, então essa checagem o torna um no-op ali.
        boolean omAlterada = false;
        if (request.omId() != null && !request.omId().equals(documento.getOm().getId())) {
            var statusPermiteAlterarOm = documento.getSituacaoLocal() == SituacaoLocalEnum.RASCUNHO
                    || documento.getSituacaoLocal() == SituacaoLocalEnum.MINUTA;
            if (!statusPermiteAlterarOm) {
                throw new StatusCannotBeUpdatedException(
                        "A organização militar só pode ser alterada enquanto o documento está em Rascunho ou Minuta.");
            }
            OrganizacaoMilitar novaOm = organizacaoMilitarRepository.findById(request.omId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organização militar não encontrada."));
            documento.setOm(novaOm);
            omAlterada = true;
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
        if (novaIdentificacao.isPresent()) {
            documentoHistoricoService.registrar(atualizado, TipoAlteracaoEnum.ALTERACAO_METADADOS,
                    "Identificação atualizada para " + novaIdentificacao.get(), null, null);
        }
        if (omAlterada) {
            documentoHistoricoService.registrar(atualizado, TipoAlteracaoEnum.ALTERACAO_METADADOS,
                    "Organização militar atualizada para " + atualizado.getOm().getNome(), null, null);
        }
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(atualizado);
    }

    @Transactional
    public DocumentoResponseSemAnexoTextualDto delete(Long id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        SituacaoLocalEnum status = documento.getSituacaoLocal();
        if (status != SituacaoLocalEnum.RASCUNHO && status != SituacaoLocalEnum.MINUTA) {
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

    public DocumentoResponseSemAnexoTextualDto clone(Long id) {
        return numeracaoUnica.executar(() -> clonar(id));
    }

    private DocumentoResponseSemAnexoTextualDto clonar(Long id) {
        Documento clonado = copiarParaNovoDocumento(id);
        documentoHistoricoService.registrar(clonado, TipoAlteracaoEnum.CLONAGEM,
                "Clonado do documento #" + id, null, SituacaoLocalEnum.RASCUNHO);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(clonado);
    }

    // "Substituir" (NPA): para mudar uma NPA publicada cria-se outra e revoga-se a anterior. Esta ação faz a primeira metade
    // de forma explícita: cria a NPA nova (cópia em RASCUNHO, da OM de quem substitui) já ligada à substituída. A revogação
    // da antiga continua sendo o pedido de revogação de sempre, feito depois (a nova ainda nem foi publicada).
    public DocumentoResponseSemAnexoTextualDto substituir(Long id) {
        return numeracaoUnica.executar(() -> substituirPor(id));
    }

    private DocumentoResponseSemAnexoTextualDto substituirPor(Long id) {
        Documento substituido = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        var ciclo = regras.para(substituido.getEspecieNormativa()).cicloDeVida();
        if (!ciclo.podeSerSubstituido(substituido.getSituacaoLocal(), substituido.getSituacaoBca())) {
            throw new StatusCannotBeUpdatedException(DocumentoException.CANNOT_BE_REPLACED.getMessage());
        }
        documentoRepository.findFirstByDocumentoSubstituidoIdAndSituacaoLocalNotOrderByIdDesc(id, SituacaoLocalEnum.CANCELADO)
                .ifPresent(existente -> {
                    throw new StatusCannotBeUpdatedException(
                            "Esta NPA já está sendo substituída pelo documento " + existente.getIdentificacao() + ".");
                });

        Documento nova = copiarParaNovoDocumento(id);
        nova.setDocumentoSubstituido(substituido);
        documentoRepository.save(nova);
        documentoHistoricoService.registrar(nova, TipoAlteracaoEnum.SUBSTITUICAO,
                "Substitui o documento #" + id + " (" + substituido.getIdentificacao() + ")", null, SituacaoLocalEnum.RASCUNHO);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(nova);
    }

    // Os dois lados da ligação "Substituir" (a NPA que este documento substitui e a que o substitui), para a tela mostrar.
    @Transactional(readOnly = true)
    public SubstituicaoDeDocumentoDto substituicaoDe(Long id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));
        var substituidoPor = documentoRepository
                .findFirstByDocumentoSubstituidoIdAndSituacaoLocalNotOrderByIdDesc(id, SituacaoLocalEnum.CANCELADO)
                .orElse(null);
        return new SubstituicaoDeDocumentoDto(
                SubstituicaoDeDocumentoDto.Ponta.de(documento.getDocumentoSubstituido()),
                SubstituicaoDeDocumentoDto.Ponta.de(substituidoPor));
    }

    // A cópia em si, comum a clonar e substituir: documento novo em RASCUNHO, de quem copia (autor e OM), com a mesma
    // habilitação de criação exigida para a espécie.
    private Documento copiarParaNovoDocumento(Long id) {

        Documento documentoAntigo = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        // Clonar cria um documento novo: vale a mesma habilitação da OM de quem clona, para a espécie do original.
        habilitacaoDeCriacaoService.exigirPodeCriar(omDoUsuarioAtual(), documentoAntigo.getEspecieNormativa());

        // O clone é um documento novo (ver clonarNormItem): quem clona vira o
        // autor, não quem criou o original -- mesma regra de "criar" no resto
        // do sistema.
        Documento documentoNovo = regras.para(documentoAntigo.getEspecieNormativa()).criacao()
                .montarCopiaDe(documentoAntigo, AutenticacaoUtil.usuarioAtual());

        documentoAntigo.setQtdReplicas(documentoAntigo.getQtdReplicas() + 1);
        documentoRepository.save(documentoAntigo);

        Documento clonado = documentoRepository.save(documentoNovo);
        regras.para(documentoAntigo.getEspecieNormativa()).camposEspecificos().copiar(documentoAntigo, clonado);

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
            copia.setOrientacao(orig.getOrientacao());
            anexoRepository.save(copia);
        }

        return clonado;
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

}
