# ── Estágio base: instala dependências ────────────────────────────────────────
FROM node:22-alpine AS base
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install

# ── Estágio de desenvolvimento (Vite HMR) ─────────────────────────────────────
FROM base AS development
COPY frontend/ .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

# ── Estágio de build para produção ────────────────────────────────────────────
FROM base AS build
ARG VITE_API_BASE_URL=http://localhost:8081/v1
ARG VITE_USE_MOCK_API=false
ARG VITE_COLLAB_URL=ws://localhost:1234
ARG VITE_DOCS_URL=http://localhost:8000
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_USE_MOCK_API=$VITE_USE_MOCK_API
ENV VITE_COLLAB_URL=$VITE_COLLAB_URL
ENV VITE_DOCS_URL=$VITE_DOCS_URL
COPY frontend/ .
RUN npm run build

# ── Estágio de produção (Nginx) ────────────────────────────────────────────────
FROM nginx:1.28-alpine AS production
COPY --from=build /app/dist /usr/share/nginx/html
# A configuração do Nginx (nginx/nginx.conf, por ambiente e fora do git) NÃO vai na imagem: o docker-compose.yml a monta em
# /etc/nginx/conf.d/default.conf. Copie de nginx/nginx.conf.example antes de subir o perfil production.
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health || exit 1
CMD ["nginx", "-g", "daemon off;"]
