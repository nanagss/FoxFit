package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.PerfilDAO;
import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Handler responsável por atualizar os dados funcionais do operador (/perfil/atualizar).
 */
public class AtualizarPerfilHandler implements HttpHandler {

    private final PerfilDAO perfilDAO = new PerfilDAO();

    @Override
    public void handle(HttpExchange requisicao) throws IOException {
        if (!"POST".equalsIgnoreCase(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        Map<String, String> dados = WebUtils.lerFormulario(requisicao);
        String accept = requisicao.getRequestHeaders().getFirst("Accept");
        boolean querJson = accept != null && accept.contains("application/json");

        Usuario usuarioLogado = WebUtils.obterUsuarioLogado(requisicao);
        int idUsuario = (usuarioLogado != null) ? usuarioLogado.getId() : 1;

        if (dados.containsKey("idOperador")) {
            try {
                idUsuario = Integer.parseInt(dados.get("idOperador"));
            } catch (NumberFormatException ignored) {
            }
        }

        String nomeCompleto = dados.getOrDefault("nomeCompleto", "").trim();
        String email = dados.getOrDefault("email", "").trim();

        try {
            perfilDAO.atualizarDados(idUsuario, nomeCompleto, email);

            if (querJson) {
                Usuario u = perfilDAO.buscarPorId(idUsuario);
                String json = String.format(
                        "{\"sucesso\":true,\"mensagem\":\"Alterações do perfil salvas com sucesso!\",\"usuario\":{\"id\":%d,\"nomeCompleto\":\"%s\",\"email\":\"%s\"}}",
                        u.getId(),
                        WebUtils.escapar(u.getNomeCompleto()),
                        WebUtils.escapar(u.getEmail())
                );
                WebUtils.responderJson(requisicao, 200, json);
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?sucesso=perfil");
            }
        } catch (IllegalArgumentException erro) {
            String msg = erro.getMessage();
            String codigoErro = "dados_invalidos";
            if (msg.contains("já está em uso")) {
                codigoErro = "email_duplicado";
            } else if (msg.contains("e-mail corporativo válido")) {
                codigoErro = "email_invalido";
            } else if (msg.contains("nome completo")) {
                codigoErro = "nome_invalido";
            }

            if (querJson) {
                WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"" + WebUtils.escapar(msg) + "\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=" + codigoErro);
            }
        } catch (SQLException erro) {
            erro.printStackTrace();
            if (querJson) {
                WebUtils.responderJson(requisicao, 500, "{\"sucesso\":false,\"mensagem\":\"Erro interno ao atualizar perfil no banco.\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=interno");
            }
        }
    }
}
