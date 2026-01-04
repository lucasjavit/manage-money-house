# Multi-stage build para otimizar o tamanho da imagem
FROM node:20-alpine AS build

WORKDIR /app

# Argumento para URL da API
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL

# Copiar arquivos de dependências primeiro (cache layer)
COPY package*.json ./
RUN npm ci

# Copiar código fonte e compilar
COPY . .
RUN npm run build

# Imagem final com Nginx
FROM nginx:alpine

# Copiar arquivos compilados
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuração customizada do Nginx (se necessário)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expor porta
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Nginx já inicia automaticamente, não precisa de CMD

