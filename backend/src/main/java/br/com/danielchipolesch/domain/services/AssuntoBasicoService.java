package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.assuntoBasicoDtos.AssuntoBasicoRequestCreateDto;
import br.com.danielchipolesch.application.dtos.assuntoBasicoDtos.AssuntoBasicoRequestUpdateDto;
import br.com.danielchipolesch.application.dtos.assuntoBasicoDtos.AssuntoBasicoResponseDto;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceAlreadyExistsException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.BasicSubjectException;
import br.com.danielchipolesch.infrastructure.repositories.AssuntoBasicoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssuntoBasicoService {

    @Autowired
    private AssuntoBasicoRepository assuntoBasicoRepository;

    @Autowired
    private ModelMapper modelMapper;

    public AssuntoBasicoResponseDto create(AssuntoBasicoRequestCreateDto request) throws Exception {
        if(assuntoBasicoRepository.existsByCodigo(request.getCodigo())){
            throw new ResourceAlreadyExistsException(BasicSubjectException.ALREADY_EXISTS.getMessage());
        }

        AssuntoBasico assuntoBasico = modelMapper.map(request, AssuntoBasico.class);
        assuntoBasicoRepository.save(assuntoBasico);
        return modelMapper.map(assuntoBasico, AssuntoBasicoResponseDto.class);
    }


    public AssuntoBasicoResponseDto update(Long id, AssuntoBasicoRequestUpdateDto request) throws Exception{

        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(BasicSubjectException.NOT_FOUND.getMessage()));

        assuntoBasico.setCodigo(request.getCodigo().isBlank() ? assuntoBasico.getCodigo() : request.getCodigo());
        assuntoBasico.setNome(request.getNome().isBlank() ? assuntoBasico.getNome() : request.getNome());
        assuntoBasico.setDescricao(request.getDescricao().isBlank() ? assuntoBasico.getDescricao() : request.getDescricao());

        assuntoBasicoRepository.save(assuntoBasico);

        return modelMapper.map(assuntoBasico, AssuntoBasicoResponseDto.class);
    }

    public AssuntoBasicoResponseDto delete(Long id) throws Exception {
        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BasicSubjectException.NOT_FOUND.getMessage()));

        assuntoBasicoRepository.delete(assuntoBasico);

        return modelMapper.map(assuntoBasico, AssuntoBasicoResponseDto.class);
    }

    public AssuntoBasicoResponseDto getById(Long id) throws Exception {
        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BasicSubjectException.NOT_FOUND.getMessage()));

        return modelMapper.map(assuntoBasico, AssuntoBasicoResponseDto.class);
    }

    public AssuntoBasicoResponseDto getByNumber(String number) throws Exception {
        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findByCodigo(number);
//                .orElseThrow(() -> new Exception(BasicSubjectException.NOT_FOUND.getMessage()));

        return modelMapper.map(assuntoBasico, AssuntoBasicoResponseDto.class);
    }

    public List<AssuntoBasicoResponseDto> getAll(Pageable pageable) throws Exception {
        Page<AssuntoBasico> basicSubjects = assuntoBasicoRepository.findAll(pageable);

        List<AssuntoBasicoResponseDto> responseDtos = basicSubjects.stream().map(basicSubject -> modelMapper.map(basicSubject, AssuntoBasicoResponseDto.class)).toList();

        return responseDtos;
    }
}
