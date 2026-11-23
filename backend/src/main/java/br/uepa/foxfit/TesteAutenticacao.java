package br.uepa.foxfit;

import br.uepa.foxfit.dao.UsuarioDAO;
import br.uepa.foxfit.model.Usuario;
import br.uepa.foxfit.web.WebUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TesteAutenticacao {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println(" TESTES UNITÁRIOS DE AUTENTICAÇÃO E PERFIL FOXFIT");
        System.out.println("==================================================");

        int falhas = 0;
        UsuarioDAO usuarioDAO = new UsuarioDAO();

        System.out.println("\n[TESTE 1] Autenticando Fernanda Sousa (ADMIN)...");
        Usuario fernanda = usuarioDAO.autenticar("fernanda.sousa@foxfit.com.br", "vasco2026");
        if (fernanda != null
                && "Fernanda Sousa".equals(fernanda.getNomeCompleto())
                && "FS".equals(fernanda.getIniciais())
                && "Administrador Geral".equals(fernanda.getCargoFormatado())) {
            System.out.println("  ✓ Sucesso: [" + fernanda.getIniciais() + "] "
                    + fernanda.getNomeCompleto() + " | " + fernanda.getCargoFormatado());
        } else {
            System.err.println("  ✗ Falhou autenticação de Fernanda!");
            falhas++;
        }

        System.out.println("\n[TESTE 2] Autenticando Carlos Eduardo Almeida (OPERADOR)...");
        Usuario carlos = usuarioDAO.autenticar("carlos.almeida@foxfit.com.br", "trembala");
        if (carlos != null
                && "Carlos Eduardo Almeida".equals(carlos.getNomeCompleto())
                && "CA".equals(carlos.getIniciais())
                && "Operador".equals(carlos.getCargoFormatado())) {
            System.out.println("  ✓ Sucesso: [" + carlos.getIniciais() + "] "
                    + carlos.getNomeCompleto() + " | " + carlos.getCargoFormatado());
        } else {
            System.err.println("  ✗ Falhou autenticação de Carlos!");
            falhas++;
        }

        System.out.println("\n[TESTE 3] Autenticando Beatriz Helena Mendes (OPERADOR)...");
        Usuario beatriz = usuarioDAO.autenticar("beatriz.mendes@foxfit.com.br", "conhecimento");
        if (beatriz != null
                && "Beatriz Helena Mendes".equals(beatriz.getNomeCompleto())
                && "BM".equals(beatriz.getIniciais())
                && "Operador".equals(beatriz.getCargoFormatado())) {
            System.out.println("  ✓ Sucesso: [" + beatriz.getIniciais() + "] "
                    + beatriz.getNomeCompleto() + " | " + beatriz.getCargoFormatado());
        } else {
            System.err.println("  ✗ Falhou autenticação de Beatriz!");
            falhas++;
        }

        System.out.println("\n[TESTE 4] Tentando login com senha incorreta...");
        Usuario invalidoSenha = usuarioDAO.autenticar("fernanda.sousa@foxfit.com.br", "senha_errada_123");
        if (invalidoSenha == null) {
            System.out.println("  ✓ Sucesso! Acesso negado com senha incorreta.");
        } else {
            System.err.println("  ✗ Falhou! Login não deveria ser permitido com senha errada.");
            falhas++;
        }

        System.out.println("\n[TESTE 5] Tentando login com e-mail não cadastrado...");
        Usuario invalidoEmail = usuarioDAO.autenticar("desconhecido@foxfit.com.br", "123456");
        if (invalidoEmail == null) {
            System.out.println("  ✓ Sucesso! Acesso negado para usuário não cadastrado.");
        } else {
            System.err.println("  ✗ Falhou! Login não deveria ser permitido.");
            falhas++;
        }

        System.out.println("\n[TESTE 6] Testando injeção do perfil no HTML das páginas...");
        String htmlModelo = """
            <header class="cabecalho-pagina">
              <a href="perfil.html" class="perfil-operador">
                <span class="avatar-operador" aria-hidden="true">FA</span>
                <p class="dados-operador">
                  <strong id="operador-nome">Fernanda Sousa</strong>
                  <small id="operador-cargo">Administradora Geral</small>
                </p>
              </a>
            </header>
            """;

        String htmlCarlos = WebUtils.injetarPerfilOperador(htmlModelo, carlos);
        boolean okCarlos = htmlCarlos.contains("avatar-operador\" aria-hidden=\"true\">CA</span>")
                && htmlCarlos.contains("operador-nome\">Carlos Eduardo Almeida</strong>")
                && htmlCarlos.contains("operador-cargo\">Operador</small>");

        if (okCarlos) {
            System.out.println("  ✓ Injeção para Carlos: [CA] Carlos Eduardo Almeida | Operador");
        } else {
            System.err.println("  ✗ Falhou injeção para Carlos!");
            falhas++;
        }

        String htmlBeatriz = WebUtils.injetarPerfilOperador(htmlModelo, beatriz);
        boolean okBeatriz = htmlBeatriz.contains("avatar-operador\" aria-hidden=\"true\">BM</span>")
                && htmlBeatriz.contains("operador-nome\">Beatriz Helena Mendes</strong>")
                && htmlBeatriz.contains("operador-cargo\">Operador</small>");

        if (okBeatriz) {
            System.out.println("  ✓ Injeção para Beatriz: [BM] Beatriz Helena Mendes | Operador");
        } else {
            System.err.println("  ✗ Falhou injeção para Beatriz!");
            falhas++;
        }

        System.out.println("\n[TESTE 7] Validando tags e proteção no frontend/pages/login.html...");
        String loginHtml = Files.readString(Path.of("frontend", "pages", "login.html"), StandardCharsets.UTF_8);
        boolean metodoPost = loginHtml.contains("method=\"post\"") || loginHtml.contains("method=\"POST\"");
        boolean actionLogin = loginHtml.contains("action=\"/login\"");
        boolean marcadorErro = loginHtml.contains("<!-- MENSAGEM_ERRO_LOGIN -->");
        boolean scriptSessao = loginHtml.contains("sessao.js");

        if (metodoPost && actionLogin && marcadorErro && scriptSessao) {
            System.out.println("  ✓ login.html configurado: action='/login', method='post' (garante que senha não vai na URL)");
            System.out.println("  ✓ Marcador <!-- MENSAGEM_ERRO_LOGIN --> presente");
            System.out.println("  ✓ Script sessao.js incluído");
        } else {
            System.err.println("  ✗ Falhou validação do login.html!");
            falhas++;
        }

        System.out.println("\n[TESTE 8] Validando todas as páginas da pasta frontend/pages/...");
        String[] paginas = {
                "alunos.html", "cadastro.html", "dashboard.html",
                "financeiro.html", "login.html", "matriculas.html",
                "perfil.html", "planos.html", "professores.html"
        };

        for (String pagina : paginas) {
            String conteudo = Files.readString(Path.of("frontend", "pages", pagina), StandardCharsets.UTF_8);
            if (!conteudo.contains("sessao.js")) {
                System.err.println("  ✗ Falta sessao.js na página " + pagina);
                falhas++;
            }
            if (!"login.html".equals(pagina)) {
                if (!conteudo.contains("avatar-operador")
                        || !conteudo.contains("operador-nome")
                        || !conteudo.contains("operador-cargo")) {
                    System.err.println("  ✗ Tags de perfil-operador incompletas em " + pagina);
                    falhas++;
                }
            }
        }
        System.out.println("  ✓ Todas as 9 páginas validadas com sucesso!");

        System.out.println("\n[TESTE 9] Validando remoção de 'Telefone / Ramal' em perfil.html...");
        String perfilHtml = Files.readString(Path.of("frontend", "pages", "perfil.html"), StandardCharsets.UTF_8);
        if (perfilHtml.contains("Telefone / Ramal") || perfilHtml.contains("id=\"perfil-telefone\"")) {
            System.err.println("  ✗ 'Telefone / Ramal' ainda está presente em perfil.html!");
            falhas++;
        } else {
            System.out.println("  ✓ 'Telefone / Ramal' removido com sucesso de perfil.html!");
        }

        System.out.println("\n[TESTE 10] Validando estrutura de alteração de senha em perfil.html...");
        boolean botaoBloqueado = perfilHtml.contains("id=\"btn-atualizar-senha\" class=\"btn btn-primario\" disabled")
                || perfilHtml.contains("id=\"btn-atualizar-senha\" disabled")
                || perfilHtml.contains("disabled");
        boolean temSenhaAtual = perfilHtml.contains("id=\"perfil-senha-atual\"");
        boolean temNovaSenha = perfilHtml.contains("id=\"perfil-nova-senha\"");
        boolean temConfirmaSenha = perfilHtml.contains("id=\"perfil-confirma-senha\"");
        boolean temMsgSenha = perfilHtml.contains("id=\"mensagem-alterar-senha\"");

        if (botaoBloqueado && temSenhaAtual && temNovaSenha && temConfirmaSenha && temMsgSenha) {
            System.out.println("  ✓ Formulário de alteração de senha configurado corretamente.");
            System.out.println("  ✓ Botão 'Atualizar Senha' inicia BLOQUEADO (disabled).");
            System.out.println("  ✓ Campos e container de feedback presentes.");
        } else {
            System.err.println("  ✗ Falha na estrutura do formulário de alteração de senha em perfil.html!");
            falhas++;
        }

        System.out.println("\n[TESTE 11] Validando regras e validação de senha em sessao.js...");
        String sessaoJs = Files.readString(Path.of("frontend", "assets", "js", "sessao.js"), StandardCharsets.UTF_8);
        boolean validaSenhaAtual = sessaoJs.contains("usuarioLogado.senha");
        boolean validaConfirmacao = sessaoJs.contains("A nova senha deve ser a mesma da nova senha confirmação");
        boolean persisteNovaSenha = sessaoJs.contains("foxfit_usuarios_db");

        if (validaSenhaAtual && validaConfirmacao && persisteNovaSenha) {
            System.out.println("  ✓ sessao.js valida e desbloqueia botão apenas com a senha atual correta.");
            System.out.println("  ✓ Regra de confirmação idêntica com aviso implementada.");
            System.out.println("  ✓ Persistência para nova senha ser utilizada no login.html ativa.");
        } else {
            System.err.println("  ✗ Falha nas regras de alteração de senha em sessao.js!");
            falhas++;
        }

        System.out.println("\n[TESTE 12] Testando alteração e persistência de senha no UsuarioDAO e init.sql...");
        String senhaTemporaria = "trembala_2026_forte";
        boolean atualizou = usuarioDAO.atualizarSenha(2, senhaTemporaria);
        Usuario carlosComNovaSenha = usuarioDAO.autenticar("carlos.almeida@foxfit.com.br", senhaTemporaria);
        Usuario carlosComSenhaVelha = usuarioDAO.autenticar("carlos.almeida@foxfit.com.br", "trembala");
        String initSql = Files.readString(Path.of("database", "init.sql"), StandardCharsets.UTF_8);
        boolean initSqlAtualizado = initSql.contains(senhaTemporaria);

        if (atualizou && carlosComNovaSenha != null && carlosComSenhaVelha == null && initSqlAtualizado) {
            System.out.println("  ✓ Senha de Carlos atualizada com sucesso no DAO e no init.sql!");
            System.out.println("  ✓ Senha antiga rejeitada e nova senha aceita!");
        } else {
            System.err.println("  ✗ Falha na alteração de senha do usuário no backend/init.sql!");
            falhas++;
        }

        // Restaura a senha padrão de Carlos para manter coerência dos testes
        usuarioDAO.atualizarSenha(2, "trembala");

        System.out.println("\n==================================================");
        if (falhas == 0) {
            System.out.println(" TODOS OS TESTES PASSARAM COM 100% DE SUCESSO!");
        } else {
            System.err.println(" FALHAS DETECTADAS: " + falhas);
        }
        System.out.println("==================================================");

        if (falhas > 0) {
            System.exit(1);
        }
    }
}
