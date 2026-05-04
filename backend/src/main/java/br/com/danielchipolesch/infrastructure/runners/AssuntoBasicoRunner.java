package br.com.danielchipolesch.infrastructure.runners;

import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.infrastructure.enums.AssuntoBasicoEnum;
import br.com.danielchipolesch.infrastructure.repositories.AssuntoBasicoRepository;
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
