package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.handlers.exceptions.ResourceAlreadyExistsException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentationTypeException;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.DocumentationTypeRequestCreateDto;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.DocumentationTypeResponseDto;
import br.com.danielchipolesch.application.dtos.especieNormativaDtos.DocumentationTypeRequestUpdateDto;
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

    public DocumentationTypeResponseDto create(DocumentationTypeRequestCreateDto request) throws Exception {

        if(especieNormativaRepository.existsBySigla(request.getAcronym())){
            throw new ResourceAlreadyExistsException(DocumentationTypeException.ALREADY_EXISTS.getMessage());
        }

        EspecieNormativa especieNormativa = new EspecieNormativa();
        especieNormativa.setSigla(request.getAcronym());
        especieNormativa.setNome(request.getName());
        especieNormativa.setDescricao(request.getDescription());
        especieNormativaRepository.save(especieNormativa);
        return toDto(especieNormativa);
    }


    public DocumentationTypeResponseDto update(Long id, DocumentationTypeRequestUpdateDto request) throws Exception {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentationTypeException.NOT_FOUND.getMessage()));

        especieNormativa.setSigla(request.getAcronym().isBlank() ? especieNormativa.getSigla() : request.getAcronym());
        especieNormativa.setNome(request.getName().isBlank() ? especieNormativa.getNome() : request.getName());
        especieNormativa.setDescricao(request.getDescription().isBlank() ? especieNormativa.getDescricao() : request.getDescription());

        especieNormativaRepository.save(especieNormativa);

        return toDto(especieNormativa);
    }

    public DocumentationTypeResponseDto delete(Long id) throws Exception {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentationTypeException.NOT_FOUND.getMessage()));

        especieNormativaRepository.delete(especieNormativa);

        return toDto(especieNormativa);
    }

    public DocumentationTypeResponseDto getById(Long id) throws Exception {
        EspecieNormativa especieNormativa = especieNormativaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentationTypeException.NOT_FOUND.getMessage()));
        return toDto(especieNormativa);
    }

    public List<DocumentationTypeResponseDto> getAll(Pageable pageable) throws Exception {
        Page<EspecieNormativa> documentationTypes = especieNormativaRepository.findAll(pageable);
        return documentationTypes.stream().map(this::toDto).toList();
    }

    private DocumentationTypeResponseDto toDto(EspecieNormativa e) {
        return new DocumentationTypeResponseDto(e.getId(), e.getSigla(), e.getNome(), e.getDescricao());
    }
}
