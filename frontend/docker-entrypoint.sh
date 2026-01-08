#!/bin/sh
set -e

# Verificar se os arquivos HTML existem
echo "=== Verificando arquivos HTML ==="
ls -la /usr/share/nginx/html/

if [ ! -f /usr/share/nginx/html/index.html ]; then
    echo "ERRO: index.html não encontrado!"
    exit 1
fi

echo "✓ index.html encontrado"

# Verificar se os assets existem
if [ -d /usr/share/nginx/html/assets ]; then
    echo "✓ Pasta assets encontrada"
    ls -la /usr/share/nginx/html/assets/
else
    echo "⚠ Pasta assets não encontrada (pode ser normal se não houver assets)"
fi

# Substituir o nome do backend no nginx.conf se BACKEND_HOST estiver definido
if [ -n "$BACKEND_HOST" ]; then
    echo "=== Ajustando nome do backend para: $BACKEND_HOST ==="
    sed -i "s|http://backend:8080|http://${BACKEND_HOST}:8080|g" /etc/nginx/conf.d/default.conf
fi

# Testar configuração do nginx
echo "=== Testando configuração nginx ==="
nginx -t

# Iniciar nginx
echo "=== Iniciando nginx ==="
exec nginx -g "daemon off;"

