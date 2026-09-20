package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.Documento;
import br.com.danielchipolesch.domain.entities.estruturaDocumento.SituacaoLocalEnum;
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

    private static final Set<SituacaoLocalEnum> STATUS_EXCLUIVEIS = EnumSet.of(
            SituacaoLocalEnum.RASCUNHO, SituacaoLocalEnum.MINUTA);

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoCompartilhamentoRepository compartilhamentoRepository;

    // Autor/coautor com papel EDIT -- exceto durante EM_REVISAO, onde a pessoa
    // ATRIBUÍDA como revisora (papel APROV) também pode editar (ver
    // SituacaoLocalEnum.EM_REVISAO). Fora daí (EM_PUBLICACAO em diante, ou o
    // fluxo de revogação inteiro) ninguém edita, nem o autor.
    public boolean podeEditar(Long documentoId, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;

        if (doc.getSituacaoLocal() == SituacaoLocalEnum.EM_REVISAO
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
        if (!STATUS_EXCLUIVEIS.contains(doc.getSituacaoLocal())) return false;
        return podeEditar(documentoId, auth);
    }

    private static final Set<SituacaoLocalEnum> DESTINOS_ENTRADA_REVISOR = EnumSet.of(
            SituacaoLocalEnum.EM_REVISAO, SituacaoLocalEnum.ANALISE_REVOGACAO);
    // O que o revisor atribuído pode pedir a partir de EM_REVISAO/ANALISE_REVOGACAO: aprovar
    // (EM_PUBLICACAO / EM_REVOGACAO) ou devolver (MINUTA / EM_ALTERACAO / SEM_ETAPA).
    private static final Set<SituacaoLocalEnum> DESTINOS_ACAO_REVISOR = EnumSet.of(
            SituacaoLocalEnum.EM_PUBLICACAO, SituacaoLocalEnum.EM_REVOGACAO,
            SituacaoLocalEnum.MINUTA, SituacaoLocalEnum.EM_ALTERACAO, SituacaoLocalEnum.SEM_ETAPA);
    // O que o publicador atribuído pode pedir a partir de EM_PUBLICACAO/EM_REVOGACAO: registrar
    // a portaria (SEM_ETAPA) ou devolver (MINUTA / EM_ALTERACAO -- só de EM_PUBLICACAO).
    private static final Set<SituacaoLocalEnum> DESTINOS_ACAO_PUBLICADOR = EnumSet.of(
            SituacaoLocalEnum.SEM_ETAPA, SituacaoLocalEnum.MINUTA, SituacaoLocalEnum.EM_ALTERACAO);

    // Cada transição do fluxo de revisão/publicação tem um dono diferente, dependendo de ONDE
    // o documento está agora e para ONDE está indo -- ver SituacaoLocalEnum/DocumentoStatusService
    // para a tabela completa. Aqui só se decide QUEM pode pedir; se a transição em si é válida é
    // coisa do DocumentoStatusService.
    public boolean podeMudarStatus(Long documentoId, SituacaoLocalEnum destino, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;
        SituacaoLocalEnum atual = doc.getSituacaoLocal();

        // Enviar para revisão / pedir análise de revogação: quem tem posse de editar.
        if (DESTINOS_ENTRADA_REVISOR.contains(destino)) {
            return podeEditar(documentoId, auth);
        }

        // Única transição sem atribuição pessoal prévia: qualquer papel APROV da mesma OM pode
        // reabrir um documento publicado para alteração -- e desistir dela (cancelar), além de
        // quem já conduz a edição (autor/coautor).
        if (destino == SituacaoLocalEnum.EM_ALTERACAO && atual == SituacaoLocalEnum.SEM_ETAPA) {
            return ehAprovadorDaOm(doc, usuario);
        }
        if (destino == SituacaoLocalEnum.SEM_ETAPA && atual == SituacaoLocalEnum.EM_ALTERACAO) {
            return ehAprovadorDaOm(doc, usuario) || podeEditar(documentoId, auth);
        }

        // A partir de EM_REVISAO/ANALISE_REVOGACAO, só quem foi atribuído como revisor decide o
        // próximo passo (aprovar, aprovar revogação ou devolver).
        if (atual == SituacaoLocalEnum.EM_REVISAO || atual == SituacaoLocalEnum.ANALISE_REVOGACAO) {
            if (!DESTINOS_ACAO_REVISOR.contains(destino)) return false;
            return doc.getRevisorAtribuido() != null && doc.getRevisorAtribuido().getId().equals(usuario.getId());
        }

        // A partir de EM_PUBLICACAO/EM_REVOGACAO, só quem foi atribuído como publicador decide o
        // próximo passo (publicar, revogar ou devolver).
        if (atual == SituacaoLocalEnum.EM_PUBLICACAO || atual == SituacaoLocalEnum.EM_REVOGACAO) {
            if (!DESTINOS_ACAO_PUBLICADOR.contains(destino)) return false;
            return doc.getPublicadorAtribuido() != null && doc.getPublicadorAtribuido().getId().equals(usuario.getId());
        }

        // Demais transições (ex.: RASCUNHO/MINUTA -> CANCELADO) seguem a mesma posse de editar --
        // quem conduz o rascunho decide cancelá-lo.
        return podeEditar(documentoId, auth);
    }

    private boolean ehAprovadorDaOm(Documento doc, Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.APROV) && doc.getOm().getId().equals(usuario.getOm().getId());
    }

    private boolean ehAutor(Documento doc, Usuario usuario) {
        return doc.getAutor().getId().equals(usuario.getId());
    }

    private Usuario usuarioDe(Authentication auth) {
        return ((UsuarioPrincipal) auth.getPrincipal()).getUsuario();
    }
}
