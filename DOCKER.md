# Docker - Manage House Money

Este documento explica como usar Docker para rodar o banco de dados PostgreSQL.

## Pré-requisitos

- Docker instalado
- Docker Compose instalado

## Como Usar

### 1. Iniciar o banco de dados

```bash
cd ManageHouseMoney
docker-compose up -d
```

Isso irá:
- Baixar a imagem do PostgreSQL 16 (se necessário)
- Criar e iniciar o container `manage-house-money-db`
- Criar o banco de dados `manage_house_money` automaticamente (via variável POSTGRES_DB)
- Expor a porta 5432 para conexão local

### 2. Verificar se está rodando

```bash
docker-compose ps
```

Você deve ver o container `manage-house-money-db` com status `Up`.

### 3. Parar o banco de dados

```bash
docker-compose down
```

### 4. Parar e remover volumes (apaga os dados)

```bash
docker-compose down -v
```

### 5. Ver logs

```bash
docker-compose logs -f postgres
```

## Configuração

O banco de dados está configurado com:
- **Usuário**: `postgres`
- **Senha**: `postgres`
- **Banco**: `manage_house_money`
- **Porta**: `5432`

Essas configurações já estão no arquivo `application.properties` do Spring Boot.

## Conectar ao banco

Você pode conectar usando qualquer cliente PostgreSQL:

- **Host**: `localhost`
- **Porta**: `5432`
- **Usuário**: `postgres`
- **Senha**: `postgres`
- **Database**: `manage_house_money`

## Persistência de Dados

Os dados são persistidos em um volume Docker chamado `postgres_data`. Isso significa que mesmo se você parar o container, os dados serão mantidos.

Para remover completamente os dados:

```bash
docker-compose down -v
```

## Troubleshooting

### Porta 5432 já está em uso

Se você já tem um PostgreSQL rodando na porta 5432, você pode:

1. Parar o PostgreSQL local
2. Ou alterar a porta no `docker-compose.yml`:
   ```yaml
   ports:
     - "5433:5432"  # Mude 5433 para outra porta disponível
   ```
   E atualizar o `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5433/manage_house_money
   ```

### Container não inicia

Verifique os logs:
```bash
docker-compose logs postgres
```

### Resetar o banco de dados

```bash
docker-compose down -v
docker-compose up -d
```

Isso irá remover todos os dados e criar um banco novo.

