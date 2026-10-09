package intraer.fablegis.infrastructure.runners;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.domain.entities.numeracaoDocumento.EspecieNormativa;
import intraer.fablegis.infrastructure.enums.EspecieNormativaEnum;
import intraer.fablegis.infrastructure.repositories.EspecieNormativaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EspecieNormativaRunner implements CommandLineRunner {

    final EspecieNormativaRepository especieNormativaRepository;

    @Override
    public void run(String... args) throws Exception {
        EspecieNormativaEnum[] especieNormativaEnum = EspecieNormativaEnum.values();
        for (EspecieNormativaEnum especieNormativaEnumAcronym : especieNormativaEnum){
            if (!especieNormativaRepository.existsBySigla(especieNormativaEnumAcronym.name())){
                EspecieNormativa especieNormativa = new EspecieNormativa();
                especieNormativa.setSigla(especieNormativaEnumAcronym.name());
                especieNormativa.setNome(especieNormativaEnumAcronym.getName());
                especieNormativa.setDescricao(especieNormativaEnumAcronym.getDescription());
                especieNormativa.setTipoDeEspecie(especieNormativaEnumAcronym.getTipoDeEspecie());
                especieNormativaRepository.save(especieNormativa);
            }
        }
    }
}
