package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.DocumentoStatusEnum;
import br.com.danielchipolesch.domain.entities.usuario.PapelEnum;
import br.com.danielchipolesch.domain.entities.usuario.Usuario;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import br.com.danielchipolesch.infrastructure.repositories.DocumentoRepository;
import br.com.danielchipolesch.infrastructure.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

// Única fonte de checagem de posse do sistema -- ver o design doc de
// autenticação/autorização (§2). Visualizar e baixar PDF NÃO passam por
// aqui: são liberados para qualquer usuário autenticado, em qualquer OM, em
// qualquer situação (basta estar logado, o que o SecurityConfig já exige).
// Só editar, compartilhar e excluir precisam de posse.
//
// ADMIN nunca aparece aqui como bypass: sob o modelo atual de papéis, Admin é
// puramente administrativo (usuários/OMs) e não tem poder nenhum sobre o
// ciclo de vida de um documento -- ver PapelEnum.
@Service
public class DocumentoAcessoService {

    private static final Set<DocumentoStatusEnum> STATUS_EXCLUIVEIS = EnumSet.of(
            DocumentoStatusEnum.RASCUNHO, DocumentoStatusEnum.MINUTA);

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoCompartilhamentoRepository compartilhamentoRepository;

    // Autor/coautor com papel EDIT -- exceto durante EM_REVISAO, onde a pessoa
    // ATRIBUÍDA como revisora (papel APROV) também pode editar (ver
    // DocumentoStatusEnum.EM_REVISAO). Fora daí (EM_PUBLICACAO em diante, ou o
    // fluxo de revogação inteiro) ninguém edita, nem o autor.
    public boolean podeEditar(Long documentoId, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;

        if (doc.getDocumentoStatus() == DocumentoStatusEnum.EM_REVISAO
                && doc.getRevisorAtribuido() != null
                && doc.getRevisorAtribuido().getId().equals(usuario.getId())) {
            return true;
        }

        if (!usuario.getPapeis().contains(PapelEnum.EDIT)) return false;
        return ehAutor(doc, usuario) || compartilhamentoRepository.existsByDocumentoIdAndUsuarioId(documentoId, usuario.getId());
    }

    // Só quem criou o documento pode adicionar ou remover coautores -- um
    // coautor não pode, por sua vez, compartilhar com mais alguém.
    public boolean podeCompartilhar(Long documentoId, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        return doc != null && ehAutor(doc, usuario);
    }

    // Autor OU coautor, mas só enquanto o documento estiver em RASCUNHO/MINUTA
    // -- um rascunho de outra pessoa, não compartilhado com o usuário, nunca
    // aparece como excluível, mesmo estando nessas situações. Fora delas,
    // ninguém com o perfil padrão exclui, nem o próprio autor.
    public boolean podeExcluir(Long documentoId, Authentication auth) {
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;
        if (!STATUS_EXCLUIVEIS.contains(doc.getDocumentoStatus())) return false;
        return podeEditar(documentoId, auth);
    }

    private static final Set<DocumentoStatusEnum> STATUS_ENTRADA_REVISOR = EnumSet.of(
            DocumentoStatusEnum.EM_REVISAO, DocumentoStatusEnum.ANALISE_REVOGACAO);
    private static final Set<DocumentoStatusEnum> STATUS_ACAO_REVISOR = EnumSet.of(
            DocumentoStatusEnum.APROVADO, DocumentoStatusEnum.ALTERADO, DocumentoStatusEnum.EM_REVOGACAO,
            DocumentoStatusEnum.MINUTA, DocumentoStatusEnum.EM_ALTERACAO, DocumentoStatusEnum.PUBLICADO);
    private static final Set<DocumentoStatusEnum> STATUS_ACAO_PUBLICADOR = EnumSet.of(
            DocumentoStatusEnum.PUBLICADO, DocumentoStatusEnum.REVOGADO,
            DocumentoStatusEnum.MINUTA, DocumentoStatusEnum.EM_ALTERACAO);

    // Cada transição do fluxo de revisão/publicação tem um dono diferente,
    // dependendo de ONDE o documento está agora e para ONDE está indo -- ver
    // DocumentoStatusEnum/DocumentoStatusService para a tabela completa.
    public boolean podeMudarStatus(Long documentoId, DocumentoStatusEnum novoStatus, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;
        DocumentoStatusEnum statusAtual = doc.getDocumentoStatus();

        // Enviar para revisão/análise de revogação: quem tem posse de editar.
        if (STATUS_ENTRADA_REVISOR.contains(novoStatus)) {
            return podeEditar(documentoId, auth);
        }

        // Única transição sem atribuição pessoal prévia: qualquer papel APROV
        // da mesma OM pode reabrir um documento publicado para alteração.
        if (novoStatus == DocumentoStatusEnum.EM_ALTERACAO && statusAtual == DocumentoStatusEnum.PUBLICADO) {
            return usuario.getPapeis().contains(PapelEnum.APROV) && doc.getOm().getId().equals(usuario.getOm().getId());
        }

        // A partir de EM_REVISAO/ANALISE_REVOGACAO, só quem foi atribuído como
        // revisor decide o próximo passo (aprovar, aprovar revogação ou devolver).
        if (statusAtual == DocumentoStatusEnum.EM_REVISAO || statusAtual == DocumentoStatusEnum.ANALISE_REVOGACAO) {
            if (!STATUS_ACAO_REVISOR.contains(novoStatus)) return false;
            return doc.getRevisorAtribuido() != null && doc.getRevisorAtribuido().getId().equals(usuario.getId());
        }

        // A partir de EM_PUBLICACAO/EM_REVOGACAO, só quem foi atribuído como
        // publicador decide o próximo passo (publicar, revogar ou devolver).
        if (statusAtual == DocumentoStatusEnum.EM_PUBLICACAO || statusAtual == DocumentoStatusEnum.EM_REVOGACAO) {
            if (!STATUS_ACAO_PUBLICADOR.contains(novoStatus)) return false;
            return doc.getPublicadorAtribuido() != null && doc.getPublicadorAtribuido().getId().equals(usuario.getId());
        }

        // Demais transições (ex.: RASCUNHO/MINUTA -> CANCELADO) seguem a mesma
        // posse de editar -- quem conduz o rascunho decide cancelá-lo.
        return podeEditar(documentoId, auth);
    }

    private boolean ehAutor(Documento doc, Usuario usuario) {
        return doc.getAutor().getId().equals(usuario.getId());
    }

    private Usuario usuarioDe(Authentication auth) {
        return ((UsuarioPrincipal) auth.getPrincipal()).getUsuario();
    }
}
