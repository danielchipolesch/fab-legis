# Contexto de build: a RAIZ do repositório (docker-compose.yml: context ".", dockerfile collab/Dockerfile) -- o servidor usa o
# modelo do documento que fica em frontend/src/editor/modelo (ver collab/schema.js).
FROM node:20-alpine

WORKDIR /app/collab

COPY collab/package.json collab/package-lock.json ./
RUN npm ci --omit=dev

COPY collab/server.js collab/schema.js collab/metricas.js ./

# O modelo do documento (compartilhado com o editor), no mesmo caminho relativo que tem no repositório. O package.json do
# frontend só informa "type": "module"; o link faz os imports de @tiptap/* desses arquivos acharem o node_modules do collab.
COPY frontend/package.json /app/frontend/package.json
COPY frontend/src/editor/modelo /app/frontend/src/editor/modelo
COPY frontend/src/utils/estiloDeTabela.js /app/frontend/src/utils/estiloDeTabela.js
RUN ln -s /app/collab/node_modules /app/frontend/node_modules

EXPOSE 1234

CMD ["node", "server.js"]
