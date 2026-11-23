# FoxFit

**FoxFit** é um sistema web acadêmico de gestão para academias desenvolvido em **Java 21** com servidor HTTP nativo (`com.sun.net.httpserver.HttpServer`), **PostgreSQL 15** via JDBC, Docker Compose e interface em **HTML5/CSS3 puro**.

## Principais funcionalidades

- Autenticação com login/logout por sessão e níveis de acesso (`ADMIN` e `OPERADOR`).
- Perfil do operador com edição de nome/e-mail e alteração de senha.
- Dashboard gerencial com 4 KPIs consolidados e tabela de matrículas recentes.
- Gestão completa de alunos (cadastro, filtros por nome/CPF/status, edição e exclusão protegida).
- Gestão de professores/instrutores (CREF, especialidade, turno, edição e desativação).
- Catálogo de planos de assinatura com cálculo automático do valor total do contrato.
- Controle de matrículas com cálculo de vencimento, geração automática de mensalidade, renovação e cancelamento.
- Fluxo financeiro com indicadores, filtros por status/competência/busca e baixa de pagamentos.
- Gestão de usuários e permissões.

## Estrutura

```text
FoxFit/
├── backend/
│   ├── lib/postgresql-42.7.14.jar        # Driver JDBC do PostgreSQL
│   └── src/
│       ├── main/java/br/uepa/foxfit/
│       │   ├── database/                 # Conexão JDBC (Conexao.java)
│       │   ├── model/                    # Entidades POO (EntidadeBase -> Pessoa -> ...)
│       │   ├── dao/                      # Acesso ao PostgreSQL (BaseDAO + RepositorioCRUD)
│       │   └── controller/               # ServidorFoxFit, BaseHandler, WebUtils e 9 Controllers
│       └── teste/java/br/uepa/foxfit/    # Testes automatizados (TestePOO, TesteAutenticacao, TestePerfil)
├── database/
│   └── init.sql                          # Criação das tabelas e dados iniciais
├── frontend/                             # Telas HTML5, CSS3, sessao.js e logotipo
├── Dockerfile                            # Build da imagem Java 21 (backend + frontend)
└── docker-compose.yml                    # Sobe PostgreSQL 15 + aplicação FoxFit juntos
```

## Como rodar

### Requisitos

- **Docker Desktop** (ou Docker Compose)
- Porta `8080` livre para a aplicação
- Porta `5432` livre para o PostgreSQL local

### Suba tudo com:

```bash
docker compose up -d --build
```

Depois de subir:

- **Sistema (Frontend + Backend):** http://localhost:8080
- **PostgreSQL:** `localhost:5432` (banco `foxfit`, usuário `postgres`)

**Credenciais demo:**

- **Admin:** `fernanda.sousa@foxfit.com.br` / `fernanda26`
- **Operador:** `aryel.jesus@foxfit.com.br` / `trembala`

**Para parar a aplicação:**

```bash
docker compose down
```

**Para recriar o banco local do zero:**

```bash
docker compose down -v
docker compose up -d --build
```