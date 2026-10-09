package intraer.fablegis.domain.services;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.dtos.anexoDtos.AnexoResponseDto;
import intraer.fablegis.application.dtos.documentoDtos.DocumentoStatusRequestDto;
import intraer.fablegis.domain.entities.estruturaDocumento.Anexo;
import intraer.fablegis.domain.entities.estruturaDocumento.Documento;
import intraer.fablegis.domain.entities.estruturaDocumento.OrientacaoDoAnexo;
import intraer.fablegis.domain.entities.estruturaDocumento.SituacaoLocalEnum;
import intraer.fablegis.domain.handlers.exceptions.InvalidInputException;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.handlers.exceptions.StatusCannotBeUpdatedException;
import intraer.fablegis.domain.handlers.exceptions.enums.DocumentoException;
import intraer.fablegis.infrastructure.repositories.AnexoRepository;
import intraer.fablegis.infrastructure.repositories.DocumentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AnexoService {

    private final AnexoRepository anexoRepository;

    private final DocumentoRepository documentoRepository;

    private final ImagemService imagemService;

    private final DocumentoStatusService documentoStatusService;

    public List<AnexoResponseDto> listar(Long documentoId) {
        documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));
        return anexoRepository.findByDocumentoIdOrderByOrdemAsc(documentoId)
                .stream().map(AnexoResponseDto::from).toList();
    }

    // orientacao: "RETRATO" | "PAISAGEM" (sem diferenciar maiúsculas); nula ou em branco, é sugerida pela proporção da imagem.
    @Transactional
    public AnexoResponseDto adicionar(Long documentoId, String titulo, MultipartFile arquivo, String orientacao) throws Exception {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));

        exigirDocumentoEmEdicao(documento);
        OrientacaoDoAnexo orientacaoDoAnexo = orientacaoEscolhida(orientacao, arquivo);

        String url = imagemService.uploadImagem(arquivo);

        int proximaOrdem = anexoRepository.findMaxOrdemByDocumentoId(documentoId) + 1;

        Anexo anexo = new Anexo();
        anexo.setDocumento(documento);
        anexo.setTitulo(titulo);
        anexo.setUrlImagem(url);
        anexo.setOrdem(proximaOrdem);
        anexo.setOrientacao(orientacaoDoAnexo);

        AnexoResponseDto resultado = AnexoResponseDto.from(anexoRepository.save(anexo));

        if (documento.getSituacaoLocal() == SituacaoLocalEnum.RASCUNHO) {
            documentoStatusService.changeStatus(documentoId, new DocumentoStatusRequestDto(
                    SituacaoLocalEnum.MINUTA, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null));
        }

        return resultado;
    }

    // A orientação enviada vale; sem ela, vem da proporção da imagem (OrientacaoDoAnexo.sugeridaPara).
    private OrientacaoDoAnexo orientacaoEscolhida(String orientacao, MultipartFile arquivo) {
        if (orientacao == null || orientacao.isBlank()) {
            return sugerirPelaImagem(arquivo);
        }
        try {
            return OrientacaoDoAnexo.valueOf(orientacao.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Orientação do anexo inválida: use RETRATO ou PAISAGEM.");
        }
    }

    // Lê só o cabeçalho da imagem (sem decodificá-la inteira). Formato que o ImageIO não conhece ou arquivo ilegível:
    // retrato, o padrão de sempre.
    private OrientacaoDoAnexo sugerirPelaImagem(MultipartFile arquivo) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(arquivo.getInputStream())) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) return OrientacaoDoAnexo.RETRATO;
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                return OrientacaoDoAnexo.sugeridaPara(leitor.getWidth(0), leitor.getHeight(0));
            } finally {
                leitor.dispose();
            }
        } catch (Exception e) {
            return OrientacaoDoAnexo.RETRATO;
        }
    }

    // Os anexos fazem parte do documento, então seguem a mesma regra de edição do resto dele (metadados e parte normativa):
    // quem pode pedir é decidido no controller (@PreAuthorize podeEditar); aqui só se barra a etapa -- ver SituacaoLocalEnum.aceitaEdicao.
    private void exigirDocumentoEmEdicao(Documento documento) {
        if (!documento.getSituacaoLocal().aceitaEdicao()) {
            throw new StatusCannotBeUpdatedException(DocumentoException.ANEXOS_CANNOT_BE_UPDATED.getMessage());
        }
    }

    @Transactional
    public void remover(Long documentoId, Long anexoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(DocumentoException.NOT_FOUND.getMessage()));
        exigirDocumentoEmEdicao(documento);

        Anexo anexo = anexoRepository.findByIdAndDocumentoId(anexoId, documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Anexo não encontrado."));
        anexoRepository.delete(anexo);

        // Reordena os anexos restantes
        List<Anexo> restantes = anexoRepository.findByDocumentoIdOrderByOrdemAsc(documentoId);
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).setOrdem(i + 1);
            anexoRepository.save(restantes.get(i));
        }
    }
}
