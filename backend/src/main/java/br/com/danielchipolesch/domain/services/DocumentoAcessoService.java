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
@Service
public class DocumentoAcessoService {

    private static final Set<DocumentoStatusEnum> STATUS_EXCLUIVEIS = EnumSet.of(
            DocumentoStatusEnum.RASCUNHO, DocumentoStatusEnum.MINUTA);

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoCompartilhamentoRepository compartilhamentoRepository;

    public boolean podeEditar(Long documentoId, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc == null) return false;
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

    // APROVADOR só aprova/publica documentos da própria OM -- ver a pergunta
    // em aberto no design doc sobre se essa restrição deve mesmo se manter
    // agora que visualizar é universal; por ora, mantida.
    public boolean podeAprovarNaOm(Long documentoId, Authentication auth) {
        Usuario usuario = usuarioDe(auth);
        if (!usuario.getPapeis().contains(PapelEnum.ADMIN) && !usuario.getPapeis().contains(PapelEnum.APROVADOR)) {
            return false;
        }
        if (usuario.getPapeis().contains(PapelEnum.ADMIN)) return true;
        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        return doc != null && doc.getOm().getId().equals(usuario.getOm().getId());
    }

    // Transições que representam aprovar/publicar/consolidar o documento
    // exigem APROVADOR (ou ADMIN) da mesma OM; as demais (ex.: RASCUNHO ->
    // MINUTA, ou CANCELADO) seguem a mesma posse de editar -- o próprio
    // REDATOR conduz o rascunho até pedir aprovação.
    private static final Set<DocumentoStatusEnum> STATUS_REQUER_APROVADOR = EnumSet.of(
            DocumentoStatusEnum.APROVADO, DocumentoStatusEnum.PUBLICADO, DocumentoStatusEnum.ALTERADO,
            DocumentoStatusEnum.EM_ALTERACAO, DocumentoStatusEnum.ARQUIVADO, DocumentoStatusEnum.REVOGADO);

    public boolean podeMudarStatus(Long documentoId, DocumentoStatusEnum novoStatus, Authentication auth) {
        if (STATUS_REQUER_APROVADOR.contains(novoStatus)) {
            return podeAprovarNaOm(documentoId, auth);
        }
        return podeEditar(documentoId, auth);
    }

    private boolean ehAutor(Documento doc, Usuario usuario) {
        return doc.getAutor().getId().equals(usuario.getId());
    }

    private Usuario usuarioDe(Authentication auth) {
        return ((UsuarioPrincipal) auth.getPrincipal()).getUsuario();
    }
}
