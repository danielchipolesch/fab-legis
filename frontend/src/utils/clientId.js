// Id gerado uma vez por aba/sessão do navegador (módulo ES é singleton -- o mesmo
// valor vale por toda a vida da aba). Enviado em PATCH /documentos/{id}/secoes
// (header X-Client-Id) e devolvido no evento SSE `estrutura`, pra quem originou uma
// mudança estrutural poder ignorar o próprio eco em vez de reaplicá-lo -- ver
// DocumentoEditorPage.vue e DocumentoPresencaEmitterRegistry no backend.
export const clientId = crypto.randomUUID()
