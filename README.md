# Manage House Money

Sistema de gerenciamento financeiro doméstico desenvolvido com Spring Boot (backend) e React (frontend).

## Funcionalidades

- **Login por Email**: Autenticação simples usando apenas email
- **Planilha de Despesas**: Interface tipo Excel para gerenciar despesas mensais
- **Cores por Usuário**: 
  - Lucas (azul) - vyeiralucas@gmail.com
  - Mariana (rosa) - marii_borges@hotmail.com
- **Tipos de Despesa**: 14 tipos pré-configurados (Aluguel, Condomínio, Luz, Água, etc.)
- **Cálculos Automáticos**: Totais por mês, por tipo e geral
- **Histórico Anual**: Visualização e edição de despesas de 2023 em diante

## Estrutura do Projeto

```
ManageHouseMoney/
├── backend/                # Backend Spring Boot
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/managehouse/money/
│   │       │       ├── entity/        # Entidades JPA
│   │       │       ├── repository/     # Repositórios
│   │       │       ├── service/        # Lógica de negócio
│   │       │       ├── controller/     # Controllers REST
│   │       │       ├── dto/            # Data Transfer Objects
│   │       │       └── config/         # Configurações
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml
├── frontend/               # Frontend React
│   ├── src/
│   │   ├── components/     # Componentes React
│   │   ├── context/        # Context API (Auth)
│   │   ├── services/       # Serviços de API
│   │   └── types/          # Tipos TypeScript
│   ├── package.json
│   └── vite.config.ts
├── docker-compose.yml     # Configuração Docker para PostgreSQL
└── README.md
```

## Pré-requisitos

- Java 21
- Maven 3.6+
- Node.js 18+
- PostgreSQL 12+

## Configuração do Banco de Dados

### Opção 1: Usando Docker (Recomendado)

1. Inicie o banco de dados com Docker:
```bash
cd ManageHouseMoney
docker-compose up -d
```

O banco será criado automaticamente. Veja mais detalhes em [DOCKER.md](DOCKER.md).

### Opção 2: PostgreSQL Local

1. Crie um banco de dados PostgreSQL:
```sql
CREATE DATABASE manage_house_money;
```

2. Configure as credenciais no arquivo `backend/src/main/resources/application.properties`

### Dados Iniciais

Ao iniciar a aplicação pela primeira vez, os dados iniciais serão criados automaticamente:
   - 2 usuários (Lucas e Mariana)
   - 14 tipos de despesa

## Como Executar

### Backend (Spring Boot)

```bash
cd ManageHouseMoney/backend
mvn spring-boot:run
```

O backend estará disponível em `http://localhost:8080`

### Frontend (React)

```bash
cd ManageHouseMoney/frontend
npm install
npm run dev
```

O frontend estará disponível em `http://localhost:3000`

**Nota**: Certifique-se de que o backend está rodando antes de iniciar o frontend.

## Como Usar

1. Acesse `http://localhost:3000`
2. Faça login com um dos emails cadastrados:
   - `vyeiralucas@gmail.com` (Lucas - células azuis)
   - `marii_borges@hotmail.com` (Mariana - células rosas)
3. Selecione o ano desejado (começando em 2023)
4. Clique em uma célula da planilha para adicionar/editar uma despesa
5. Preencha o tipo de despesa e o valor
6. As células serão coloridas conforme o usuário que adicionou a despesa

## Tipos de Despesa

- Aluguel
- Condomínio
- Luz
- Água
- Gás
- IPTU
- Internet
- Mercado
- Marmitas
- Saladas
- Diarista
- Viagem
- Carro
- Outros

## Tecnologias

### Backend
- Spring Boot 3.2.4
- Spring Data JPA
- PostgreSQL
- Lombok
- Java 21

### Frontend
- React 18
- TypeScript
- Vite
- Tailwind CSS
- React Router
- Axios
- Context API

## API Endpoints

- `POST /api/auth/login` - Login por email
- `GET /api/expenses?year=2024` - Listar despesas do ano
- `POST /api/expenses` - Criar/atualizar despesa
- `DELETE /api/expenses/{id}` - Deletar despesa
- `GET /api/expense-types` - Listar tipos de despesa

## Desenvolvimento

O projeto está configurado com:
- CORS habilitado para comunicação entre frontend e backend
- Proxy no Vite para redirecionar requisições `/api` para o backend
- Hot reload em ambos os ambientes
- Dados iniciais criados automaticamente na primeira execução

