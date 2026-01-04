# 🔧 Variáveis de Ambiente do Backend

## 📋 Lista Completa para Adicionar no Coolify

Adicione estas variáveis de ambiente na configuração do backend no Coolify:

### ⚙️ Variáveis Obrigatórias

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://manage-house-money-db:5432/manage_house_money
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=sua_senha_aqui
PORT=8080
```

### 🔐 Variáveis de Configuração

```env
DDL_AUTO=update
SHOW_SQL=false
EXCHANGE_RATE=5.42
CORS_ALLOWED_ORIGINS=*
JAVA_OPTS=-Xmx512m -Xms256m
```

### 🤖 Variáveis do OpenAI (Opcional)

```env
OPENAI_API_URL=https://api.openai.com/v1/chat/completions
OPENAI_MODEL=gpt-4o-mini
```

---

## 📝 Como Adicionar no Coolify

1. Na tela de configuração da aplicação backend, procure por **"Environment Variables"** ou **"Variáveis de Ambiente"**
2. Clique em **"+ Add Variable"** ou **"+ Adicionar Variável"**
3. Adicione cada variável uma por uma:

### Exemplo de como adicionar:

**Variável 1:**
- **Key**: `SPRING_PROFILES_ACTIVE`
- **Value**: `prod`

**Variável 2:**
- **Key**: `DATABASE_URL`
- **Value**: `jdbc:postgresql://manage-house-money-db:5432/manage_house_money`

**Variável 3:**
- **Key**: `DATABASE_USERNAME`
- **Value**: `postgres`

**Variável 4:**
- **Key**: `DATABASE_PASSWORD`
- **Value**: `[COLE AQUI A SENHA DO SEU POSTGRESQL]`

E assim por diante...

---

## ⚠️ IMPORTANTE - O que você precisa ajustar:

### 1. DATABASE_URL
Substitua `manage-house-money-db` pelo nome real do seu serviço PostgreSQL no Coolify.

**Como descobrir:**
- Vá na página do serviço PostgreSQL que você criou
- Procure por "Host" ou "Service Name"
- Use esse nome no lugar de `manage-house-money-db`

**Exemplos:**
- Se o serviço se chama `postgres-abc123`, use: `jdbc:postgresql://postgres-abc123:5432/manage_house_money`
- Se o host é `postgres.coolify.local`, use: `jdbc:postgresql://postgres.coolify.local:5432/manage_house_money`

### 2. DATABASE_PASSWORD
Substitua `sua_senha_aqui` pela senha que você criou quando configurou o PostgreSQL.

### 3. CORS_ALLOWED_ORIGINS
- Se você ainda não tem domínio: deixe como `*` (permite todas as origens)
- Se você já tem domínio do frontend: use `https://seu-dominio.com` ou `https://app.seudominio.com`

---

## 📋 Lista Completa (Copie e Cole)

Use esta lista completa para copiar e colar (ajuste os valores marcados com ⚠️):

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://⚠️SEU-POSTGRES-HOST⚠️:5432/manage_house_money
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=⚠️SUA-SENHA-POSTGRES⚠️
DDL_AUTO=update
SHOW_SQL=false
EXCHANGE_RATE=5.42
OPENAI_API_URL=https://api.openai.com/v1/chat/completions
OPENAI_MODEL=gpt-4o-mini
PORT=8080
CORS_ALLOWED_ORIGINS=*
JAVA_OPTS=-Xmx512m -Xms256m
```

---

## ✅ Checklist

Antes de salvar, verifique:

- [ ] `DATABASE_URL` está com o host correto do PostgreSQL
- [ ] `DATABASE_PASSWORD` está com a senha correta
- [ ] `CORS_ALLOWED_ORIGINS` está configurado (use `*` por enquanto se não tiver domínio)
- [ ] Todas as variáveis foram adicionadas

---

## 🎯 Próximo Passo

Após adicionar todas as variáveis:
1. Clique em **"Save"** ou **"Salvar"**
2. Clique em **"Deploy"** ou **"Fazer Deploy"**
3. Aguarde o build completar
4. Verifique os logs para confirmar que está funcionando

---

**Dúvidas?** Veja o guia completo em [COOLIFY-DEPLOY.md](./COOLIFY-DEPLOY.md)

