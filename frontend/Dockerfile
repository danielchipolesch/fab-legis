# ── Estágio base: instala dependências ────────────────────────────────────────
FROM node:20-alpine AS base
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci --prefer-offline

# ── Estágio de desenvolvimento (Vite HMR) ─────────────────────────────────────
FROM base AS development
COPY frontend/ .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

# ── Estágio de build para produção ────────────────────────────────────────────
FROM base AS build
ARG VITE_API_BASE_URL=http://localhost:8081/v1
ARG VITE_USE_MOCK_API=false
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_USE_MOCK_API=$VITE_USE_MOCK_API
COPY frontend/ .
RUN npm run build

# ── Estágio de produção (Nginx) ────────────────────────────────────────────────
FROM nginx:1.27-alpine AS production
COPY --from=build /app/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1
CMD ["nginx", "-g", "daemon off;"]
