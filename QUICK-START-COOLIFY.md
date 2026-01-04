# ⚡ Quick Start - Deploy no Coolify

Guia rápido para deploy em 5 minutos.

## 🎯 Resumo Rápido

### 1️⃣ PostgreSQL
```
Services → New Service → PostgreSQL
Nome: manage-house-money-db
Database: manage_house_money
User: postgres
Password: [crie uma senha forte]
```

### 2️⃣ Backend
```
Applications → New Application → Dockerfile
Build Context: ./backend
Dockerfile: ./backend/Dockerfile
Port: 8080

Variáveis de Ambiente:
DATABASE_URL=jdbc:postgresql://manage-house-money-db:5432/manage_house_money
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=[sua senha]
SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGINS=*
```

### 3️⃣ Frontend
```
Applications → New Application → Dockerfile
Build Context: ./frontend
Dockerfile: ./frontend/Dockerfile
Port: 80

Build Args:
VITE_API_URL=/api
```

### 4️⃣ Deploy
```
Clique em "Deploy" em cada aplicação
Aguarde o build completar
Verifique os logs
```

## ✅ Checklist

- [ ] PostgreSQL criado
- [ ] Backend configurado e deployado
- [ ] Frontend configurado e deployado
- [ ] Health checks funcionando
- [ ] Teste de login funcionando

## 📖 Guia Completo

Para instruções detalhadas, veja [COOLIFY-DEPLOY.md](./COOLIFY-DEPLOY.md)

