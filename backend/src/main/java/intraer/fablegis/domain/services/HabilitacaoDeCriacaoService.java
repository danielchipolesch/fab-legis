package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.especieNormativaDtos.EspecieNormativaResponseDto;
import intraer.fablegis.application.dtos.habilitacaoDtos.HabilitacaoDaOmResponseDto;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.domain.entities.usuario.HabilitacaoDaOm;
import intraer.fablegis.domain.entities.usuario.OrganizacaoMilitar;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.handlers.exceptions.OperacaoNaoAutorizadaException;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.regras.RegrasDasEspecies;
import intraer.fablegis.infrastructure.repositories.EspecieNormativaRepository;
import intraer.fablegis.infrastructure.repositories.HabilitacaoDaOmRepository;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Quais espécies normativas cada OM pode CRIAR (docs/autenticacao.md, "Habilitação de documentos"). A regra de QUAIS espécies
// dependem de habilitação é da espécie (RegraDeHabilitacaoDaOm): as convencionais dependem, a NPA não. Só a criação (e a
// clonagem, que cria um documento novo) é barrada; consultar e ser coautor não dependem da habilitação.
@Service
@RequiredArgsConstructor
public class HabilitacaoDeCriacaoService {

    private final HabilitacaoDaOmRepository habilitacaoRepository;
    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;
    private final EspecieNormativaRepository especieRepository;
    private final RegrasDasEspecies regras;

    public boolean podeCriar(Long omId, EspecieNormativa especie) {
        if (!exigeHabilitacao(especie)) return true;
        return omId != null && habilitacaoRepository.existsByOmIdAndEspecieId(omId, especie.getId());
    }

    // Lança OperacaoNaoAutorizadaException (403) se a OM não pode criar documentos da espécie.
    public void exigirPodeCriar(Long omId, EspecieNormativa especie) {
        if (!podeCriar(omId, especie)) {
            throw new OperacaoNaoAutorizadaException("A sua organização militar não está habilitada a criar documentos da espécie "
                    + especie.getSigla() + ". Peça a habilitação ao administrador do sistema (CENDOC).");
        }
    }

    // As espécies que uma OM pode criar: todas as que não dependem de habilitação, mais as convencionais habilitadas para ela.
    @Transactional(readOnly = true)
    public List<EspecieNormativaResponseDto> especiesQuePodeCriar(Long omId) {
        Set<Long> habilitadas = omId == null ? Set.of() : habilitadasDa(omId);
        return especieRepository.findAll().stream()
                .filter(e -> !exigeHabilitacao(e) || habilitadas.contains(e.getId()))
                .sorted(Comparator.comparing(EspecieNormativa::getSigla))
                .map(e -> new EspecieNormativaResponseDto(e.getId(), e.getSigla(), e.getNome(), e.getDescricao(), e.getTipoDeEspecie().name()))
                .toList();
    }

    // Para a tela de administração: cada OM com as espécies convencionais que pode criar.
    @Transactional(readOnly = true)
    public List<HabilitacaoDaOmResponseDto> listar() {
        Map<Long, List<Long>> porOm = habilitacaoRepository.findAll().stream().collect(Collectors.groupingBy(
                HabilitacaoDaOm::getOrganizacaoMilitarId, Collectors.mapping(HabilitacaoDaOm::getEspecieNormativaId, Collectors.toList())));
        return organizacaoMilitarRepository.findAll().stream()
                .sorted(Comparator.comparing(OrganizacaoMilitar::getSigla))
                .map(om -> new HabilitacaoDaOmResponseDto(om.getId(), om.getSigla(), om.getNome(),
                        porOm.getOrDefault(om.getId(), List.of()).stream().sorted().toList()))
                .toList();
    }

    // Substitui as habilitações da OM pela lista pedida (vazia = a OM deixa de poder criar espécies convencionais).
    @Transactional
    public HabilitacaoDaOmResponseDto definir(Long omId, List<Long> especieIds) {
        OrganizacaoMilitar om = organizacaoMilitarRepository.findById(omId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização militar não encontrada."));
        Set<Long> pedidas = new HashSet<>(especieIds);
        for (Long id : pedidas) {
            EspecieNormativa especie = especieRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Espécie normativa não encontrada (id " + id + ")."));
            if (!exigeHabilitacao(especie)) {
                throw new InvalidInputException("A espécie " + especie.getSigla() + " não depende de habilitação: toda OM a cria.");
            }
        }
        habilitacaoRepository.deleteByOmId(omId);
        habilitacaoRepository.flush();
        pedidas.forEach(id -> habilitacaoRepository.save(new HabilitacaoDaOm(omId, id)));
        return new HabilitacaoDaOmResponseDto(om.getId(), om.getSigla(), om.getNome(), pedidas.stream().sorted().toList());
    }

    private Set<Long> habilitadasDa(Long omId) {
        return habilitacaoRepository.findByOmId(omId).stream().map(HabilitacaoDaOm::getEspecieNormativaId).collect(Collectors.toSet());
    }

    private boolean exigeHabilitacao(EspecieNormativa especie) {
        return regras.para(especie).habilitacao().exigeHabilitacaoDaOm();
    }
}
