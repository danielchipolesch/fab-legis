FROM node:20-alpine AS base
WORKDIR /app
COPY frontend/package*.json ./

# Development stage
FROM base AS development
RUN npm install
COPY frontend/ .
EXPOSE 5173
CMD ["npm", "run", "dev"]

# Build stage
FROM base AS build
RUN npm ci
COPY frontend/ .
RUN npm run build

# Production stage (nginx)
FROM nginx:alpine AS production
COPY --from=build /app/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
