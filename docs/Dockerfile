# ── Estágio de build — gera o site estático (mkdocs build) ────────────────────
FROM squidfunk/mkdocs-material:9.5.44 AS build
WORKDIR /docs
COPY mkdocs.yml .
COPY docs/ ./docs
RUN mkdocs build --strict

# ── Estágio de produção (Nginx) ────────────────────────────────────────────────
FROM nginx:1.28-alpine AS production
COPY --from=build /docs/site /usr/share/nginx/html
COPY docs/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1
CMD ["nginx", "-g", "daemon off;"]
