# Deploy no Coolify - Guia Rápido

## Arquivos Criados

✅ **Backend:**
- `backend/Dockerfile` - Build multi-stage otimizado
- `backend/src/main/resources/application-prod.properties` - Configurações de produção

✅ **Frontend:**
- `frontend/Dockerfile` - Build com Nginx
- `frontend/nginx.conf` - Configuração do Nginx para SPA

✅ **Docker Compose:**
- `docker-compose.prod.yml` - Para deploy completo (opcional)

✅ **Documentação:**
- `DEPLOY.md` - Guia completo de deploy
- `.env.example` - Exemplo de variáveis de ambiente

## Passos Rápidos no Coolify

### 1. Backend

1. Nova aplicação → Dockerfile
2. **Build Context**: `./backend`
3. **Dockerfile**: `./backend/Dockerfile`
4. **Porta**: `8080`
5. **Variáveis de Ambiente**:
   ```
   SPRING_PROFILES_ACTIVE=prod
   DATABASE_URL=jdbc:postgresql://seu-postgres:5432/manage_house_money
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=sua_senha
   CORS_ALLOWED_ORIGINS=https://seu-dominio.com
   ```

### 2. Frontend

1. Nova aplicação → Dockerfile
2. **Build Context**: `./frontend`
3. **Dockerfile**: `./frontend/Dockerfile`
4. **Porta**: `80`
5. **Build Args**:
   ```
   VITE_API_URL=/api
   ```

### 3. PostgreSQL

Use um serviço PostgreSQL do Coolify ou configure externamente.

## Health Checks

- **Backend**: `http://localhost:8080/actuator/health`
- **Frontend**: `http://localhost/`

## Próximos Passos

1. Configure domínio no Coolify
2. Configure SSL/HTTPS
3. Ajuste `CORS_ALLOWED_ORIGINS` com o domínio real
4. Configure backups do banco

Veja `DEPLOY.md` para detalhes completos.

