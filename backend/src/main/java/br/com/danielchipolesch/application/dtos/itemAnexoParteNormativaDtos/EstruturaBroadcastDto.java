package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import java.util.List;

// Envelope do evento SSE "estrutura" -- carrega o id do cliente que originou a mudança
// (gerado uma vez por aba no frontend, ver frontend/src/utils/clientId.js) junto com
// os eventos, para que quem DISPAROU o PATCH /secoes possa ignorar o próprio eco (já
// aplicou a mudança localmente antes mesmo de mandar a requisição) em vez de
// reprocessá-la — evita duplicar elementos criados por ela mesma numa corrida entre a
// resposta HTTP direta e este broadcast.
public record EstruturaBroadcastDto(
        String origem,
        List<EventoEstruturaDto> eventos
) {
}
