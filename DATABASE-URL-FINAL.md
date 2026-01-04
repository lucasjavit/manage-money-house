# 🔗 URL do Banco de Dados - Configuração Final

## 📋 Sua URL do PostgreSQL

```
postgres://postgres:0wCtsnUWtuabJax0aFdHrH5te7kDpvtGGWM3BNWuwunPOplPCNIfAq3F7kyUjDp5@r0sc8wok88c8w8gk800ccwwc:5432/postgres
```

## 🔍 Informações Extraídas

- **Host**: `r0sc8wok88c8w8gk800ccwwc`**
- **Port**: `5432`
- **Username**: `postgres`
- **Password**: `0wCtsnUWtuabJax0aFdHrH5te7kDpvtGGWM3BNWuwunPOplPCNIfAq3F7kyUjDp5`
- **Database atual**: `postgres` ⚠️

---

## ⚠️ IMPORTANTE: Database

A URL aponta para o database `postgres`, mas a aplicação precisa do database `manage_house_money`.

### ✅ Opção 1: Criar o database `manage_house_money` (RECOMENDADO)

**Passo a passo:**

1. Conecte ao PostgreSQL (pode ser via terminal do Coolify ou ferramenta externa como pgAdmin, DBeaver, etc.)

2. Execute este comando SQL:
   ```sql
   CREATE DATABASE manage_house_money;
   ```

3. Use esta URL no Coolify:
   ```env
   DATABASE_URL=jdbc:postgresql://r0sc8wok88c8w8gk800ccwwc:5432/manage_house_money
   ```

### ⚠️ Opção 2: Usar o database `postgres` temporariamente

Se não conseguir criar o database agora, pode usar temporariamente:

```env
DATABASE_URL=jdbc:postgresql://r0sc8wok88c8w8gk800ccwwc:5432/postgres
```

⚠️ **Nota**: Isso não é recomendado para produção, mas funciona temporariamente. Depois crie o database correto.

---

## 📋 Variáveis de Ambiente COMPLETAS para o Coolify

### ✅ Se você criou o database `manage_house_money`:

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://r0sc8wok88c8w8gk800ccwwc:5432/manage_house_money
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=0wCtsnUWtuabJax0aFdHrH5te7kDpvtGGWM3BNWuwunPOplPCNIfAq3F7kyUjDp5
DDL_AUTO=update
SHOW_SQL=false
EXCHANGE_RATE=5.42
OPENAI_API_URL=https://api.openai.com/v1/chat/completions
OPENAI_MODEL=gpt-4o-mini
PORT=8080
CORS_ALLOWED_ORIGINS=*
JAVA_OPTS=-Xmx512m -Xms256m
```

### ⚠️ Se você vai usar o database `postgres` temporariamente:

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://r0sc8wok88c8w8gk800ccwwc:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=0wCtsnUWtuabJax0aFdHrH5te7kDpvtGGWM3BNWuwunPOplPCNIfAq3F7kyUjDp5
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

## 🔧 Como Criar o Database `manage_house_money`

### Método 1: Via Terminal do Coolify

1. No Coolify, vá na página do serviço PostgreSQL
2. Procure por "Terminal" ou "Console"
3. Execute:
   ```sql
   CREATE DATABASE manage_house_money;
   ```

### Método 2: Via Ferramenta Externa (pgAdmin, DBeaver, etc.)

1. Conecte usando:
   - **Host**: `r0sc8wok88c8w8gk800ccwwc`
   - **Port**: `5432`
   - **Database**: `postgres`
   - **Username**: `postgres`
   - **Password**: `0wCtsnUWtuabJax0aFdHrH5te7kDpvtGGWM3BNWuwunPOplPCNIfAq3F7kyUjDp5`

2. Execute:
   ```sql
   CREATE DATABASE manage_house_money;
   ```

### Método 3: Via psql (se tiver acesso)

```bash
psql -h r0sc8wok88c8w8gk800ccwwc -p 5432 -U postgres -d postgres
```

Depois execute:
```sql
CREATE DATABASE manage_house_money;
```

---

## ✅ Checklist

- [ ] Database `manage_house_money` criado (ou usar `postgres` temporariamente)
- [ ] `DATABASE_URL` configurada corretamente no Coolify
- [ ] `DATABASE_USERNAME` configurada (`postgres`)
- [ ] `DATABASE_PASSWORD` configurada (sua senha)
- [ ] Todas as outras variáveis adicionadas
- [ ] Salvar e fazer deploy

---

## 🎯 Próximo Passo

1. **Crie o database `manage_house_money`** (recomendado) ou use `postgres` temporariamente
2. **Adicione todas as variáveis** no Coolify com a URL correta
3. **Salve** a configuração
4. **Faça o deploy**
5. **Verifique os logs** para confirmar a conexão

---

## 💡 Dica

O Spring Boot com `DDL_AUTO=update` vai criar as tabelas automaticamente quando conectar, mas o **database precisa existir primeiro**.

