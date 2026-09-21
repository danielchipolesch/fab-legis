package br.com.danielchipolesch.domain.regras.convencional;

import br.com.danielchipolesch.application.dtos.documentoDtos.DocumentoRequestCreateDto;
import br.com.danielchipolesch.domain.builders.DocumentoBuilder;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.AssuntoBasico;
import br.com.danielchipolesch.domain.entities.numeracaoDocumento.EspecieNormativa;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.domain.handlers.exceptions.InvalidInputException;
import br.com.danielchipolesch.domain.handlers.exceptions.ResourceNotFoundException;
import br.com.danielchipolesch.domain.handlers.exceptions.enums.AssuntoBasicoException;
import br.com.danielchipolesch.domain.regras.RegrasDeCriacaoDoDocumento;
import br.com.danielchipolesch.infrastructure.repositories.AssuntoBasicoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

// Um ato normativo (DCA, ICA, NSCA...) é identificado por espécie + assunto básico + sequencial ("DCA 11-3"): o
// assunto é obrigatório e o sequencial é o menor número livre daquela espécie naquele assunto (reaproveita o
// número de um documento excluído).
@Component
public class CriacaoDeEspecieConvencional implements RegrasDeCriacaoDoDocumento {

    private final AssuntoBasicoRepository assuntoBasicoRepository;
    private final DocumentoRepository documentoRepository;

    public CriacaoDeEspecieConvencional(AssuntoBasicoRepository assuntoBasicoRepository, DocumentoRepository documentoRepository) {
        this.assuntoBasicoRepository = assuntoBasicoRepository;
        this.documentoRepository = documentoRepository;
    }

    @Override
    public Documento montar(DocumentoRequestCreateDto pedido, EspecieNormativa especie, Usuario autor) {
        if (pedido.idAssuntoBasico() == null) {
            throw new InvalidInputException("Informe o assunto básico do documento.");
        }
        AssuntoBasico assunto = assuntoBasicoRepository.findById(pedido.idAssuntoBasico())
                .orElseThrow(() -> new ResourceNotFoundException(AssuntoBasicoException.NOT_FOUND.getMessage()));
        return novo(especie, assunto, pedido.tituloDocumento(), autor);
    }

    @Override
    public Documento montarCopiaDe(Documento original, Usuario autor) {
        return novo(original.getEspecieNormativa(), original.getAssuntoBasico(), original.getTituloDocumento(), autor);
    }

    private Documento novo(EspecieNormativa especie, AssuntoBasico assunto, String titulo, Usuario autor) {
        int sequencial = proximoSequencial(especie, assunto);
        return new DocumentoBuilder()
                .especieNormativa(especie)
                .assuntoBasico(assunto)
                .numeroSecundario(sequencial)
                .identificacao(identificacao(especie, assunto, sequencial))
                .tituloDocumento(titulo)
                .situacaoLocal(SituacaoLocalEnum.RASCUNHO)
                .autor(autor)
                .om(autor.getOm())
                .build();
    }

    static String identificacao(EspecieNormativa especie, AssuntoBasico assunto, int sequencial) {
        return especie.getSigla() + " " + assunto.getCodigo() + "-" + sequencial;
    }

    static int proximoSequencial(List<Integer> existentes) {
        List<Integer> ordenados = existentes.stream().sorted().toList();
        for (int i = 1; i <= ordenados.size(); i++) {
            if (!ordenados.contains(i)) return i;
        }
        return ordenados.size() + 1;
    }

    private int proximoSequencial(EspecieNormativa especie, AssuntoBasico assunto) {
        return proximoSequencial(documentoRepository.findByEspecieNormativaAndAssuntoBasico(especie, assunto).stream()
                .map(Documento::getNumeroSecundario).toList());
    }
}
