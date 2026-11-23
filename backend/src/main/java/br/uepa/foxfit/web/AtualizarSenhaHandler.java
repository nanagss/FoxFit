package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.UsuarioDAO;
import br.uepa.foxfit.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * Handler responsável por atualizar a senha do operador no perfil.
 */
public class AtualizarSenhaHandler implements HttpHandler {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void handle(HttpExchange requisicao) throws IOException {
        if (!"POST".equalsIgnoreCase(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        Map<String, String> dados = WebUtils.lerFormulario(requisicao);
        String senhaAtual = dados.getOrDefault("senhaAtual", "").trim();
        String novaSenha = dados.getOrDefault("novaSenha", "").trim();
        String confirmarNovaSenha = dados.getOrDefault("confirmarNovaSenha", "").trim();

        Usuario usuario = WebUtils.obterUsuarioLogado(requisicao);
        if (usuario == null) {
            String idStr = dados.get("idOperador");
            if (idStr != null && !idStr.isBlank()) {
                try {
                    usuario = usuarioDAO.buscarPorId(Integer.parseInt(idStr));
                } catch (Exception ignored) {
                }
            }
        }

        if (usuario == null) {
            WebUtils.responderJson(requisicao, 401, "{\"sucesso\":false,\"mensagem\":\"Usuário não autenticado.\"}");
            return;
        }

        // Valida se a senha atual confere
        if (usuario.getSenhaOriginal() != null && !usuario.getSenhaOriginal().equals(senhaAtual)) {
            WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"Senha atual incorreta.\"}");
            return;
        }

        // Valida se a nova senha e a confirmação são iguais
        if (!confirmarNovaSenha.isBlank() && !novaSenha.equals(confirmarNovaSenha)) {
            WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"A nova senha deve ser a mesma da nova senha confirmação.\"}");
            return;
        }

        if (novaSenha.isBlank()) {
            WebUtils.responderJson(requisicao, 400, "{\"sucesso\":false,\"mensagem\":\"Nova senha não pode ser vazia.\"}");
            return;
        }

        usuarioDAO.atualizarSenha(usuario.getId(), novaSenha);

        WebUtils.responderJson(requisicao, 200, "{\"sucesso\":true,\"mensagem\":\"Senha atualizada com sucesso!\"}");
    }
}
