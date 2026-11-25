package intraer.fablegis.domain.services;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoBcaEnum;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.entities.usuario.PapelEnum;
import intraer.fablegis.domain.entities.usuario.Usuario;
import intraer.fablegis.domain.regras.RegrasDasEspecies;
import intraer.fablegis.infrastructure.repositories.DocumentoCompartilhamentoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import intraer.fablegis.infrastructure.security.AutenticacaoUtil;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

// Única fonte de checagem de posse do sistema -- ver o design doc de
// autenticação/autorização (§2). Visualizar e baixar PDF NÃO passam por
// aqui: são liberados para qualquer usuário autenticado, em qualquer OM, em
// qualquer situação (basta estar logado, o que o SecurityConfig já exige).
// Só editar, compartilhar e excluir precisam de posse -- e editar depende também da ETAPA do documento (podeEditar):
// conteúdo congelado não é editado por ninguém, nem pelo autor.
//
// ADMIN nunca aparece aqui como bypass: sob o modelo atual de papéis, Admin é
// puramente administrativo (usuários/OMs) e não tem poder nenhum sobre o
// ciclo de vida de um documento -- ver PapelEnum.
@Service
@RequiredArgsConstructor
public class DocumentoAcessoService {

    // Só o rascunho se exclui (e se cancela): um documento que já foi para aprovação precisa ser devolvido aos autores antes.
    private static final Set<SituacaoLocalEnum> STATUS_EXCLUIVEIS = EnumSet.of(SituacaoLocalEnum.RASCUNHO);

    private final DocumentoRepository documentoRepository;

    private final DocumentoCompartilhamentoRepository compartilhamentoRepository;

    private final RegrasDasEspecies regras;

    // Pode editar o que COMPÕE o documento (texto de cada elemento, estrutura, metadados, anexos, cabeçalho da NPA, emendas)
    // AGORA -- a etapa manda, e é aqui, na única fonte de checagem de acesso, que ela é barrada (antes, só alguns serviços
    // barravam, cada um à sua maneira, e o conteúdo de um documento já aprovado ainda podia ser alterado pela API):
    //   RASCUNHO, EM_ALTERACAO : autor/coautor com papel EDIT.
    //   MINUTA                 : SÓ quem aprova (papel APR_OM, de qualquer aprovador da OM do documento), e só se o documento ainda
    //                            não foi publicado -- a aprovação de uma alteração de ato PUBLICADO é somente leitura (o texto
    //                            vigente só muda por emenda, em EM_ALTERACAO). Nem o autor edita na minuta.
    //   demais etapas (EM_PUBLICACAO em diante, todo o fluxo de revogação, CANCELADO, ARQUIVADO, SEM_ETAPA) : ninguém, nem o autor.
    // Mesma regra que a interface já aplica (podeEditarAgora, DocumentoEditorPage.vue); ver docs/ciclo-de-vida.md.
    public boolean podeEditar(Long documentoId, Authentication auth) {
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null || !doc.getSituacaoLocal().aceitaEdicao()) return false;

        Usuario usuario = usuarioDe(auth);
        if (doc.getSituacaoLocal() == SituacaoLocalEnum.MINUTA) {
            return doc.getSituacaoBca() != SituacaoBcaEnum.PUBLICADO && ehAprovadorDaOm(doc, usuario);
        }
        return usuario.getPapeis().contains(PapelEnum.ELB) && ehAutor(doc, usuario);
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
        return ehPublicador(doc, usuarioDe(auth));
    }

    // Publicador: quem a espécie do documento manda publicar -- o Publicador do BCA (CENDOC) nas convencionais, de qualquer OM; o
    // Publicador do Boletim Interno nas NPAs, só o da OM do documento (RegrasDaEspecieNormativa.podePublicar).
    private boolean ehPublicador(Documento doc, Usuario usuario) {
        return regras.para(doc.getEspecieNormativa()).podePublicar(usuario, doc.getOm().getId());
    }

    // Posse do documento, SEM olhar a etapa: autor/coautor com papel EDIT ou, durante a MINUTA, quem aprova. É o que autoriza
    // mudar de etapa (enviar para aprovação, pedir análise de revogação, cancelar...) e
    // excluir -- coisas que se pedem justamente em etapas em que o conteúdo já está congelado (ex.: pedir a revogação de
    // um ato publicado, em SEM_ETAPA). Editar o conteúdo é outra pergunta: ver podeEditar.
    private boolean temPosse(Long documentoId, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;

        if (doc.getSituacaoLocal() == SituacaoLocalEnum.MINUTA && ehAprovadorDaOm(doc, usuario)) {
            return true;
        }

        if (!usuario.getPapeis().contains(PapelEnum.ELB)) return false;
        return ehAutor(doc, usuario);
    }

    // Autoria: quem criou o documento e quem foi acrescentado depois são autores IGUAIS, sem distinção (um coautor é um autor). Qualquer
    // autor atribui a autoria a outra pessoa (compartilha) e vê quem são os autores -- a autoria é da pessoa, não da OM: ela continua
    // editando se for transferida. Quem desvincula um autor é o Administrador da OM do documento (podeAtribuirAutoria) ou o próprio autor,
    // ao se desvincular (podeDesvincular).
    public boolean podeCompartilhar(Long documentoId, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        return doc != null && (ehAutor(doc, usuario) || ehAdministradorDaOm(doc, usuario));
    }

    // Atribuir autoria a qualquer pessoa, desvincular qualquer autor e atribuir a autoria a si mesmo, em qualquer etapa do documento: só o
    // Administrador da OM do documento (o Administrador Geral NÃO -- a função dele é cuidar dos administradores locais).
    public boolean podeAtribuirAutoria(Long documentoId, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        return doc != null && ehAdministradorDaOm(doc, usuario);
    }

    // Desvincular um autor: o Administrador da OM do documento desvincula qualquer autor; o autor desvincula a PRÓPRIA autoria (desvincula-se), mas só
    // depois de sair da OM do documento -- quem ainda é dela não se desvincula (a opção nem aparece); sair da OM é o que torna a
    // autoria dispensável. Que o documento não fique sem autor é conferido por DocumentoCompartilhamentoService.
    public boolean podeDesvincular(Long documentoId, Long usuarioId, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;
        if (ehAdministradorDaOm(doc, usuario)) return true;
        return usuario.getId().equals(usuarioId) && ehAutor(doc, usuario) && !ehDaOmDoDocumento(doc, usuario);
    }

    // Autor OU coautor, mas só enquanto o documento estiver em RASCUNHO -- um rascunho de outra pessoa, não compartilhado com o usuário,
    // nunca aparece como excluível. Fora do rascunho ninguém exclui, nem o próprio autor: um documento que já foi para aprovação
    // (MINUTA) só se exclui depois de a autoridade devolvê-lo aos autores.
    public boolean podeExcluir(Long documentoId, Authentication auth) {
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;
        if (!STATUS_EXCLUIVEIS.contains(doc.getSituacaoLocal())) return false;
        return temPosse(documentoId, auth);
    }

    // Enviar para aprovação (RASCUNHO/EM_ALTERACAO -> MINUTA) e pedir a análise de revogação: quem tem posse de editar.
    private static final Set<SituacaoLocalEnum> DESTINOS_ENTRADA_REVISOR = EnumSet.of(
            SituacaoLocalEnum.MINUTA, SituacaoLocalEnum.ANALISE_REVOGACAO);
    // O que quem aprova pode pedir a partir de MINUTA/ANALISE_REVOGACAO: aprovar (EM_PUBLICACAO / EM_REVOGACAO), devolver aos autores
    // (RASCUNHO / EM_ALTERACAO / SEM_ETAPA) ou arquivar (ARQUIVADO, só da MINUTA).
    private static final Set<SituacaoLocalEnum> DESTINOS_ACAO_REVISOR = EnumSet.of(
            SituacaoLocalEnum.EM_PUBLICACAO, SituacaoLocalEnum.EM_REVOGACAO,
            SituacaoLocalEnum.RASCUNHO, SituacaoLocalEnum.EM_ALTERACAO, SituacaoLocalEnum.SEM_ETAPA,
            SituacaoLocalEnum.ARQUIVADO);
    // O que o publicador pode pedir a partir de EM_PUBLICACAO/EM_REVOGACAO: registrar
    // a portaria (SEM_ETAPA) ou devolver aos autores (RASCUNHO / EM_ALTERACAO -- só de EM_PUBLICACAO).
    private static final Set<SituacaoLocalEnum> DESTINOS_ACAO_PUBLICADOR = EnumSet.of(
            SituacaoLocalEnum.SEM_ETAPA, SituacaoLocalEnum.RASCUNHO, SituacaoLocalEnum.EM_ALTERACAO);

    // Cada transição do fluxo de revisão/publicação tem um dono diferente, dependendo de ONDE
    // o documento está agora e para ONDE está indo -- ver SituacaoLocalEnum/DocumentoStatusService
    // para a tabela completa. Aqui só se decide QUEM pode pedir; se a transição em si é válida é
    // coisa do DocumentoStatusService.
    public boolean podeMudarStatus(Long documentoId, SituacaoLocalEnum destino, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;
        SituacaoLocalEnum atual = doc.getSituacaoLocal();

        // Arquivar é da autoridade (qualquer aprovador da OM do documento) e só de uma minuta: nem os autores arquivam o próprio
        // rascunho (eles o cancelam ou o excluem).
        if (destino == SituacaoLocalEnum.ARQUIVADO) {
            return atual == SituacaoLocalEnum.MINUTA && ehAprovadorDaOm(doc, usuario);
        }

        // Enviar para aprovação / pedir análise de revogação: quem tem posse de editar. A MINUTA como destino parte do RASCUNHO ou da
        // EM_ALTERACAO (os autores); a partir da própria MINUTA quem decide é quem aprova (abaixo).
        if (DESTINOS_ENTRADA_REVISOR.contains(destino) && atual != SituacaoLocalEnum.MINUTA) {
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

        // A partir de MINUTA/ANALISE_REVOGACAO, qualquer aprovador da OM do documento decide o próximo passo (aprovar, aprovar a
        // revogação, devolver aos autores ou, na minuta, arquivar).
        if (atual == SituacaoLocalEnum.MINUTA || atual == SituacaoLocalEnum.ANALISE_REVOGACAO) {
            if (!DESTINOS_ACAO_REVISOR.contains(destino)) return false;
            return ehAprovadorDaOm(doc, usuario);
        }

        // A partir de EM_PUBLICACAO/EM_REVOGACAO, qualquer publicador da espécie decide o
        // próximo passo (publicar, revogar ou devolver).
        if (atual == SituacaoLocalEnum.EM_PUBLICACAO || atual == SituacaoLocalEnum.EM_REVOGACAO) {
            if (!DESTINOS_ACAO_PUBLICADOR.contains(destino)) return false;
            return ehPublicador(doc, usuario);
        }

        // Demais transições (ex.: RASCUNHO -> CANCELADO) seguem a mesma posse de editar --
        // quem conduz o rascunho decide cancelá-lo.
        return temPosse(documentoId, auth);
    }

    private boolean ehAprovadorDaOm(Documento doc, Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.APR_OM) && doc.getOm().getId().equals(usuario.getOm().getId());
    }

    // Autor = quem consta na autoria do documento (t_documento_compartilhamento), incluindo quem o criou: a pessoa que criou entra nela
    // na criação e, como qualquer autor, pode sair dela. Documento.autor é só o registro histórico de quem o criou.
    private boolean ehAutor(Documento doc, Usuario usuario) {
        return compartilhamentoRepository.existsByDocumentoIdAndUsuarioId(doc.getId(), usuario.getId());
    }

    private boolean ehAdministradorDaOm(Documento doc, Usuario usuario) {
        return usuario.getPapeis().contains(PapelEnum.ADM_OM) && ehDaOmDoDocumento(doc, usuario);
    }

    private boolean ehDaOmDoDocumento(Documento doc, Usuario usuario) {
        return doc.getOm().getId().equals(usuario.getOm().getId());
    }

    private Usuario usuarioDe(Authentication auth) {
        return AutenticacaoUtil.usuarioDe(auth);
    }
}
