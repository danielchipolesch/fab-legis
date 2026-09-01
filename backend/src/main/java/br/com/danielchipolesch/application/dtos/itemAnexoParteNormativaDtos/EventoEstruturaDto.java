package br.com.danielchipolesch.application.dtos.itemAnexoParteNormativaDtos;

import br.com.danielchipolesch.domain.entities.estruturaDocumento.ItemAnexoParteNormativaTipoEnum;

// Evento transmitido via SSE (event: estrutura, ver DocumentoPresencaEmitterRegistry)
// sempre que PATCH /{id}/secoes cria, atualiza (parent/ordem/titulo) ou exclui um
// elemento -- permite que outros clientes conectados ao mesmo documento apliquem o
// patch na árvore local sem precisar recarregar o documento inteiro. Nunca carrega
// `conteudo`: esse campo é responsabilidade exclusiva do serviço de colaboração
// (Hocuspocus), que os outros clientes já acompanham por conta própria quando o
// elemento está aberto para edição.
public record EventoEstruturaDto(
        TipoEventoEstrutura tipo,
        Long id,
        Long parentId,
        ItemAnexoParteNormativaTipoEnum elementType,
        Integer elementOrder,
        String elementTitle
) {
    public enum TipoEventoEstrutura { CRIADO, ATUALIZADO, EXCLUIDO }

    public static EventoEstruturaDto criado(Long id, Long parentId, ItemAnexoParteNormativaTipoEnum tipo, Integer elementOrder, String titulo) {
        return new EventoEstruturaDto(TipoEventoEstrutura.CRIADO, id, parentId, tipo, elementOrder, titulo);
    }

    public static EventoEstruturaDto atualizado(Long id, Long parentId, ItemAnexoParteNormativaTipoEnum tipo, Integer elementOrder, String titulo) {
        return new EventoEstruturaDto(TipoEventoEstrutura.ATUALIZADO, id, parentId, tipo, elementOrder, titulo);
    }

    public static EventoEstruturaDto excluido(Long id) {
        return new EventoEstruturaDto(TipoEventoEstrutura.EXCLUIDO, id, null, null, null, null);
    }
}
