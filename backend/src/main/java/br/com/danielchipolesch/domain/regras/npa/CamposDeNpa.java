package br.com.danielchipolesch.domain.regras.npa;

import br.com.danielchipolesch.application.dtos.npaDtos.AssinaturaDaNpaDto;
import br.com.danielchipolesch.application.dtos.npaDtos.CamposDaNpaDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.CamposDaNpa;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import br.com.danielchipolesch.domain.handlers.exceptions.InvalidInputException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.regras.CamposEspecificosDaEspecie;
import br.com.danielchipolesch.domain.regras.TipoDeRegras;
import br.com.danielchipolesch.infrastructure.repositories.CamposDaNpaRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

// Campos do cabeçalho e do fecho que só a NPA tem: setor emissor, local do fecho e blocos de assinatura em texto
// livre (rótulo + linhas). Nascem com valores de orientação entre colchetes, para o autor preencher.
//
// Depois de publicada (ou revogada) a NPA não muda mais -- não há alteração --, então os campos também não.
@Component
public class CamposDeNpa implements CamposEspecificosDaEspecie {

    static final String SETOR_INICIAL = "[SETOR EMISSOR]";
    static final String LOCAL_INICIAL = "[Local]";

    private final CamposDaNpaRepository repositorio;
    private final DocumentoRepository documentoRepository;
    private final ObjectMapper objectMapper;

    public CamposDeNpa(CamposDaNpaRepository repositorio, DocumentoRepository documentoRepository,
                       ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.documentoRepository = documentoRepository;
        this.objectMapper = objectMapper;
    }

    static List<AssinaturaDaNpaDto> assinaturasIniciais() {
        return List.of(
                new AssinaturaDaNpaDto("Elaborado por", List.of("[Nome completo, posto e função]")),
                new AssinaturaDaNpaDto("Aprovo", List.of("[Nome completo, posto e função da autoridade]")));
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
        if (documento.getEspecieNormativa().getTipoDeRegras() != TipoDeRegras.NPA) {
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
        return repositorio.findById(documentoId)
                .map(c -> new CamposDaNpaDto(c.getSetorEmissor(), c.getLocal(), ler(c), c.getBoletimDaRevogacao()))
                .orElseGet(() -> new CamposDaNpaDto(SETOR_INICIAL, LOCAL_INICIAL, assinaturasIniciais()));
    }
}
