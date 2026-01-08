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
RUN npm run build && \
    echo "=== Verificando build ===" && \
    ls -la dist/ && \
    test -f dist/index.html && echo "✓ index.html gerado" || (echo "✗ ERRO: index.html não foi gerado!" && exit 1) && \
    echo "✓ Build concluído com sucesso"

# Imagem final com Nginx
FROM nginx:alpine

# Remover configuração padrão do nginx (pode causar conflitos)
RUN rm -f /etc/nginx/conf.d/default.conf

# Copiar arquivos compilados do build
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuração customizada do Nginx
# IMPORTANTE: nginx.conf deve estar no diretório frontend/
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar script de entrada
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Verificar se os arquivos foram copiados corretamente (debug)
RUN echo "=== Verificando arquivos HTML ===" && \
    ls -la /usr/share/nginx/html/ && \
    echo "=== Verificando index.html ===" && \
    test -f /usr/share/nginx/html/index.html && echo "✓ index.html existe" || echo "✗ index.html NÃO existe" && \
    echo "=== Verificando assets ===" && \
    ls -la /usr/share/nginx/html/assets/ 2>/dev/null || echo "Pasta assets não encontrada" && \
    echo "=== Verificando configuração nginx ===" && \
    ls -la /etc/nginx/conf.d/ && \
    echo "=== Testando configuração nginx ===" && \
    nginx -t

# Expor porta
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Usar script de entrada customizado
ENTRYPOINT ["/docker-entrypoint.sh"]

