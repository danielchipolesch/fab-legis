package intraer.fablegis.domain.services;

import lombok.RequiredArgsConstructor;
import intraer.fablegis.application.dtos.buscaDtos.ItemBuscaResponseDto;
import intraer.fablegis.infrastructure.repositories.BuscaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoBuscaService {

    private final BuscaRepository buscaRepository;

    public Page<ItemBuscaResponseDto> buscar(String termo, Pageable pageable) {
        if (termo == null || termo.isBlank()) return Page.empty(pageable);
        List<ItemBuscaResponseDto> itens = buscaRepository.buscar(termo, pageable.getPageSize(), pageable.getOffset());
        long total = buscaRepository.contar(termo);
        return new PageImpl<>(itens, pageable, total);
    }
}
