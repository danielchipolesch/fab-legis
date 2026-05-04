package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.handlers.exceptions.ResourceAlreadyExistsException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentationTypeException;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.DocumentationTypeRequestCreateDto;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.DocumentationTypeResponseDto;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.DocumentationTypeRequestUpdateDto;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.infrastructure.repositories.EspecieNormativaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspecieNormativaService {

    @Autowired
    EspecieNormativaRepository especieNormativaRepository;

    @Autowired
    ModelMapper modelMapper;

    public DocumentationTypeResponseDto create(DocumentationTypeRequestCreateDto request) throws Exception {

        if(especieNormativaRepository.existsBySigla(request.getAcronym())){
            throw new ResourceAlreadyExistsException(DocumentationTypeException.ALREADY_EXISTS.getMessage());
        }

        EspecieNormativa especieNormativa = modelMapper.map(request, EspecieNormativa.class);
        especieNormativaRepository.save(especieNormativa);
        return modelMapper.map(especieNormativa, DocumentationTypeResponseDto.class);
    }


    public DocumentationTypeResponseDto update(Long id, DocumentationTypeRequestUpdateDto request) throws Exception {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentationTypeException.NOT_FOUND.getMessage()));

        especieNormativa.setSigla(request.getAcronym().isBlank() ? especieNormativa.getSigla() : request.getAcronym());
        especieNormativa.setNome(request.getName().isBlank() ? especieNormativa.getNome() : request.getName());
        especieNormativa.setDescricao(request.getDescription().isBlank() ? especieNormativa.getDescricao() : request.getDescription());

        especieNormativaRepository.save(especieNormativa);

        return modelMapper.map(especieNormativa, DocumentationTypeResponseDto.class);
    }

    public DocumentationTypeResponseDto delete(Long id) throws Exception {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentationTypeException.NOT_FOUND.getMessage()));

        especieNormativaRepository.delete(especieNormativa);

        return modelMapper.map(especieNormativa, DocumentationTypeResponseDto.class);
    }

    public DocumentationTypeResponseDto getById(Long id) throws Exception {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentationTypeException.NOT_FOUND.getMessage()));

        return modelMapper.map(especieNormativa, DocumentationTypeResponseDto.class);
    }

    public List<DocumentationTypeResponseDto> getAll(Pageable pageable) throws Exception {
        Page<EspecieNormativa> documentationTypes = especieNormativaRepository.findAll(pageable);

        return documentationTypes.stream().map(documentationType -> modelMapper.map(documentationType, DocumentationTypeResponseDto.class)).toList();
    }
}
