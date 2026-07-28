package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestUpdateDto;
import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoResponseSemAnexoTextualDto;
import br.com.danielchipolesch.domain.builders.DocumentoBuilder;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.TipoAlteracaoEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.BasicSubjectException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentException;
import br.com.danielchipolesch.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.DocumentationTypeException;
import br.com.danielchipolesch.domain.mappers.DocumentoMapper;
import br.com.danielchipolesch.infrastructure.repositories.AssuntoBasicoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.EspecieNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemAnexoParteNormativaRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemParteFinalRepository;
import br.com.danielchipolesch.infrastructure.repositories.ItemPartePreliminarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class DocumentoService {

    @Autowired
    DocumentoRepository documentoRepository;

    @Autowired
    EspecieNormativaRepository especieNormativaRepository;

    @Autowired
    AssuntoBasicoRepository assuntoBasicoRepository;

    @Autowired
    ItemAnexoParteNormativaRepository itemAnexoParteNormativaRepository;

    @Autowired
    ItemPartePreliminarRepository itemPartePreliminarRepository;

    @Autowired
    ItemParteFinalRepository itemParteFinalRepository;

    @Autowired
    DocumentoHistoricoService documentoHistoricoService;


    @Transactional
    public DocumentoResponseSemAnexoTextualDto create(DocumentoRequestCreateDto request) throws RuntimeException {

        EspecieNormativa especieNormativa = especieNormativaRepository.findById(request.getIdEspecieNormativa()).orElseThrow(() -> new ResourceNotFoundException(DocumentationTypeException.NOT_FOUND.getMessage()));
        AssuntoBasico assuntoBasico = assuntoBasicoRepository.findById(request.getIdAssuntoBasico()).orElseThrow(() ->  new ResourceNotFoundException(BasicSubjectException.NOT_FOUND.getMessage()));

        var secondaryNumber = this.calculateSecondaryNumber(especieNormativa, assuntoBasico);

        Documento documento = new DocumentoBuilder()
                .especieNormativa(especieNormativa)
                .assuntoBasico(assuntoBasico)
                .numeroSecundario(secondaryNumber)
                .tituloDocumento(request.getTituloDocumento())
                .documentoStatus(DocumentoStatusEnum.RASCUNHO)
                .build();

        Documento salvo = documentoRepository.save(documento);
        documentoHistoricoService.registrar(salvo, TipoAlteracaoEnum.CRIACAO,
                "Documento criado", null, DocumentoStatusEnum.RASCUNHO);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(salvo);
    }

    public Documento getById(Long id) throws RuntimeException{

        return documentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));
    }

    public List<DocumentoResponseSemAnexoTextualDto> getByDocumentationTypeAndBasicSubject(Long documentationTypeId, Long basicSubjectId) throws ResourceNotFoundException {

        var especieNormativa = especieNormativaRepository.findById(documentationTypeId).orElseThrow(() -> new ResourceNotFoundException(DocumentationTypeException.NOT_FOUND.getMessage()));
        var assuntoBasico = assuntoBasicoRepository.findById(basicSubjectId).orElseThrow(() -> new ResourceNotFoundException(BasicSubjectException.NOT_FOUND.getMessage()));

        List<Documento> documents = documentoRepository.findByEspecieNormativaAndAssuntoBasico(especieNormativa, assuntoBasico);

        return documents.stream().map(DocumentoMapper::documentoToDocumentoSemAnexoTextualResponseDto).toList();
    }

    public List<Documento> getAll(Pageable pageable) throws RuntimeException {
        try{
            Page<Documento> documents = documentoRepository.findAll(pageable);
            return documents.stream().toList();
        } catch (Exception e) {
            throw new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage());
        }
    }

    @Transactional
    public DocumentoResponseSemAnexoTextualDto update(Long id, DocumentoRequestUpdateDto request) throws RuntimeException {

        Documento document = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));

        if (document.getDocumentoStatus() != DocumentoStatusEnum.RASCUNHO
                && document.getDocumentoStatus() != DocumentoStatusEnum.MINUTA) {
            throw new StatusCannotBeUpdatedException(DocumentException.CANNOT_BE_UPDATED.getMessage());
        }

        boolean tituloAlterado = !document.getTituloDocumento().equals(request.getTituloDocumento());
        document.setTituloDocumento(request.getTituloDocumento());
        Documento atualizado = documentoRepository.save(document);
        if (tituloAlterado) {
            documentoHistoricoService.registrar(atualizado, TipoAlteracaoEnum.ALTERACAO_METADADOS,
                    "Título atualizado", null, null);
        }
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(atualizado);
    }

    @Transactional
    public DocumentoResponseSemAnexoTextualDto delete(Long id) throws RuntimeException {
        Documento document = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));
        itemPartePreliminarRepository.deleteAllByDocumentoId(id);
        itemAnexoParteNormativaRepository.nullifyParentsForDocument(id);
        itemAnexoParteNormativaRepository.deleteAllByDocumentoId(id);
        itemParteFinalRepository.deleteAllByDocumentoId(id);
        documentoRepository.delete(document);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(document);
    }

    public DocumentoResponseSemAnexoTextualDto clone(Long id) throws RuntimeException {

        Documento documentOld = documentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DocumentException.NOT_FOUND.getMessage()));

        var secondaryNumber = this.calculateSecondaryNumber(documentOld.getEspecieNormativa(), documentOld.getAssuntoBasico());

        Documento documentNew = new DocumentoBuilder()
                .especieNormativa(documentOld.getEspecieNormativa())
                .assuntoBasico(documentOld.getAssuntoBasico())
                .numeroSecundario(secondaryNumber)
                .tituloDocumento(documentOld.getTituloDocumento())
                .documentoStatus(DocumentoStatusEnum.RASCUNHO)
                .build();

        Documento clonado = documentoRepository.save(documentNew);
        documentoHistoricoService.registrar(clonado, TipoAlteracaoEnum.CLONAGEM,
                "Clonado do documento #" + id, null, DocumentoStatusEnum.RASCUNHO);
        return DocumentoMapper.documentoToDocumentoSemAnexoTextualResponseDto(clonado);
    }

    private Integer calculateSecondaryNumber(EspecieNormativa especieNormativa, AssuntoBasico assuntoBasico){

        List<Documento> documents = documentoRepository.findByEspecieNormativaAndAssuntoBasico(especieNormativa, assuntoBasico);

        if (documents.isEmpty()) {
            return 1;
        }

        List<Integer> secondaryNumbers = documents.stream()
                .map(Documento::getNumeroSecundario)
                .sorted()
                .toList();

        for (int i = 1; i <= secondaryNumbers.size(); i++) {
            if (!secondaryNumbers.contains(i)) {
                return i;
            }
        }

        return secondaryNumbers.size() + 1;
    }
}
