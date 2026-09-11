package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.handlers.exceptions.ResourceAlreadyExistsException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.EspecieNormativaException;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.EspecieNormativaRequestCreateDto;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.EspecieNormativaResponseDto;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.EspecieNormativaRequestUpdateDto;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.infrastructure.repositories.EspecieNormativaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspecieNormativaService {

    @Autowired
    EspecieNormativaRepository especieNormativaRepository;

    public EspecieNormativaResponseDto create(EspecieNormativaRequestCreateDto request) throws Exception {

        if(especieNormativaRepository.existsBySigla(request.getSigla())){
            throw new ResourceAlreadyExistsException(EspecieNormativaException.ALREADY_EXISTS.getMessage());
        }

        EspecieNormativa especieNormativa = new EspecieNormativa();
        especieNormativa.setSigla(request.getSigla());
        especieNormativa.setNome(request.getNome());
        especieNormativa.setDescricao(request.getDescricao());
        especieNormativaRepository.save(especieNormativa);
        return toDto(especieNormativa);
    }


    public EspecieNormativaResponseDto update(Long id, EspecieNormativaRequestUpdateDto request) throws Exception {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(EspecieNormativaException.NOT_FOUND.getMessage()));

        especieNormativa.setSigla(request.getSigla().isBlank() ? especieNormativa.getSigla() : request.getSigla());
        especieNormativa.setNome(request.getNome().isBlank() ? especieNormativa.getNome() : request.getNome());
        especieNormativa.setDescricao(request.getDescricao().isBlank() ? especieNormativa.getDescricao() : request.getDescricao());

        especieNormativaRepository.save(especieNormativa);

        return toDto(especieNormativa);
    }

    public EspecieNormativaResponseDto delete(Long id) throws Exception {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(EspecieNormativaException.NOT_FOUND.getMessage()));

        especieNormativaRepository.delete(especieNormativa);

        return toDto(especieNormativa);
    }

    public EspecieNormativaResponseDto getById(Long id) throws Exception {
        EspecieNormativa especieNormativa = especieNormativaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EspecieNormativaException.NOT_FOUND.getMessage()));
        return toDto(especieNormativa);
    }

    public List<EspecieNormativaResponseDto> getAll(Pageable pageable) throws Exception {
        Page<EspecieNormativa> especiesNormativas = especieNormativaRepository.findAll(pageable);
        return especiesNormativas.stream().map(this::toDto).toList();
    }

    private EspecieNormativaResponseDto toDto(EspecieNormativa e) {
        return new EspecieNormativaResponseDto(e.getId(), e.getSigla(), e.getNome(), e.getDescricao());
    }
}
