package br.com.danielchipolesch.application.helpers;

import br.com.danielchipolesch.application.controllers.DocumentoController;
import br.com.danielchipolesch.application.controllers.PortariaController;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class DocumentoHelper {

    public static EntityModel<DocumentoResponseComAnexoTextualDto> DocumentoLinkPortariaHateoas(Documento documento) {

        EntityModel<DocumentoResponseComAnexoTextualDto> resource = EntityModel.of(DocumentoMapper.documentoToDocumentoComAnexoTextualResponseDto(documento));
        Link selfLink = linkTo(methodOn(DocumentoController.class).getById(documento.getId())).withSelfRel();

        resource.add(selfLink);

        if (documento.getIdPortaria() != null) {

            Link linkToRegulatoryAct = linkTo(methodOn(PortariaController.class).getRegulatoryActById(documento.getIdPortaria())).withRel("portaria");
            Link linkToRegulatoryActPdf = linkTo(methodOn(PortariaController.class).getRegulatoryActPdfById(documento.getIdPortaria())).withRel("portaria-pdf");
            resource.add(linkToRegulatoryAct);
            resource.add(linkToRegulatoryActPdf);
        }

        return resource;
    }
}
