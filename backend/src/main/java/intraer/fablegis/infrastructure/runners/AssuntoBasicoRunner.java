package intraer.fablegis.infrastructure.runners;

import intraer.fablegis.domain.entities.numeracaoDocumento.AssuntoBasico;
import intraer.fablegis.infrastructure.enums.AssuntoBasicoEnum;
import intraer.fablegis.infrastructure.repositories.AssuntoBasicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AssuntoBasicoRunner implements CommandLineRunner {

    @Autowired
    AssuntoBasicoRepository assuntoBasicoRepository;

    @Override
    public void run(String... args) throws Exception {
        AssuntoBasicoEnum[] assuntoBasicoEnums = AssuntoBasicoEnum.values();
        for (AssuntoBasicoEnum assuntoBasicoEnum : assuntoBasicoEnums){
            if (!assuntoBasicoRepository.existsByCodigo(assuntoBasicoEnum.getCode())){
                AssuntoBasico assuntoBasico = new AssuntoBasico();
                assuntoBasico.setCodigo(assuntoBasicoEnum.getCode());
                assuntoBasico.setNome(assuntoBasicoEnum.getName());
                assuntoBasico.setDescricao(assuntoBasicoEnum.getDescription());
                assuntoBasicoRepository.save(assuntoBasico);
            }
        }
    }
}
