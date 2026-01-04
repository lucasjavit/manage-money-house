# 🖥️ Guia Visual - Tela de Criação no Coolify

## 📋 O que preencher na tela "Create a new Application"

### Para o BACKEND (primeira aplicação):

1. **Repository URL**: ✅ Já está correto
   ```
   https://github.com/lucasjavit/manage-money-house
   ```

2. **Branch**: ✅ Já está correto
   ```
   main
   ```

3. **Build Pack**: ⚠️ **MUDE ISTO!**
   - ❌ Nixpacks (atual)
   - ✅ **Dockerfile** (selecione esta opção)

4. **Base Directory**: ⚠️ **MUDE ISTO!**
   - ❌ `/` (atual)
   - ✅ `./backend` (digite exatamente assim)

5. **Port**: ⚠️ **MUDE ISTO!**
   - ❌ `3000` (atual)
   - ✅ `8080` (porta do backend)

6. **Is it a static site?**: ✅ Já está correto (desmarcado)

7. Clique em **"Continue"**

---

### Para o FRONTEND (segunda aplicação, crie depois):

1. **Repository URL**: ✅
   ```
   https://github.com/lucasjavit/manage-money-house
   ```

2. **Branch**: ✅
   ```
   main
   ```

3. **Build Pack**: ⚠️ **MUDE ISTO!**
   - ✅ **Dockerfile** (selecione esta opção)

4. **Base Directory**: ⚠️ **MUDE ISTO!**
   - ✅ `./frontend` (digite exatamente assim)

5. **Port**: ⚠️ **MUDE ISTO!**
   - ✅ `80` (porta do frontend)

6. **Is it a static site?**: ✅ Já está correto (desmarcado)

7. Clique em **"Continue"**

---

## ⚠️ IMPORTANTE: O que você precisa fazer AGORA

### Na tela atual (Backend):

1. **Mude "Build Pack" de "Nixpacks" para "Dockerfile"**
2. **Mude "Base Directory" de "/" para "./backend"**
3. **Mude "Port" de "3000" para "8080"**
4. Clique em **"Continue"**

### Depois, na próxima tela:

Você precisará configurar as **Variáveis de Ambiente**. Veja a seção abaixo.

---

## 🔧 Variáveis de Ambiente do Backend

Na próxima tela (após clicar Continue), você verá uma seção de "Environment Variables". Adicione:

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://manage-house-money-db:5432/manage_house_money
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=sua_senha_aqui
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
- Substitua `sua_senha_aqui` pela senha do PostgreSQL que você criou
- Substitua `manage-house-money-db` pelo nome do seu serviço PostgreSQL no Coolify

---

## 📝 Resumo Rápido - O que mudar AGORA:

| Campo | Valor Atual | Valor Correto |
|-------|-------------|---------------|
| Build Pack | Nixpacks | **Dockerfile** |
| Base Directory | `/` | **`./backend`** |
| Port | `3000` | **`8080`** |

---

## 🎯 Próximos Passos

1. ✅ Ajuste os 3 campos acima
2. ✅ Clique em "Continue"
3. ✅ Configure as variáveis de ambiente
4. ✅ Faça o deploy
5. ✅ Repita o processo para o frontend

---

**Precisa de ajuda?** Veja o guia completo em [COOLIFY-DEPLOY.md](./COOLIFY-DEPLOY.md)

