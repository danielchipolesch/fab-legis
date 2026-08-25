package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.assuntoBasicoDtos.AssuntoBasicoRequestCreateDto;
import br.com.danielchipolesch.application.dtos.assuntoBasicoDtos.AssuntoBasicoRequestUpdateDto;
import br.com.danielchipolesch.application.dtos.assuntoBasicoDtos.AssuntoBasicoResponseDto;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceAlreadyExistsException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.BasicSubjectException;
import br.com.danielchipolesch.infrastructure.repositories.AssuntoBasicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssuntoBasicoService {

    @Autowired
    private AssuntoBasicoRepository assuntoBasicoRepository;

    public AssuntoBasicoResponseDto create(AssuntoBasicoRequestCreateDto request) throws Exception {
        if(assuntoBasicoRepository.existsByCodigo(request.codigo())){
            throw new ResourceAlreadyExistsException(BasicSubjectException.ALREADY_EXISTS.getMessage());
        }

        AssuntoBasico assuntoBasico = new AssuntoBasico();
        assuntoBasico.setCodigo(request.codigo());
        assuntoBasico.setNome(request.nome());
        assuntoBasico.setDescricao(request.descricao());
        assuntoBasicoRepository.save(assuntoBasico);
        return toDto(assuntoBasico);
    }


    public AssuntoBasicoResponseDto update(Long id, AssuntoBasicoRequestUpdateDto request) throws Exception{

        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(BasicSubjectException.NOT_FOUND.getMessage()));

        assuntoBasico.setCodigo(request.codigo().isBlank() ? assuntoBasico.getCodigo() : request.codigo());
        assuntoBasico.setNome(request.nome().isBlank() ? assuntoBasico.getNome() : request.nome());
        assuntoBasico.setDescricao(request.descricao().isBlank() ? assuntoBasico.getDescricao() : request.descricao());

        assuntoBasicoRepository.save(assuntoBasico);

        return toDto(assuntoBasico);
    }

    public AssuntoBasicoResponseDto delete(Long id) throws Exception {
        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BasicSubjectException.NOT_FOUND.getMessage()));

        assuntoBasicoRepository.delete(assuntoBasico);

        return toDto(assuntoBasico);
    }

    public AssuntoBasicoResponseDto getById(Long id) throws Exception {
        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BasicSubjectException.NOT_FOUND.getMessage()));
        return toDto(assuntoBasico);
    }

    public AssuntoBasicoResponseDto getByNumber(String number) throws Exception {
        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findByCodigo(number);
        return toDto(assuntoBasico);
    }

    public List<AssuntoBasicoResponseDto> getAll(Pageable pageable) throws Exception {
        Page<AssuntoBasico> basicSubjects = assuntoBasicoRepository.findAll(pageable);
        return basicSubjects.stream().map(this::toDto).toList();
    }

    private AssuntoBasicoResponseDto toDto(AssuntoBasico a) {
        return new AssuntoBasicoResponseDto(a.getId(), a.getCodigo(), a.getNome(), a.getDescricao());
    }
}
