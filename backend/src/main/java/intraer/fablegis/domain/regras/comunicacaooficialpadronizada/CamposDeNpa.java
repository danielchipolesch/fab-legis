package intraer.fablegis.domain.regras.comunicacaooficialpadronizada;

import intraer.fablegis.application.dtos.npaDtos.AssinaturaDaNpaDto;
import intraer.fablegis.application.dtos.npaDtos.CamposDaNpaDto;
import intraer.fablegis.domain.entities.estruturaDocumento.CamposDaNpa;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.handlers.exceptions.ResourceCannotBeUpdatedException;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.regras.CamposEspecificosDaEspecie;
import intraer.fablegis.domain.regras.TipoDeEspecie;
import intraer.fablegis.infrastructure.repositories.CamposDaNpaRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Campos do cabeçalho e do fecho que só a NPA tem: setor emissor, local do fecho e blocos de assinatura em texto
// livre (rótulo + linhas). Nascem com valores de orientação entre colchetes, para o autor preencher.
//
// Dois blocos de assinatura não são escritos: "Elaborado por" (o autor e todos os coautores) e "Aprovado por" (quem
// aprova o documento) saem do próprio documento, para nunca ficarem desatualizados -- ver CabecalhoDaNpa. Por isso
// os blocos gravados não podem levar esses rótulos.
//
// Depois de publicada (ou revogada) a NPA não muda mais -- não há alteração --, então os campos também não.
@Component
public class CamposDeNpa implements CamposEspecificosDaEspecie {

    static final String SETOR_INICIAL = "[SETOR EMISSOR]";
    static final String LOCAL_INICIAL = "[Local]";

    private final CamposDaNpaRepository repositorio;
    private final DocumentoRepository documentoRepository;
    private final DocumentoCompartilhamentoRepository compartilhamentoRepository;
    private final ObjectMapper objectMapper;

    public CamposDeNpa(CamposDaNpaRepository repositorio, DocumentoRepository documentoRepository,
                       DocumentoCompartilhamentoRepository compartilhamentoRepository, ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.documentoRepository = documentoRepository;
        this.compartilhamentoRepository = compartilhamentoRepository;
        this.objectMapper = objectMapper;
    }

    // Nasce sem blocos escritos: "Elaborado por" e "Aprovado por" vêm do documento.
    static List<AssinaturaDaNpaDto> assinaturasIniciais() {
        return List.of();
    }

    @Override
    public void criarPara(Documento documento) {
        gravar(documento.getId(), SETOR_INICIAL, LOCAL_INICIAL, assinaturasIniciais());
    }

    @Override
    public void copiar(Documento original, Documento copia) {
        repositorio.findById(original.getId()).ifPresentOrElse(
                campos -> gravar(copia.getId(), campos.getSetorEmissor(), campos.getLocal(), ler(campos)),
                () -> criarPara(copia));
    }

    @Transactional(readOnly = true)
    public CamposDaNpaDto obter(Long documentoId) {
        exigirNpa(documentoId);
        return camposParaLeiaute(documentoId);
    }

    @Transactional
    public CamposDaNpaDto atualizar(Long documentoId, CamposDaNpaDto novos) {
        Documento documento = exigirNpa(documentoId);
        if (documento.getSituacaoBca() != SituacaoBcaEnum.NAO_PUBLICADO) {
            throw new ResourceCannotBeUpdatedException(
                    "A NPA já foi publicada e não pode ser alterada. Para mudá-la, crie outra e revogue esta.");
        }
        var assinaturas = novos.assinaturas() == null ? List.<AssinaturaDaNpaDto>of() : novos.assinaturas();
        if (assinaturas.stream().anyMatch(a -> CabecalhoDaNpa.rotuloReservado(a.rotulo()))) {
            throw new InvalidInputException("Os blocos \"Elaborado por\" e \"Aprovado por\" são preenchidos automaticamente "
                    + "(autor e coautores; quem aprova) e não podem ser escritos aqui.");
        }
        if (novos.setorEmissor() == null || novos.setorEmissor().isBlank()
                || novos.local() == null || novos.local().isBlank()) {
            throw new InvalidInputException("Informe o setor emissor e o local.");
        }
        gravar(documentoId, novos.setorEmissor().strip(), novos.local().strip(), assinaturas);
        return obter(documentoId);
    }

    private Documento exigirNpa(Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado."));
        if (documento.getEspecieNormativa().getTipoDeEspecie() != TipoDeEspecie.COMUNICACAO_OFICIAL_PADRONIZADA) {
            throw new ResourceNotFoundException("Este documento não tem os campos de uma NPA.");
        }
        return documento;
    }

    // Chamado ao revogar a NPA: guarda o Boletim Interno da revogação sem tocar nos demais campos.
    public void registrarRevogacao(Long documentoId, String boletim) {
        var campos = repositorio.findById(documentoId).orElseGet(() -> {
            var novos = new CamposDaNpa();
            novos.setDocumentoId(documentoId);
            novos.setSetorEmissor(SETOR_INICIAL);
            novos.setLocal(LOCAL_INICIAL);
            novos.setAssinaturas(objectMapper.writeValueAsString(assinaturasIniciais()));
            return novos;
        });
        campos.setBoletimDaRevogacao(boletim);
        repositorio.save(campos);
    }

    private void gravar(Long documentoId, String setor, String local, List<AssinaturaDaNpaDto> assinaturas) {
        var campos = repositorio.findById(documentoId).orElseGet(CamposDaNpa::new);
        campos.setDocumentoId(documentoId);
        campos.setSetorEmissor(setor);
        campos.setLocal(local);
        campos.setAssinaturas(objectMapper.writeValueAsString(assinaturas));
        repositorio.save(campos);
    }

    private List<AssinaturaDaNpaDto> ler(CamposDaNpa campos) {
        return objectMapper.readValue(campos.getAssinaturas(), new TypeReference<List<AssinaturaDaNpaDto>>() {});
    }

    // Os campos de uma NPA: os gravados ou, se por algum motivo não existirem, os iniciais. É também o que o layout
    // da NPA (PDF/HTML) lê, sem exigir que o chamador já tenha carregado o documento.
    public CamposDaNpaDto camposParaLeiaute(Long documentoId) {
        var documento = documentoRepository.findById(documentoId).orElse(null);
        var elaboradoPor = elaboradoPor(documento);
        var aprovadoPor = aprovadoPor(documento);
        return repositorio.findById(documentoId)
                .map(c -> new CamposDaNpaDto(c.getSetorEmissor(), c.getLocal(), ler(c), c.getBoletimDaRevogacao(),
                        elaboradoPor, aprovadoPor))
                .orElseGet(() -> new CamposDaNpaDto(SETOR_INICIAL, LOCAL_INICIAL, assinaturasIniciais(), null,
                        elaboradoPor, aprovadoPor));
    }

    // O autor e, depois dele, cada coautor (na ordem em que foram incluídos), um por linha.
    private List<String> elaboradoPor(Documento documento) {
        if (documento == null || documento.getAutor() == null) return List.of();
        var nomes = new ArrayList<String>();
        nomes.add(pessoa(documento.getAutor()));
        if (documento.getId() != null) {
            compartilhamentoRepository.findByDocumentoId(documento.getId()).stream()
                    .sorted(java.util.Comparator.comparing(c -> c.getId() == null ? 0L : c.getId()))
                    .map(c -> pessoa(c.getUsuario()))
                    .filter(n -> !nomes.contains(n))
                    .forEach(nomes::add);
        }
        return nomes;
    }

    // Quem aprova é a pessoa (papel APROV) escolhida ao enviar o documento para revisão -- mas o nome só aparece quando
    // ela de fato aprova (é a mesma data de aprovação da EMISSÃO); antes disso o bloco fica com a máscara de
    // CabecalhoDaNpa, para a NPA em elaboração não parecer já aprovada por alguém.
    private List<String> aprovadoPor(Documento documento) {
        if (documento == null || documento.getRevisorAtribuido() == null || documento.getDtAprovacao() == null) {
            return List.of();
        }
        return List.of(pessoa(documento.getRevisorAtribuido()));
    }

    // "Cel FULANO DE TAL SILVA": posto ou graduação (bigrama) e o nome completo em caixa alta; sem posto (servidor civil), só o nome.
    static String pessoa(Usuario usuario) {
        String nome = usuario.getNome() == null ? "" : usuario.getNome().strip().toUpperCase(Locale.ROOT);
        return usuario.getPostoGraduacao() != null ? usuario.getPostoGraduacao().getBigrama() + " " + nome : nome;
    }
}
