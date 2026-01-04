# 🚀 Guia Passo a Passo - Deploy no Coolify

Este guia detalha cada passo para fazer o deploy da aplicação ManageHouseMoney no Coolify.

## 📋 Pré-requisitos

- ✅ Coolify instalado e acessível
- ✅ Acesso ao repositório Git (GitHub, GitLab, etc.)
- ✅ Conta no Coolify configurada
- ✅ PostgreSQL disponível (pode ser criado no Coolify)

---

## 📦 PASSO 1: Preparar o Repositório

### 1.1 Verificar Arquivos

Certifique-se de que os seguintes arquivos estão no repositório:

```
ManageHouseMoney/
├── backend/
│   ├── Dockerfile ✅
│   └── src/
├── frontend/
│   ├── Dockerfile ✅
│   └── nginx.conf ✅
└── docker-compose.prod.yml ✅ (opcional)
```

### 1.2 Fazer Commit e Push

```bash
git add .
git commit -m "feat: Preparar para deploy no Coolify"
git push origin main
```

---

## 🗄️ PASSO 2: Criar Banco de Dados PostgreSQL

### 2.1 Criar Serviço PostgreSQL no Coolify

1. No painel do Coolify, vá em **"Services"** ou **"Serviços"**
2. Clique em **"+ New Service"** ou **"+ Novo Serviço"**
3. Selecione **"PostgreSQL"**
4. Configure:
   - **Nome**: `manage-house-money-db`
   - **Versão**: `16` (ou a mais recente)
   - **Database Name**: `manage_house_money`
   - **Username**: `postgres` (ou outro de sua preferência)
   - **Password**: ⚠️ **Crie uma senha forte e anote!**
5. Clique em **"Create"** ou **"Criar"**

### 2.2 Anotar Informações de Conexão

Após criar, anote:
- **Host**: (geralmente algo como `postgres-xxx.coolify.local` ou IP interno)
- **Port**: `5432`
- **Database**: `manage_house_money`
- **Username**: `postgres`
- **Password**: (a senha que você criou)

💡 **Dica**: No Coolify, você pode ver essas informações na página do serviço PostgreSQL.

---

## 🔧 PASSO 3: Criar Aplicação Backend

### 3.1 Criar Nova Aplicação

1. No painel do Coolify, vá em **"Applications"** ou **"Aplicações"**
2. Clique em **"+ New Application"** ou **"+ Nova Aplicação"**
3. Selecione **"Dockerfile"** como tipo de aplicação

### 3.2 Configurar Repositório

1. **Source**: Selecione seu repositório Git (GitHub, GitLab, etc.)
2. **Branch**: `main` (ou a branch que você usa)
3. **Build Pack**: `Dockerfile`

### 3.3 Configurar Build

1. **Build Context**: `./backend`
   - ⚠️ **IMPORTANTE**: Deve ser `./backend` (com o ponto e barra)
2. **Dockerfile Path**: `./backend/Dockerfile`
   - Ou apenas `Dockerfile` se o build context já for `./backend`

### 3.4 Configurar Porta

- **Port**: `8080`
- **Expose Port**: `8080`

### 3.5 Configurar Variáveis de Ambiente

Adicione as seguintes variáveis de ambiente:

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://manage-house-money-db:5432/manage_house_money
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=SUA_SENHA_AQUI
DDL_AUTO=update
SHOW_SQL=false
EXCHANGE_RATE=5.42
OPENAI_API_URL=https://api.openai.com/v1/chat/completions
OPENAI_MODEL=gpt-4o-mini
PORT=8080
CORS_ALLOWED_ORIGINS=*
JAVA_OPTS=-Xmx512m -Xms256m
```

⚠️ **IMPORTANTE**:
- Substitua `SUA_SENHA_AQUI` pela senha do PostgreSQL que você criou
- Substitua `manage-house-money-db` pelo nome/host do seu serviço PostgreSQL
- Ajuste `CORS_ALLOWED_ORIGINS` com o domínio do seu frontend (ex: `https://meuapp.com`)

### 3.6 Configurar Health Check (Opcional)

- **Health Check Path**: `/actuator/health`
- **Health Check Port**: `8080`
- **Health Check Interval**: `30`

### 3.7 Salvar e Fazer Deploy

1. Clique em **"Save"** ou **"Salvar"**
2. Clique em **"Deploy"** ou **"Fazer Deploy"**
3. Aguarde o build e deploy completarem

### 3.8 Verificar Logs

1. Vá em **"Logs"** da aplicação backend
2. Verifique se aparecem mensagens como:
   - `Started ManageHouseMoneyApplication`
   - `Tomcat started on port(s): 8080`

💡 **Dica**: Se houver erros, verifique:
- Se o PostgreSQL está acessível
- Se as credenciais estão corretas
- Se a URL do banco está correta

---

## 🎨 PASSO 4: Criar Aplicação Frontend

### 4.1 Criar Nova Aplicação

1. No painel do Coolify, vá em **"Applications"** ou **"Aplicações"**
2. Clique em **"+ New Application"** ou **"+ Nova Aplicação"**
3. Selecione **"Dockerfile"** como tipo de aplicação

### 4.2 Configurar Repositório

1. **Source**: Selecione o mesmo repositório Git
2. **Branch**: `main` (ou a branch que você usa)
3. **Build Pack**: `Dockerfile`

### 4.3 Configurar Build

1. **Build Context**: `./frontend`
   - ⚠️ **IMPORTANTE**: Deve ser `./frontend` (com o ponto e barra)
2. **Dockerfile Path**: `./frontend/Dockerfile`
   - Ou apenas `Dockerfile` se o build context já for `./frontend`

### 4.4 Configurar Build Arguments

Adicione o seguinte build argument:

```env
VITE_API_URL=/api
```

💡 **Nota**: Se o frontend e backend estiverem em domínios diferentes, use a URL completa do backend:
```env
VITE_API_URL=https://backend.seudominio.com/api
```

### 4.5 Configurar Porta

- **Port**: `80`
- **Expose Port**: `80`

### 4.6 Configurar Health Check (Opcional)

- **Health Check Path**: `/`
- **Health Check Port**: `80`
- **Health Check Interval**: `30`

### 4.7 Salvar e Fazer Deploy

1. Clique em **"Save"** ou **"Salvar"**
2. Clique em **"Deploy"** ou **"Fazer Deploy"**
3. Aguarde o build e deploy completarem

### 4.8 Verificar Logs

1. Vá em **"Logs"** da aplicação frontend
2. Verifique se o Nginx iniciou corretamente

---

## 🌐 PASSO 5: Configurar Domínios (Opcional)

### 5.1 Configurar Domínio para Backend

1. Na aplicação backend, vá em **"Domains"** ou **"Domínios"**
2. Adicione um domínio (ex: `api.seudominio.com`)
3. Configure SSL/HTTPS (o Coolify pode fazer isso automaticamente com Let's Encrypt)

### 5.2 Configurar Domínio para Frontend

1. Na aplicação frontend, vá em **"Domains"** ou **"Domínios"**
2. Adicione um domínio (ex: `app.seudominio.com` ou `seudominio.com`)
3. Configure SSL/HTTPS

### 5.3 Atualizar CORS no Backend

Se você configurou domínios, atualize a variável de ambiente `CORS_ALLOWED_ORIGINS` no backend:

```env
CORS_ALLOWED_ORIGINS=https://app.seudominio.com,https://seudominio.com
```

⚠️ **IMPORTANTE**: Após atualizar, faça um redeploy do backend.

---

## ✅ PASSO 6: Verificar Funcionamento

### 6.1 Testar Backend

1. Acesse: `http://seu-backend-url:8080/actuator/health`
   - Deve retornar: `{"status":"UP"}`

2. Teste um endpoint da API:
   - `http://seu-backend-url:8080/api/expense-types`
   - Deve retornar uma lista JSON

### 6.2 Testar Frontend

1. Acesse: `http://seu-frontend-url`
2. Verifique se a página carrega
3. Tente fazer login
4. Verifique se as requisições para a API funcionam

### 6.3 Verificar Conexão Frontend → Backend

1. Abra o DevTools do navegador (F12)
2. Vá na aba **"Network"** ou **"Rede"**
3. Tente fazer uma ação que chame a API
4. Verifique se as requisições estão sendo feitas corretamente

---

## 🔍 Troubleshooting

### Problema: Backend não conecta ao banco

**Solução**:
1. Verifique se o PostgreSQL está rodando
2. Verifique se `DATABASE_URL` está correto
3. Verifique se `DATABASE_USERNAME` e `DATABASE_PASSWORD` estão corretos
4. No Coolify, verifique se os serviços estão na mesma rede

### Problema: Frontend não acessa o backend

**Solução**:
1. Verifique se `VITE_API_URL` está correto
2. Se estiverem em domínios diferentes, use a URL completa
3. Verifique CORS no backend
4. Verifique os logs do backend para erros de CORS

### Problema: Build falha

**Solução**:
1. Verifique os logs de build no Coolify
2. Verifique se o `Build Context` está correto (`./backend` ou `./frontend`)
3. Verifique se o Dockerfile está no caminho correto
4. Verifique se todas as dependências estão no repositório

### Problema: Health check falha

**Solução**:
1. Verifique se o Spring Boot Actuator está configurado (já está no projeto)
2. Verifique se a porta está correta
3. Aguarde alguns segundos após o deploy (o app pode demorar para iniciar)

---

## 📝 Checklist Final

Antes de considerar o deploy completo, verifique:

- [ ] PostgreSQL criado e rodando
- [ ] Backend buildado e rodando
- [ ] Frontend buildado e rodando
- [ ] Health checks funcionando
- [ ] Backend conecta ao banco
- [ ] Frontend acessa o backend
- [ ] Domínios configurados (se aplicável)
- [ ] SSL/HTTPS configurado (se aplicável)
- [ ] CORS configurado corretamente
- [ ] Login funciona
- [ ] Aplicação funciona end-to-end

---

## 🎉 Pronto!

Sua aplicação está no ar! 🚀

Para atualizações futuras, basta fazer push no repositório Git e o Coolify pode fazer deploy automático (se configurado).

---

## 📚 Recursos Adicionais

- [Documentação do Coolify](https://coolify.io/docs)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Docker Multi-stage Builds](https://docs.docker.com/build/building/multi-stage/)

---

**Última atualização**: 2025-01-XX

