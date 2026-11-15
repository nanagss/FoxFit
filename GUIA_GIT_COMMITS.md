#  Guia de Contribuição e Política de Commits — FoxFit

Este guia detalha o fluxo oficial para configuração de credenciais, criação de branches, padronização de commits e envio de código via Pull Request para o repositório da equipe.

---

## 1.  Configuração Inicial de Identidade (Faça uma única vez)

Antes de realizar qualquer commit, configure sua assinatura no Git com o **mesmo e-mail** cadastrado na sua conta do GitHub:

```bash
git config --global user.name "Seu Nome Completo"
git config --global user.email "seu-email-do-github@exemplo.com"
```

> **Atenção:** Se o e-mail configurado for diferente do e-mail do GitHub, seus commits não serão contabilizados para o seu perfil.

---

## 2.  Como Subir e Autenticar suas Credenciais no GitHub

O GitHub não permite mais o uso da senha convencional da conta no terminal. Escolha uma das opções abaixo:

### Opção A: Login via Navegador (Git Credential Manager)
Na primeira vez que você executar o comando `git push`, uma janela do navegador será aberta solicitando autorização. Basta clicar em **"Authorize GitCredentialManager"** e confirmar seu login.

### Opção B: Token de Acesso Pessoal (Personal Access Token - PAT)
Se o terminal solicitar diretamente usuário e senha:
1. No GitHub, clique na sua foto de perfil no topo direito ➔ **Settings**.
2. No menu lateral esquerdo, desça até **Developer settings**.
3. Acesse **Personal access tokens** ➔ **Tokens (classic)**.
4. Clique em **Generate new token** ➔ **Generate new token (classic)**.
5. Em **Note**, digite: `FoxFit Token`.
6. Em **Expiration**, selecione `30 days` ou `90 days`.
7. Marque o escopo obrigatório: **`repo`** (concede acesso para clonar e enviar código).
8. Clique em **Generate token** no final da página.
9. **Copie o token gerado** (começa com `ghp_...`). Quando o Git pedir senha no terminal, **cole o token no lugar da senha**.

---

## 3.  Padrão de Nomenclatura das Branches

>  **REGRA GERAL:** **Nunca commite direto na branch `main`!** A branch principal é protegida.

Todas as modificações devem ser feitas em branches separadas pelo padrão:

$$\text{tipo/numero-da-issue-descricao-curta}$$

* Regras: letras minúsculas, sem acentos e separadas por hífen `-`.
* O número da issue corresponde à tarefa no Taiga / GitHub Projects.

### Exemplos de Branches:
| Situação | Nome da Branch |
| :--- | :--- |
| Nova funcionalidade (Issue #12) | `feat/12-busca-por-artista` |
| Infraestrutura de Banco / Docker (Issue #25) | `feat/25-infra-banco-docker` |
| Correção de bug (Issue #21) | `fix/21-remover-da-playlist` |
| Refatoração de código (Issue #9) | `refactor/9-dividir-music-player` |

---

## 4.  Formato da Mensagem de Commit

Toda mensagem de commit deve seguir rigorosamente a estrutura:

$$\text{tipo(escopo): descrição curta (\#número-da-issue)}$$

* **tipo (obrigatório):** Um dos tipos oficiais da tabela abaixo.
* **escopo (opcional):** `frontend`, `backend` ou `database`.
* **descrição (obrigatório):** O que o commit faz, em letras minúsculas e sem ponto final. Deve completar a frase *"Este commit..."*.
* **(#número) (obrigatório):** Número da issue no Taiga/GitHub Projects.

### Tipos Permitidos:
| Tipo | Quando usar | Exemplo |
| :--- | :--- | :--- |
| **`feat`** | Nova funcionalidade para o usuário | `feat(frontend): adiciona botão de favoritar música (#14)` |
| **`fix`** | Correção de bug / erro | `fix(backend): corrige erro ao remover item (#21)` |
| **`refactor`**| Melhora o código sem alterar o comportamento | `refactor(frontend): divide painel em componentes menores (#9)` |
| **`docs`** | Alteração em documentação (README, manuais) | `docs: atualiza instruções de instalação no README (#5)` |
| **`style`**| Formatação, espaços, indentação (sem alterar lógica)| `style(frontend): padroniza indentação dos componentes (#9)` |
| **`test`** | Criação ou ajuste de testes automatizados | `test(backend): adiciona testes do AlunoService (#17)` |
| **`chore`**| Manutenção, dependências, arquivos de build | `chore(database): adiciona docker-compose e init.sql (#25)` |
| **`ci`** | Alterações em pipelines de CI/CD (GitHub Actions) | `ci: adiciona etapa de lint no workflow (#3)` |

### Exemplos Certos vs. Errados:
*  **Errado:** `ajustes` *(Não diz o que foi alterado)*
*  **Errado:** `Feat: Adicionei a tela de login.` *(Maiúsculas, verbo no passado e com ponto final)*
*  **Errado:** `feat(frontend): adiciona tela de perfil` *(Faltou a issue)*
*  **Errado:** `feat: tela de perfil, correções e testes` *(Várias mudanças em um único commit)*
*  **Correto:** `feat(frontend): adiciona tela de login (#8)`
*  **Correto:** `fix(backend): impede cadastro com e-mail duplicado (#19)`

---

## 5. O Que NUNCA Deve Ir Para o Commit

| Arquivo / Pasta | Por quê? |
| :--- | :--- |
| `.env` ou senhas/chaves | Ficam expostos no histórico do Git de forma pública |
| `node_modules/`, `dist/`, `target/` | Arquivos gerados automaticamente |
| Testes pessoais (`teste.js`, `rascunho.txt`) | Poluem o repositório da equipe |
| `package-lock.json` sem novas dependências | Gera conflitos gigantescos sem motivo |

---

## 6. Fluxo Completo de uma Tarefa (Passo a Passo)

```bash
# 1. Garanta que a main local esteja atualizada
git checkout main
git pull

# 2. Crie a branch específica da sua tarefa
git checkout -b feat/25-infra-banco-docker

# 3. Desenvolva, confira os arquivos alterados e adicione
git status
git add .

# 4. Faça o commit no formato padronizado
git commit -m "feat(database): adiciona docker-compose e script de banco foxfit (#25)"

# 5. Envie a branch para o repositório remoto
git push -u origin feat/25-infra-banco-docker
```

---

## 7. Abrindo o Pull Request no GitHub

1. Acesse o repositório do projeto no GitHub: [github.com/nanagss/FoxFit](https://github.com/nanagss/FoxFit).
2. Clique no botão verde **"Compare & pull request"**.
3. Na descrição do Pull Request, coloque a tag de fechamento da issue:
   ```text
   Closes #25
   ```
4. Solicite a revisão de **1 colega de equipe**.
5. Quando o colega aprovar e os testes passarem, realize o merge na `main`!

---

## 8. Errei a mensagem do commit, e agora?

* **Se você AINDA NÃO deu `git push`:**
  ```bash
  git commit --amend -m "mensagem corrigida (#numero)"
  ```
* **Se você JÁ deu `git push`:**
  Deixe como está e capriche no próximo. **Nunca use `git push --force`**.
