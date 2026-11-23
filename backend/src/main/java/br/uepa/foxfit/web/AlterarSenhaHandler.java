package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.PerfilDAO;
import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Handler responsável por atualizar a senha de acesso do operador (/perfil/alterar-senha).
 */
public class AlterarSenhaHandler implements HttpHandler {

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

        String senhaAtual = dados.getOrDefault("senhaAtual", "");
        String novaSenha = dados.getOrDefault("novaSenha", "");
        String confirmarNovaSenha = dados.getOrDefault("confirmarNovaSenha", "");

        if (!novaSenha.equals(confirmarNovaSenha)) {
            if (querJson) {
                WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"A confirmação da nova senha não coincide.\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=senha_divergente");
            }
            return;
        }

        try {
            perfilDAO.alterarSenha(idUsuario, senhaAtual, novaSenha);

            if (querJson) {
                WebUtils.responderJson(requisicao, 200, "{\"sucesso\":true,\"mensagem\":\"Senha atualizada com sucesso!\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?sucesso=senha");
            }
        } catch (IllegalArgumentException erro) {
            String msg = erro.getMessage();
            String codigoErro = "senha_invalida";
            if (msg.contains("incorreta")) {
                codigoErro = "senha_atual";
            } else if (msg.contains("mínimo 8")) {
                codigoErro = "senha_curta";
            } else if (msg.contains("diferente da senha atual")) {
                codigoErro = "senha_igual";
            }

            if (querJson) {
                WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"" + WebUtils.escapar(msg) + "\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=" + codigoErro);
            }
        } catch (SQLException erro) {
            erro.printStackTrace();
            if (querJson) {
                WebUtils.responderJson(requisicao, 500, "{\"sucesso\":false,\"mensagem\":\"Erro interno ao atualizar senha no banco.\"}");
            } else {
                WebUtils.redirecionar(requisicao, "/pages/perfil.html?erro=interno");
            }
        }
    }
}
