package br.com.danielchipolesch.domain.services;

import br.com.danielchipolesch.application.dtos.buscaDtos.ItemBuscaResponseDto;
import br.com.danielchipolesch.infrastructure.repositories.BuscaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentoBuscaService {

    @Autowired
    private BuscaRepository buscaRepository;

    public Page<ItemBuscaResponseDto> buscar(String termo, Pageable pageable) {
        if (termo == null || termo.isBlank()) return Page.empty(pageable);
        List<ItemBuscaResponseDto> itens = buscaRepository.buscar(termo, pageable.getPageSize(), pageable.getOffset());
        long total = buscaRepository.contar(termo);
        return new PageImpl<>(itens, pageable, total);
    }
}
