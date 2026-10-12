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
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_USE_MOCK_API=$VITE_USE_MOCK_API
ENV VITE_COLLAB_URL=$VITE_COLLAB_URL
COPY frontend/ .
RUN npm run build

# ── Documentação técnica (mkdocs build) -- servida pelo mesmo Nginx em /docs ─────
FROM squidfunk/mkdocs-material:9.5.44 AS docs-build
WORKDIR /docs
COPY mkdocs.yml .
COPY docs/ ./docs
RUN mkdocs build --strict

# ── Estágio de produção (Nginx) ────────────────────────────────────────────────
FROM nginx:1.28-alpine AS production
COPY --from=build /app/dist /usr/share/nginx/html
COPY --from=docs-build /docs/site /usr/share/nginx/html/docs
# O nginx da imagem substitui as variáveis do template (BACKEND_UPSTREAM, COLLAB_UPSTREAM) ao subir e grava em conf.d --
# o mesmo arquivo em desenvolvimento e em produção; só os valores (docker-compose.yml / .env) mudam.
COPY nginx/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/proxy-comum.conf /etc/nginx/proxy_comum.conf
ENV BACKEND_UPSTREAM=backend:8081 COLLAB_UPSTREAM=collab:1234
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health || exit 1
CMD ["nginx", "-g", "daemon off;"]
