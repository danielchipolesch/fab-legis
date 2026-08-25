package br.com.danielchipolesch.domain.builders;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.OrganizacaoMilitar;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;

public class DocumentoBuilder {

    private EspecieNormativa especieNormativa;
    private AssuntoBasico assuntoBasico;
    private Integer numeroSecundario;
    private String tituloDocumento;
    private DocumentoStatusEnum documentoStatusEnum;
    private Usuario autor;
    private OrganizacaoMilitar om;

    public DocumentoBuilder especieNormativa(EspecieNormativa especieNormativa) {
        this.especieNormativa = especieNormativa;
        return this;
    }

    public DocumentoBuilder assuntoBasico(AssuntoBasico assuntoBasico) {
        this.assuntoBasico = assuntoBasico;
        return this;
    }

    public DocumentoBuilder numeroSecundario(Integer numeroSecundario) {
        this.numeroSecundario = numeroSecundario;
        return this;
    }

    public DocumentoBuilder tituloDocumento(String tituloDocumento) {
        this.tituloDocumento = tituloDocumento;
        return this;
    }

    public DocumentoBuilder documentoStatus(DocumentoStatusEnum documentoStatusEnum) {
        this.documentoStatusEnum = documentoStatusEnum;
        return this;
    }

    public DocumentoBuilder autor(Usuario autor) {
        this.autor = autor;
        return this;
    }

    public DocumentoBuilder om(OrganizacaoMilitar om) {
        this.om = om;
        return this;
    }

    public Documento build() {
        Documento documento = new Documento();
        documento.setEspecieNormativa(this.especieNormativa);
        documento.setAssuntoBasico(this.assuntoBasico);
        documento.setNumeroSecundario(this.numeroSecundario);
        documento.setTituloDocumento(this.tituloDocumento);
        documento.setDocumentoStatus(this.documentoStatusEnum);
        documento.setAutor(this.autor);
        documento.setOm(this.om);
        return documento;
    }
}
