package br.com.danielchipolesch.application.controllers;

import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaDto;
import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaResponseDto;
import br.com.danielchipolesch.application.dtos.portariaDtos.PortariaResponseSemPdfDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.mappers.PortariaMapper;
import br.com.danielchipolesch.domain.services.DocumentoService;
import br.com.danielchipolesch.domain.services.PortariaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/v1/portarias")
@Tag(name = "Portaria", description = "Inserir descrição")
public class PortariaController {

    @Autowired
    private PortariaService portariaService;

    @Autowired
    private DocumentoService documentoService;


    @PostMapping(value = "documento/{id}/incluir-pdf", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<PortariaResponseSemPdfDto> insertDocumentAct(@PathVariable(value = "id") @Valid Long idDocumento,
                                                                       @RequestParam("file") @Valid MultipartFile file) throws RuntimeException, IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(portariaService.insertRegulatoryActInDocument(idDocumento, file));
    }

    @GetMapping("{idPortaria}")
    public ResponseEntity<EntityModel<PortariaResponseSemPdfDto>> getRegulatoryActById(@PathVariable(value = "idPortaria") @Valid Long idPortaria) throws RuntimeException {

        PortariaResponseSemPdfDto regulatoryActResponseDto = portariaService.getRegulatoryActNoPdfById(idPortaria);
        EntityModel<PortariaResponseSemPdfDto> resource = EntityModel.of(regulatoryActResponseDto);

        Link selfLink = linkTo(methodOn(PortariaController.class).getRegulatoryActById(idPortaria)).withSelfRel();
        resource.add(selfLink);

        resource.add(linkTo(methodOn(PortariaController.class).getRegulatoryActPdfById(regulatoryActResponseDto.getIdPortaria())).withRel("portaria-pdf"));
        resource.add(linkTo(methodOn(DocumentoController.class).getById(regulatoryActResponseDto.getIdPortaria())).withRel("documento"));

        return ResponseEntity.status(HttpStatus.OK).body(resource);
    }

    @GetMapping("{idPortaria}/pdf")
    public ResponseEntity<byte[]> getRegulatoryActPdfById(@PathVariable(value = "idPortaria") @Valid Long idPortaria) throws RuntimeException {
        PortariaResponseDto pdfFile = portariaService.getById(idPortaria);

        // Define os headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);  // Define o tipo como PDF
        headers.setContentDispositionFormData("inline", pdfFile.getNomePortaria());  // Define a exibição como inline

        // Retorna a resposta com status OK e o conteúdo do PDF em bytes
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)  // Define os headers
                .contentType(MediaType.APPLICATION_PDF)  // Define o tipo de conteúdo
                .body(pdfFile.getDadoBase64());  // Retorna o conteúdo do PDF
    }

    @GetMapping("documento/{idDocumento}/pdf")
    public ResponseEntity<byte[]> getRegulatoryActPdfByDocumentId(@PathVariable(value = "idDocumento") @Valid Long idDocumento) throws RuntimeException {

        // Busca o arquivo PDF através do service
        // RegulatoryActResponseDto pdfFile = regulatoryActService.getByDocumentId(idDocumento);

        Documento document = documentoService.getById(idDocumento);

        // Busca o arquivo PDF através do service
        PortariaResponseDto pdfFile = portariaService.getById(document.getIdPortaria());

        // Define os headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);  // Define o tipo como PDF
        headers.setContentDispositionFormData("inline", pdfFile.getNomePortaria());  // Define a exibição como inline

        // Retorna a resposta com status OK e o conteúdo do PDF em bytes
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)  // Define os headers
                .contentType(MediaType.APPLICATION_PDF)  // Define o tipo de conteúdo
                .body(pdfFile.getDadoBase64());  // Retorna o conteúdo do PDF
    }

    @GetMapping("obter-todas")
    public ResponseEntity<List<EntityModel<PortariaResponseSemPdfDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) throws RuntimeException {

        // TODO Incluir link direto para o PDF usando Spring HATEOAS

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        List<PortariaDto> regulatoryActsPageable = portariaService.getAll(pageable);

        List<EntityModel<PortariaResponseSemPdfDto>> regulatoryActs = regulatoryActsPageable.stream()
                .map(regulatoryAct -> {
                    PortariaResponseSemPdfDto regulatoryActResponseDto = PortariaMapper.regulatoryActDtoToRegulatoryActResponseNoPdfDto(regulatoryAct);
                    EntityModel<PortariaResponseSemPdfDto> resource = EntityModel.of(regulatoryActResponseDto);

                    Link selfLink = linkTo(methodOn(PortariaController.class).getRegulatoryActById(regulatoryActResponseDto.getIdPortaria())).withSelfRel();
                    resource.add(selfLink);

                    resource.add(linkTo(methodOn(PortariaController.class).getRegulatoryActPdfById(regulatoryAct.getIdPortaria())).withRel("portaria-pdf"));
//                    resource.add(linkTo(methodOn(DocumentController.class).getById(regulatoryAct.getDocumento().getIdDocumento())).withRel("documento"));

                    return resource;
                }).toList();

        return ResponseEntity.status(HttpStatus.OK).body(regulatoryActs);
    }
}