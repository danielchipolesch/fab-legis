package intraer.fablegis.domain.services;

import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import intraer.fablegis.infrastructure.security.UsuarioPrincipal;
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

    // Pode editar o que COMPÕE o documento (texto de cada elemento, estrutura, metadados, anexos, cabeçalho da NPA, emendas)
    // AGORA -- a etapa manda, e é aqui, na única fonte de checagem de acesso, que ela é barrada (antes, só alguns serviços
    // barravam, cada um à sua maneira, e o conteúdo de um documento já aprovado ainda podia ser alterado pela API):
    //   RASCUNHO, MINUTA, EM_ALTERACAO : autor/coautor com papel EDIT.
    //   EM_REVISAO                     : SÓ a pessoa atribuída como revisora (papel APROV), e só se o documento ainda não foi
    //                                    publicado -- a revisão de uma alteração de ato PUBLICADO é somente leitura (o texto
    //                                    vigente só muda por emenda, em EM_ALTERACAO). Nem o autor edita durante a revisão.
    //   demais etapas (EM_PUBLICACAO em diante, todo o fluxo de revogação, CANCELADO, SEM_ETAPA) : ninguém, nem o autor.
    // Mesma regra que a interface já aplica (podeEditarAgora, DocumentoEditorPage.vue); ver docs/ciclo-de-vida.md.
    public boolean podeEditar(Long documentoId, Authentication auth) {
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null || !doc.getSituacaoLocal().aceitaEdicao()) return false;

        Usuario usuario = usuarioDe(auth);
        if (doc.getSituacaoLocal() == SituacaoLocalEnum.EM_REVISAO) {
            return doc.getSituacaoBca() != SituacaoBcaEnum.PUBLICADO && ehRevisorAtribuido(doc, usuario);
        }
        return usuario.getPapeis().contains(PapelEnum.EDIT)
                && (ehAutor(doc, usuario) || compartilhamentoRepository.existsByDocumentoIdAndUsuarioId(documentoId, usuario.getId()));
    }

    // Subir o PDF da portaria (ou o da revogação) é um passo de quem vai registrar a publicação -- a pessoa atribuída como
    // publicador, enquanto o documento aguarda a publicação ou a revogação --, não de quem edita o documento (que a essa
    // altura está congelado). Mesma atribuição pessoal de podeMudarStatus para SEM_ETAPA.
    public boolean podeEnviarPortaria(Long documentoId, Authentication auth) {
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;
        if (doc.getSituacaoLocal() != SituacaoLocalEnum.EM_PUBLICACAO && doc.getSituacaoLocal() != SituacaoLocalEnum.EM_REVOGACAO) {
            return false;
        }
        Usuario usuario = usuarioDe(auth);
        return doc.getPublicadorAtribuido() != null && doc.getPublicadorAtribuido().getId().equals(usuario.getId());
    }

    private boolean ehRevisorAtribuido(Documento doc, Usuario usuario) {
        return doc.getRevisorAtribuido() != null && doc.getRevisorAtribuido().getId().equals(usuario.getId());
    }

    // Posse do documento, SEM olhar a etapa: autor/coautor com papel EDIT ou, durante EM_REVISAO, a pessoa atribuída
    // como revisora. É o que autoriza mudar de etapa (enviar para revisão, pedir análise de revogação, cancelar...) e
    // excluir -- coisas que se pedem justamente em etapas em que o conteúdo já está congelado (ex.: pedir a revogação de
    // um ato publicado, em SEM_ETAPA). Editar o conteúdo é outra pergunta: ver podeEditar.
    private boolean temPosse(Long documentoId, Authentication auth) {
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
        return temPosse(documentoId, auth);
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
            return temPosse(documentoId, auth);
        }

        // Única transição sem atribuição pessoal prévia: qualquer papel APROV da mesma OM pode
        // reabrir um documento publicado para alteração -- e desistir dela (cancelar), além de
        // quem já conduz a edição (autor/coautor).
        if (destino == SituacaoLocalEnum.EM_ALTERACAO && atual == SituacaoLocalEnum.SEM_ETAPA) {
            return ehAprovadorDaOm(doc, usuario);
        }
        if (destino == SituacaoLocalEnum.SEM_ETAPA && atual == SituacaoLocalEnum.EM_ALTERACAO) {
            return ehAprovadorDaOm(doc, usuario) || temPosse(documentoId, auth);
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
        return temPosse(documentoId, auth);
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
