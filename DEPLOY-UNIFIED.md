# Deploy Unificado - ManageHouseMoney

Esta aplicação foi configurada para deploy com **frontend e backend unificados** em um único container.

## Arquitetura

- **Frontend**: React + Vite (buildado em tempo de build)
- **Backend**: Spring Boot (serve API + frontend estático)
- **Banco de Dados**: PostgreSQL 16

## Configuração no Coolify

### 1. Configurações Básicas

- **Base Directory**: `/deploy`
- **Docker Compose File**: `docker-compose.yaml`

### 2. Variáveis de Ambiente (Opcionais)

```bash
# Database (já configuradas no docker-compose)
DATABASE_URL=jdbc:postgresql://postgres:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=0wCtsnUWtuabJax0aFdHrH5te7kDpvtGGWM3BNWuwunPOplPCNIfAq3F7kyUjDp5

# Application
DDL_AUTO=update
SHOW_SQL=false
EXCHANGE_RATE=5.42

# OpenAI (configure sua chave)
OPENAI_API_URL=https://api.openai.com/v1/chat/completions
OPENAI_MODEL=gpt-4o-mini
OPENAI_API_KEY=sua-chave-aqui

# Java
JAVA_OPTS=-Xmx512m -Xms256m

# CORS (não precisa mais configurar domínios específicos)
CORS_ALLOWED_ORIGINS=*
```

### 3. Serviço Único

A aplicação agora roda em um único serviço chamado `app`:
- **Nome**: manage-house-money-app
- **Porta**: 8080
- **Endpoints**:
  - `/` - Frontend (React SPA)
  - `/api/*` - Backend REST API
  - `/actuator/health` - Health check

### 4. Domínio

Configure apenas **um domínio** no Coolify apontando para o serviço `app` na porta 8080.

Exemplo:
```
https://manage-money.seu-dominio.com -> app:8080
```

## Como Funciona

1. **Build**: O Dockerfile.prod faz o build do frontend e backend em etapas separadas
2. **Frontend**: Arquivos estáticos são copiados para `/app/static` no container
3. **Backend**: Spring Boot serve a API em `/api/*` e os arquivos estáticos do frontend em `/*`
4. **Roteamento**: O Spring está configurado para:
   - Servir arquivos estáticos (JS, CSS, imagens) quando eles existem
   - Retornar `index.html` para todas as rotas SPA (exceto `/api`)
   - Servir API normalmente em `/api/*`

## Vantagens

- ✅ Deploy simplificado (apenas um serviço)
- ✅ Não precisa configurar proxy/nginx
- ✅ Menos problemas de roteamento e CORS
- ✅ Menor uso de recursos (menos containers)
- ✅ URL única para frontend e backend

## Testando Localmente

```bash
cd deploy
docker compose up --build
```

Acesse: http://localhost:8080

## Troubleshooting

### Frontend não carrega
- Verifique se o build do frontend foi concluído: `docker logs manage-house-money-app | grep "frontend-build"`
- Verifique se os arquivos estão em `/app/static`: `docker exec manage-house-money-app ls /app/static`

### API não responde
- Verifique health check: `curl http://localhost:8080/actuator/health`
- Verifique logs: `docker logs manage-house-money-app`

### Erro de conexão com banco
- Verifique se o PostgreSQL está healthy: `docker ps`
- Verifique logs do postgres: `docker logs manage-house-money-db`
