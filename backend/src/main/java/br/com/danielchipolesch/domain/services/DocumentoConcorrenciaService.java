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
// conflito de edição -- distinto de DocumentoPresencaService, que só avisa
// "fulano também está editando" sem impedir nada.
//
// Escopo reduzido desde a edição colaborativa em tempo real (Yjs/Hocuspocus,
// ver plano de colaboração): conteúdo de elemento não colide mais aqui (o
// CRDT resolve isso ao vivo, sem 409 -- ver
// DocumentoParteNormativaService.atualizarConteudoElemento, que nunca chama
// este serviço). Esta checagem hoje só protege ESTRUTURA da árvore
// (PATCH /{id}/secoes) e metadados do documento (PUT /{id}) -- a chamada de
// checarEAtualizarVersao é o que ainda bumpa nr_versao a cada uma dessas.
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
