#  FoxFit — Banco de Dados Local com Docker & PostgreSQL

Guia rápido para os membros da equipe subirem o banco de dados relacional do **FoxFit** localmente via Docker. Com este ambiente, o banco já inicializa com todas as tabelas criadas e dados de teste inseridos automaticamente.

---

##  Pré-requisitos

Antes de começar, certifique-se de ter instalado em sua máquina:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows / macOS) ou Docker Engine + Docker Compose (Linux).
- Git (para clonar o repositório).

> **Atenção:** Certifique-se de que o **Docker Desktop** esteja aberto e em execução antes de rodar os comandos no terminal.

---

##  Como Subir o Banco (Passo a Passo)

### 1. Acessar a pasta do projeto
Abra o terminal (PowerShell, CMD ou Bash) e navegue até a raiz do repositório onde está o arquivo `docker-compose.yml`:

```bash
cd caminho/para/FoxFit-main
```

### 2. Iniciar o contêiner do PostgreSQL
Execute o comando abaixo para baixar a imagem e subir o serviço em segundo plano:

```bash
docker compose up -d
```

*(Caso seu Docker seja uma versão mais antiga, use `docker-compose up -d` com hífen).*

### 3. Verificar o status
Confira se o contêiner está ativo e saudável:

```bash
docker compose ps
```

Você verá o contêiner `foxfit_postgres` com status **Up** e a porta `5432` exposta.

---

##  Credenciais de Conexão

Utilize os dados abaixo para conectar sua aplicação backend ou clientes visuais (DBeaver, pgAdmin, VS Code, DataGrip, Beekeeper Studio):

| Parâmetro | Valor |
| :--- | :--- |
| **Host** | `localhost` |
| **Porta** | `5432` |
| **Banco de Dados (Database)** | `foxfit` |
| **Usuário (User)** | `postgres` |
| **Senha (Password)** | `190207` |

---

##  Estrutura Organizada do Projeto

O projeto está estruturado separando os arquivos de estilo, imagens, páginas HTML e banco de dados:

```text
FoxFit-main/
├── assets/
│   ├── css/
│   │   └── style.css            # Folha de estilos central do sistema
│   └── images/
│       └── logofox.jpeg         # Logotipo oficial da academia
├── database/
│   └── init.sql                 # Script de criação de tabelas e mock data
├── pages/
│   ├── alunos.html              # Gestão de alunos
│   ├── cadastro.html            # Gestão e criação de usuários
│   ├── dashboard.html           # Painel de indicadores operacionais
│   ├── financeiro.html          # Fluxo de mensalidades e pagamentos
│   ├── login.html               # Tela de autenticação
│   ├── matriculas.html          # Controle de matrículas de alunos
│   ├── perfil.html              # Perfil do operador/administrador
│   ├── planos.html              # Planos de assinatura comercial
│   └── professores.html         # Corpo docente e instrutores
├── docker-compose.yml           # Configuração do PostgreSQL local no Docker
├── GUIA_GIT_COMMITS.md          # Guia oficial de commits, branches e credenciais
├── index.html                   # Entrada principal (redireciona para login.html)
└── README.md                    # Documentação do projeto
```

> 📖 **Equipe:** Consulte o arquivo [GUIA_GIT_COMMITS.md](GUIA_GIT_COMMITS.md) para ver o padrão oficial de branches, formato de commits e instruções para envio de Pull Requests.

---

##  Estrutura do Banco de Dados

O arquivo `database/init.sql` inicializa automaticamente 6 tabelas com chaves estrangeiras, restrições e 3 registros fictícios por tabela:

1. **`usuarios`**: Administradores e operadores com permissões no sistema.
2. **`alunos`**: Cadastro de alunos da academia (dados pessoais, contato e status).
3. **`professores`**: Corpo técnico de instrutores (CREF, especialidade, turno e telefone).
4. **`planos`**: Planos comerciais de assinatura (Smart, Fit, VIP) e benefícios inclusos.
5. **`matriculas`**: Contratos vinculando alunos aos planos, vigência e formas de pagamento.
6. **`mensalidades`**: Cobranças financeiras, status de pagamento e datas de vencimento.

---

##  Comandos Úteis do Dia a Dia

### Conectar via terminal interativo (psql)
Para rodar comandos SQL diretamente dentro do contêiner sem instalar nada na sua máquina:

```bash
docker exec -it foxfit_postgres psql -U postgres -d foxfit
```

Dentro do `psql`:
- Listar todas as tabelas: `\dt`
- Detalhes de uma tabela: `\d alunos`
- Consultar registros: `SELECT * FROM alunos;`
- Sair do psql: `\q`

### Ver logs do banco
Se precisar depurar alguma query ou erro:

```bash
docker compose logs -f
```

### Parar o banco de dados
Para pausar os serviços sem perder os dados cadastrados:

```bash
docker compose down
```

###  Resetar o banco do zero
Se você fez testes com dados e quiser voltar ao estado inicial do `database/init.sql`:

```bash
docker compose down -v
docker compose up -d
```

> **Dica:** O `-v` remove o volume do Docker (`postgres_data`), forçando o PostgreSQL a executar novamente o script de inicialização `database/init.sql`.

---

## 🛠️ Ferramentas Visuais Recomendadas

Se você prefere gerenciar o banco através de uma interface visual, recomendamos:
- **DBeaver Community** (Gratuito e completo)
- **Extensão "Database Client" no VS Code** (Permite consultar o PostgreSQL dentro do próprio editor)
- **pgAdmin 4**

---

## Configuração do Java e PostgreSQL

O back-end do FoxFit utiliza Java, JDBC e PostgreSQL.

### Driver JDBC

O driver do PostgreSQL está disponível em:

`lib/postgresql-42.7.14.jar`

Para configurá-lo no IntelliJ IDEA:

1. Abra File → Project Structure.
2. Acesse Modules → Dependencies.
3. Clique em + → JARs or Directories.
4. Selecione o arquivo JAR da pasta `lib`.
5. Confirme a dependência e clique em Apply → OK.

### Conexão com o banco de dados

A classe `Conexao.java` utiliza:

- Banco de dados: `foxfit`
- Servidor: `127.0.0.1`
- Porta padrão: `5432`
- Usuário: `postgres`

Configure estas variáveis de ambiente no IntelliJ,
em Run → Edit Configurations:

- `FOXFIT_DB_PASSWORD`: senha do PostgreSQL.
- `FOXFIT_DB_PORT`: porta do PostgreSQL (opcional).

Se `FOXFIT_DB_PORT` não estiver configurada,
será utilizada a porta `5432`.

Na máquina em que o Docker utiliza a porta `5433`,
configure `FOXFIT_DB_PORT=5433`.

A senha do banco não deve ser incluída no código
nem enviada ao GitHub.

### Execução

1. Inicie o PostgreSQL pelo Docker Compose.
2. Confirme que o banco `foxfit` está disponível.
3. Configure o driver JDBC no IntelliJ.
4. Configure as variáveis de ambiente necessárias.
5. Execute as classes Java utilizando o IntelliJ.