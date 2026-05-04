package br.com.danielchipolesch.infrastructure.runners;

import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.infrastructure.enums.EspecieNormativaEnum;
import br.com.danielchipolesch.infrastructure.repositories.EspecieNormativaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EspecieNormativaRunner implements CommandLineRunner {

    @Autowired
    EspecieNormativaRepository especieNormativaRepository;

    @Override
    public void run(String... args) throws Exception {
        EspecieNormativaEnum[] especieNormativaEnum = EspecieNormativaEnum.values();
        for (EspecieNormativaEnum especieNormativaEnumAcronym : especieNormativaEnum){
            if (!especieNormativaRepository.existsBySigla(especieNormativaEnumAcronym.name())){
                EspecieNormativa especieNormativa = new EspecieNormativa();
                especieNormativa.setSigla(especieNormativaEnumAcronym.name());
                especieNormativa.setNome(especieNormativaEnumAcronym.getName());
                especieNormativa.setDescricao(especieNormativaEnumAcronym.getDescription());
                especieNormativaRepository.save(especieNormativa);
            }
        }
    }
}
