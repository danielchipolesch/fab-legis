package br.com.danielchipolesch.application.helpers;

import br.com.danielchipolesch.application.controllers.DocumentoController;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseComAnexoTextualDto;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class DocumentoHelper {

    public static EntityModel<DocumentoResponseComAnexoTextualDto> buildWithLinks(
            Documento documento, DocumentoResponseComAnexoTextualDto dto) {
        EntityModel<DocumentoResponseComAnexoTextualDto> resource = EntityModel.of(dto);
        Link selfLink = linkTo(methodOn(DocumentoController.class).getById(documento.getId())).withSelfRel();
        resource.add(selfLink);
        return resource;
    }
}
