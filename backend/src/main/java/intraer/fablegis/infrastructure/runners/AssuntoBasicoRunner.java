package intraer.fablegis.infrastructure.runners;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.domain.entities.numeracaoDocumento.AssuntoBasico;
import intraer.fablegis.infrastructure.enums.AssuntoBasicoEnum;
import intraer.fablegis.infrastructure.repositories.AssuntoBasicoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssuntoBasicoRunner implements CommandLineRunner {

    final AssuntoBasicoRepository assuntoBasicoRepository;

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
