package br.uepa.foxfit.controller;

import br.uepa.foxfit.dao.UsuarioDAO;
import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Controller responsável pela autenticação e encerramento de sessão
 * (/pages/login.html, /login e /logout).
 */
public class AuthController extends BaseHandler {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void processarGet(HttpExchange requisicao) throws IOException {
        if ("/logout".equals(obterRota(requisicao))) {
            encerrarSessao(requisicao);
            return;
        }
        exibirPaginaLogin(requisicao);
    }

    @Override
    protected void processarPost(HttpExchange requisicao) throws IOException {
        if ("/logout".equals(obterRota(requisicao))) {
            encerrarSessao(requisicao);
            return;
        }
        autenticar(requisicao);
    }

    private void exibirPaginaLogin(HttpExchange requisicao) throws IOException {
        Path arquivo = Path.of("frontend", "pages", "login.html").toAbsolutePath().normalize();

        if (!Files.isRegularFile(arquivo)) {
            responderTexto(requisicao, 404, "Página login.html não encontrada.");
            return;
        }

        String html = lerPaginaHtml("login.html");
        Map<String, String> query = lerQuery(requisicao);

        boolean temErro = "1".equals(query.get("erro"));
        String mensagemErro = temErro
                ? """
                  <div class="alerta-erro-login" role="alert" style="background-color: #fde8e8; color: #9b1c1c; border: 1px solid #f8b4b4; padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 8px;">
                    <span aria-hidden="true">&#9888;</span>
                    <span>login ou senha incorretos</span>
                  </div>
                  """
                : "";

        html = html.replace("<!-- MENSAGEM_ERRO_LOGIN -->", mensagemErro);
        responderHtml(requisicao, 200, html);
    }

    private void autenticar(HttpExchange requisicao) throws IOException {
        Map<String, String> dados = lerFormulario(requisicao);
        String email = dados.getOrDefault("email", "").trim();
        String senha = dados.getOrDefault("senha", "");

        String accept = requisicao.getRequestHeaders().getFirst("Accept");
        boolean querJson = accept != null && accept.contains("application/json");

        if (email.isBlank() || senha.isBlank()) {
            if (querJson) {
                responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"login ou senha incorretos\"}");
            } else {
                redirecionar(requisicao, "/pages/login.html?erro=1");
            }
            return;
        }

        Usuario usuario = usuarioDAO.autenticar(email, senha);

        if (usuario != null) {
            requisicao.getResponseHeaders().set(
                    "Set-Cookie",
                    "FOXFIT_AUTH_USER=" + usuario.getId() + "; Path=/; HttpOnly; SameSite=Lax"
            );
            if (querJson) {
                String json = String.format(
                        "{\"sucesso\":true,\"redirecionar\":\"dashboard.html\",\"usuario\":{\"id\":%d,\"nomeCompleto\":\"%s\",\"cargo\":\"%s\",\"status\":\"ATIVO\"}}",
                        usuario.getId(),
                        escapar(usuario.getNomeCompleto()),
                        escapar(usuario.getCargo())
                );
                responderJson(requisicao, 200, json);
            } else {
                redirecionar(requisicao, "/pages/dashboard.html");
            }
        } else {
            if (querJson) {
                responderJson(requisicao, 401, "{\"sucesso\":false,\"mensagem\":\"login ou senha incorretos\"}");
            } else {
                redirecionar(requisicao, "/pages/login.html?erro=1");
            }
        }
    }

    private void encerrarSessao(HttpExchange requisicao) throws IOException {
        requisicao.getResponseHeaders().set(
                "Set-Cookie",
                "FOXFIT_AUTH_USER=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
        );
        redirecionar(requisicao, "/pages/login.html");
    }
}
