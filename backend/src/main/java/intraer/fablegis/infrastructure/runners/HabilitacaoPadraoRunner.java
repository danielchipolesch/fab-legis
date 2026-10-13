package intraer.fablegis.infrastructure.runners;

import intraer.fablegis.domain.entities.usuario.HabilitacaoDaOm;
import intraer.fablegis.domain.regras.RegrasDasEspecies;
import intraer.fablegis.infrastructure.repositories.EspecieNormativaRepository;
import intraer.fablegis.infrastructure.repositories.HabilitacaoDaOmRepository;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Habilitação padrão de documentos: o GABAER e o CENDOC são as únicas OMs que, de saída, criam TODAS as espécies convencionais;
// a gestão das demais é do administrador. Só age no primeiro boot (enquanto não existe nenhuma habilitação gravada): depois
// disso a tabela é do administrador, e uma habilitação removida por ele não volta. Roda depois da semeadura das espécies.
@Component
@Order(100)
@RequiredArgsConstructor
public class HabilitacaoPadraoRunner implements CommandLineRunner {

    static final List<String> OMS_COM_TODAS_AS_ESPECIES = List.of("GABAER", "CENDOC");

    private static final Logger log = LoggerFactory.getLogger(HabilitacaoPadraoRunner.class);

    private final HabilitacaoDaOmRepository habilitacaoRepository;
    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;
    private final EspecieNormativaRepository especieRepository;
    private final RegrasDasEspecies regras;

    @Override
    @Transactional
    public void run(String... args) {
        if (habilitacaoRepository.count() > 0) return;
        var especiesConvencionais = especieRepository.findAll().stream()
                .filter(e -> regras.para(e).habilitacao().exigeHabilitacaoDaOm()).toList();
        for (String sigla : OMS_COM_TODAS_AS_ESPECIES) {
            organizacaoMilitarRepository.findBySigla(sigla).ifPresentOrElse(
                    om -> especiesConvencionais.forEach(e -> habilitacaoRepository.save(new HabilitacaoDaOm(om.getId(), e.getId()))),
                    () -> log.warn("Habilitação padrão: OM {} não encontrada.", sigla));
        }
        log.info("Habilitação padrão aplicada: {} com as {} espécies convencionais.", OMS_COM_TODAS_AS_ESPECIES, especiesConvencionais.size());
    }
}
