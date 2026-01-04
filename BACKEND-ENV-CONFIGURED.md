# ✅ Variáveis de Ambiente Configuradas

## 🔍 Informações Extraídas da URL do PostgreSQL

Da URL fornecida:
```
postgres://postgres:0wCtsnUWtuabJax0aFdHrH5te7kDpvtGGWM3BNWuwunPOplPCNIfAq3F7kyUjDp5@r0sc8wok88c8w8gk800ccwwc:5432/postgres
```

**Informações extraídas:**
- **Host**: `r0sc8wok88c8w8gk800ccwwc`
- **Port**: `5432`
- **Username**: `postgres`
- **Password**: `0wCtsnUWtuabJax0aFdHrH5te7kDpvtGGWM3BNWuwunPOplPCNIfAq3F7kyUjDp5`
- **Database atual**: `postgres`

---

## ⚠️ IMPORTANTE: Database

A URL aponta para o database `postgres`, mas a aplicação precisa do database `manage_house_money`.

### Opção 1: Criar o database `manage_house_money` (Recomendado)

Você precisa criar o database `manage_house_money` no PostgreSQL. O Spring Boot pode criar automaticamente se você usar `DDL_AUTO=update`, mas o database precisa existir primeiro.

**Como criar:**
1. Conecte ao PostgreSQL (pode ser via Coolify ou ferramenta externa)
2. Execute: `CREATE DATABASE manage_house_money;`

### Opção 2: Usar o database `postgres` (Temporário)

Se não conseguir criar o database agora, pode usar `postgres` temporariamente, mas não é recomendado para produção.

---

## 📋 Variáveis de Ambiente Configuradas

### ✅ Use estas variáveis no Coolify:

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

---

## 🔧 Se o database `manage_house_money` não existir

### Opção A: Criar via SQL

Conecte ao PostgreSQL e execute:
```sql
CREATE DATABASE manage_house_money;
```

### Opção B: Usar `postgres` temporariamente

Se não conseguir criar agora, use esta URL temporariamente:
```env
DATABASE_URL=jdbc:postgresql://r0sc8wok88c8w8gk800ccwwc:5432/postgres
```

⚠️ **Nota**: Depois, crie o database correto e atualize a variável.

---

## ✅ Checklist

- [ ] Database `manage_house_money` criado (ou usar `postgres` temporariamente)
- [ ] Todas as variáveis adicionadas no Coolify
- [ ] `DATABASE_URL` configurada corretamente
- [ ] `DATABASE_PASSWORD` configurada corretamente
- [ ] Salvar e fazer deploy

---

## 🎯 Próximo Passo

1. Adicione todas as variáveis no Coolify
2. Se o database `manage_house_money` não existir, crie-o primeiro
3. Salve a configuração
4. Faça o deploy
5. Verifique os logs para confirmar a conexão

