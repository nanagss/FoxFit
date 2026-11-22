package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.UsuarioDAO;
import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Handler responsável pela autenticação e exibição da página de login.
 */
public class PaginaLoginHandler implements HttpHandler {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void handle(HttpExchange requisicao) throws IOException {
        String metodo = requisicao.getRequestMethod().toUpperCase();

        if ("GET".equals(metodo)) {
            tratarGet(requisicao);
        } else if ("POST".equals(metodo)) {
            tratarPost(requisicao);
        } else {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
        }
    }

    private void tratarGet(HttpExchange requisicao) throws IOException {
        Path arquivo = Path.of("frontend", "pages", "login.html").toAbsolutePath().normalize();

        if (!Files.isRegularFile(arquivo)) {
            WebUtils.responderTexto(requisicao, 404, "Página login.html não encontrada.");
            return;
        }

        String html = Files.readString(arquivo, StandardCharsets.UTF_8);
        Map<String, String> query = WebUtils.lerQuery(requisicao);

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
        WebUtils.responderHtml(requisicao, 200, html);
    }

    private void tratarPost(HttpExchange requisicao) throws IOException {
        Map<String, String> dados = WebUtils.lerFormulario(requisicao);
        String email = dados.getOrDefault("email", "").trim();
        String senha = dados.getOrDefault("senha", "");

        String accept = requisicao.getRequestHeaders().getFirst("Accept");
        boolean querJson = accept != null && accept.contains("application/json");

        if (email.isBlank() || senha.isBlank()) {
            if (querJson) {
                WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"login ou senha incorretos\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/login.html?erro=1");
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
                        "{\"sucesso\":true,\"redirecionar\":\"dashboard.html\",\"usuario\":{\"id\":%d,\"nomeCompleto\":\"%s\",\"cargo\":\"%s\"}}",
                        usuario.getId(),
                        WebUtils.escapar(usuario.getNomeCompleto()),
                        WebUtils.escapar(usuario.getCargo())
                );
                WebUtils.responderJson(requisicao, 200, json);
            } else {
                WebUtils.redirecionar(requisicao, "/pages/dashboard.html");
            }
        } else {
            if (querJson) {
                WebUtils.responderJson(requisicao, 401, "{\"sucesso\":false,\"mensagem\":\"login ou senha incorretos\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/login.html?erro=1");
            }
        }
    }
}
