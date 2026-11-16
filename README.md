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

### 2. Configurar as Variáveis de Ambiente (.env)
Por segurança, senhas e configurações locais não são versionadas diretamente no Git. Copie o arquivo de exemplo para criar seu `.env` local:

```bash
cp .env.example .env
```
*(No Windows PowerShell, você também pode usar `Copy-Item .env.example .env`). O arquivo `.env` já vem pré-configurado e é ignorado pelo `.gitignore`.*

### 3. Iniciar o contêiner do PostgreSQL
Execute o comando abaixo para baixar a imagem e subir o serviço em segundo plano:

```bash
docker compose up -d
```

*(Caso seu Docker seja uma versão mais antiga, use `docker-compose up -d` com hífen).*

### 4. Verificar o status
Confira se o contêiner está ativo e saudável:

```bash
docker compose ps
```

Você verá o contêiner `foxfit_postgres` com status **Up** e a porta `5432` exposta.

---

## 🔑 Credenciais de Conexão

Utilize os dados configurados no seu `.env` para conectar sua aplicação backend ou clientes visuais (DBeaver, pgAdmin, VS Code, DataGrip, Beekeeper Studio):

| Parâmetro | Variável (.env) | Valor Padrão Local |
| :--- | :--- | :--- |
| **Host** | — | `localhost` |
| **Porta** | `POSTGRES_PORT` | `5432` |
| **Banco de Dados (Database)** | `POSTGRES_DB` | `foxfit` |
| **Usuário (User)** | `POSTGRES_USER` | `postgres` |
| **Senha (Password)** | `POSTGRES_PASSWORD` | *(definida no seu `.env` local)* |

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
├── .env.example                 # Modelo de variáveis de ambiente do banco
├── .gitignore                   # Arquivos ignorados pelo Git (inclui o .env)
├── docker-compose.yml           # Configuração do PostgreSQL local no Docker
├── GUIA_GIT_COMMITS.md          # Guia oficial de commits, branches e credenciais
├── index.html                   # Entrada principal (redireciona para login.html)
└── README.md                    # Documentação do projeto
```

> 📖 **Equipe:** Consulte o arquivo [GUIA_GIT_COMMITS.md](GUIA_GIT_COMMITS.md) para ver o padrão oficial de branches, formato de commits e instruções para envio de Pull Requests.

---

## 🗄️ Estrutura do Banco de Dados

O arquivo `database/init.sql` inicializa automaticamente 6 tabelas com chaves estrangeiras, restrições e 3 registros fictícios por tabela:

1. **`usuarios`**: Usuários do sistema (id_usuario, nome, email, senha_hash, cargo, foto_perfil, ativo).
2. **`alunos`**: Cadastro de alunos da academia (id_aluno, nome_completo, cpf, contato, endereço, objetivo e status).
3. **`professores`**: Corpo técnico de instrutores (id_professor, nome_completo, cpf, cref, especialidade, turno, salário).
4. **`planos`**: Planos comerciais de assinatura (id_plano, nome_plano, duração, valor mensal, taxa de adesão).
5. **`matriculas`**: Contratos vinculando aluno, plano e professor responsável (id_matricula, vigência, dia vencimento, status).
6. **`financeiro`**: Mensalidades, despesas e fluxo de caixa (id_lancamento, tipo receita/despesa, categoria, valor, pagamento).

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
