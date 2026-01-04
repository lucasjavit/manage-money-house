# Guia de Deploy no Coolify

Este guia explica como fazer o deploy da aplicação ManageHouseMoney no Coolify.

> 📖 **Para um guia passo a passo detalhado, veja [COOLIFY-DEPLOY.md](./COOLIFY-DEPLOY.md)**

## Pré-requisitos

1. Coolify instalado e configurado
2. PostgreSQL disponível (pode ser um serviço separado ou container)
3. Acesso ao repositório Git

## Estrutura do Projeto

O projeto consiste em dois serviços:
- **Backend**: Spring Boot (Java 21) na porta 8080
- **Frontend**: React + Vite (Nginx) na porta 80

## Configuração no Coolify

### 1. Criar Aplicação Backend

1. No Coolify, crie uma nova aplicação
2. Selecione "Docker Compose" ou "Dockerfile"
3. Configure:
   - **Nome**: `manage-house-money-backend`
   - **Build Context**: `./backend`
   - **Dockerfile**: `./backend/Dockerfile`
   - **Porta**: `8080`

### 2. Variáveis de Ambiente do Backend

Configure as seguintes variáveis de ambiente:

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://seu-postgres:5432/manage_house_money
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=sua_senha
DDL_AUTO=update
SHOW_SQL=false
EXCHANGE_RATE=5.42
OPENAI_API_URL=https://api.openai.com/v1/chat/completions
OPENAI_MODEL=gpt-4o-mini
PORT=8080
JAVA_OPTS=-Xmx512m -Xms256m
```

### 3. Criar Aplicação Frontend

1. Crie outra aplicação no Coolify
2. Configure:
   - **Nome**: `manage-house-money-frontend`
   - **Build Context**: `./frontend`
   - **Dockerfile**: `./frontend/Dockerfile`
   - **Porta**: `80`

### 4. Variáveis de Ambiente do Frontend

```env
VITE_API_URL=http://seu-backend-url:8080
```

**Nota**: Ajuste `VITE_API_URL` para apontar para a URL do seu backend no Coolify.

### 5. Configurar PostgreSQL

Você pode usar:
- Um serviço PostgreSQL do Coolify
- Um banco de dados externo
- Um container PostgreSQL separado

**Configuração mínima do PostgreSQL:**
- Database: `manage_house_money`
- Usuário: `postgres` (ou outro)
- Senha: (defina uma senha segura)

## Build e Deploy

### Backend

O Dockerfile do backend:
1. Compila o projeto Maven
2. Cria uma imagem JRE otimizada
3. Expõe a porta 8080
4. Inclui health check

### Frontend

O Dockerfile do frontend:
1. Compila o projeto React/Vite
2. Serve com Nginx
3. Configura roteamento SPA
4. Expõe a porta 80

## Variáveis de Ambiente Importantes

### Backend

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DATABASE_URL` | URL do banco PostgreSQL | `jdbc:postgresql://localhost:5432/manage_house_money` |
| `DATABASE_USERNAME` | Usuário do banco | `postgres` |
| `DATABASE_PASSWORD` | Senha do banco | `postgres` |
| `DDL_AUTO` | Modo de atualização do schema | `update` |
| `SHOW_SQL` | Mostrar SQL no log | `false` |
| `EXCHANGE_RATE` | Taxa de câmbio USD/BRL | `5.42` |
| `PORT` | Porta do servidor | `8080` |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas para CORS (separadas por vírgula) | `*` |
| `JAVA_OPTS` | Opções JVM | `-Xmx512m -Xms256m` |

### Frontend

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `VITE_API_URL` | URL da API backend (usado no build) | `/api` |

## Health Checks

### Backend
- Endpoint: `http://localhost:8080/actuator/health`
- Intervalo: 30s
- Timeout: 3s

**Nota**: Se o Spring Boot Actuator não estiver configurado, você pode criar um endpoint simples de health check.

## Troubleshooting

### Backend não conecta ao banco
- Verifique se `DATABASE_URL` está correto
- Confirme que o PostgreSQL está acessível
- Verifique credenciais

### Frontend não acessa o backend
- Verifique `VITE_API_URL` no frontend
- Confirme que o backend está rodando
- Verifique CORS no backend

### Erro de build
- Verifique logs do build no Coolify
- Confirme que todas as dependências estão no Dockerfile
- Verifique se o Java 21 está disponível

## Próximos Passos

1. Configure o domínio no Coolify
2. Configure SSL/HTTPS
3. Configure backups do banco de dados
4. Configure monitoramento e alertas

