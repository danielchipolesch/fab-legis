package intraer.fablegis.domain.services;

import intraer.fablegis.application.dtos.usuarioDtos.OrganizacaoMilitarResponseDto;
import intraer.fablegis.domain.handlers.exceptions.ResourceNotFoundException;
import intraer.fablegis.domain.regras.comunicacaooficialpadronizada.DistintivoDaOm;
import intraer.fablegis.infrastructure.repositories.OrganizacaoMilitarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// Cadastro do DOM (distintivo) de cada OM, feito pelo administrador. A NPA lê a imagem da OM do próprio documento
// (CabecalhoDaNpa), então trocar o distintivo vale para as NPA ainda não publicadas e para as próximas gerações de PDF/HTML.
@Service
@RequiredArgsConstructor
public class DistintivoDaOmService {

    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;
    private final ImagemService imagemService;

    @Transactional
    public OrganizacaoMilitarResponseDto definir(Long omId, MultipartFile arquivo) throws Exception {
        var om = organizacaoMilitarRepository.findById(omId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização militar não encontrada."));
        DistintivoDaOm.validar(arquivo.getContentType(), arquivo.getSize());
        String anterior = om.getUrlDistintivo();
        om.setUrlDistintivo(imagemService.uploadImagem(arquivo));
        var salva = organizacaoMilitarRepository.save(om);
        imagemService.remover(anterior);
        return OrganizacaoMilitarResponseDto.from(salva);
    }

    @Transactional
    public OrganizacaoMilitarResponseDto remover(Long omId) {
        var om = organizacaoMilitarRepository.findById(omId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização militar não encontrada."));
        String anterior = om.getUrlDistintivo();
        om.setUrlDistintivo(null);
        var salva = organizacaoMilitarRepository.save(om);
        imagemService.remover(anterior);
        return OrganizacaoMilitarResponseDto.from(salva);
    }
}
