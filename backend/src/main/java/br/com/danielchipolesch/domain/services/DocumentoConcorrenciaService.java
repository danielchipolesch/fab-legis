package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.handlers.exceptions.ConflitoEdicaoException;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

// Trava otimista sobre Documento.versao (nr_versao, já existente como
// @Version para o próprio JPA) reaproveitada como checagem explícita de
// conflito de edição -- ver o design doc: isso resolve a COLISÃO entre duas
// pessoas editando ao mesmo tempo (quem salva por último por cima perde a
// mudança da outra sem aviso), distinto de DocumentoPresencaService, que só
// avisa "fulano também está editando" sem impedir nada.
//
// O cliente manda a versão que tinha quando abriu o documento
// (versaoEsperada); se não bater mais com o banco, outra pessoa já salvou
// primeiro -- 409, front recarrega em vez de sobrescrever silenciosamente.
@Service
public class DocumentoConcorrenciaService {

    @Autowired
    private DocumentoRepository documentoRepository;

    public void checarEAtualizarVersao(Documento documento, Integer versaoEsperada) {
        if (versaoEsperada != null && !versaoEsperada.equals(documento.getVersao())) {
            throw new ConflitoEdicaoException(
                    "Este documento foi alterado por outro usuário desde que você o abriu. Recarregue antes de salvar.");
        }
        // Toca um campo real (não só a anotação @UpdateTimestamp) para garantir
        // que o Hibernate emita um UPDATE e incremente nr_versao, mesmo quando
        // nenhum outro campo do próprio Documento mudou nesta chamada.
        documento.setDtAlteracao(Timestamp.from(Instant.now()));
        documentoRepository.save(documento);
    }
}
