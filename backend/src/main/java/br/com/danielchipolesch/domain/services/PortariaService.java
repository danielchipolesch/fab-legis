package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaDto;
import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaRequestDto;
import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaResponseDto;
import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaResponseSemPdfDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Portaria;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.RegulatoryActException;
import br.com.danielchipolesch.domain.mappers.PortariaMapper;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.PortariaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@Service
public class PortariaService {

    @Autowired
    PortariaRepository portariaRepository;

    @Autowired
    DocumentoRepository documentoRepository;

    @Transactional
    public PortariaResponseSemPdfDto insertRegulatoryActInDocument(Long id, MultipartFile file) throws RuntimeException, IOException {
        Documento document = documentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));
        if (!document.getDocumentoStatus().equals(DocumentoStatusEnum.APROVADO.toString())){
            throw new StatusCannotBeUpdatedException(DocumentException.DOCUMENT_ACT_APROVADO.getMessage());
        }

        Portaria portaria = new Portaria();
//        regulatoryAct.setDocument(document);
        portaria.setFileName(file.isEmpty() ? null : file.getOriginalFilename());
        portaria.setData(file.isEmpty() ? null : file.getBytes());
        var regulatoryActCreated = portariaRepository.save(portaria);
        document.setIdPortaria(regulatoryActCreated.getId());
        documentoRepository.save(document);
        return PortariaMapper.regulatoryActToRegulatoryActResponseNoPdfDto(portaria);
    }

    public List<PortariaDto> getAll(Pageable pageable) throws RuntimeException {
        try{
            Page<Portaria> regulatoryActs = portariaRepository.findAll(pageable);
            return regulatoryActs.stream().map(PortariaMapper::regulatoryActToRegulatoryActDto).toList();
        } catch (Exception e) {
            throw new ResourceNotFoundException(RegulatoryActException.NOT_FOUND.getMessage());
        }
    }

    public PortariaResponseDto getById(Long idPortaria) throws RuntimeException {

        Portaria portaria = portariaRepository.findById(idPortaria).orElseThrow(() -> new ResourceNotFoundException(RegulatoryActException.NOT_FOUND.getMessage()));
        return PortariaMapper.regulatoryActToRegulatoryActResponseDto(portaria);

    }

    public PortariaResponseSemPdfDto getRegulatoryActNoPdfById(Long idPortaria) throws RuntimeException {

        Portaria portaria = portariaRepository.findById(idPortaria).orElseThrow(() -> new ResourceNotFoundException(RegulatoryActException.NOT_FOUND.getMessage()));
        return PortariaMapper.regulatoryActToRegulatoryActResponseNoPdfDto(portaria);
    }


//    public RegulatoryActResponseDto getByDocumentId(String documentId) throws RuntimeException {
//
//        Doc document = docRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));
//        Optional<RegulatoryAct> regulatoryAct = regulatoryActRepository.findById(document.getRegulatoryActId());
//        return RegulatoryActMapper.regulatoryActToRegulatoryActResponseDto(regulatoryAct);
//    }

    //TODO Finish updateRegulatoryActInDocument method
    @Transactional
    public DocumentoResponseSemAnexoTextualDto updateRegulatoryActInDocument(Long id, PortariaRequestDto request) throws RuntimeException, IOException {
        return null;
    }
}
