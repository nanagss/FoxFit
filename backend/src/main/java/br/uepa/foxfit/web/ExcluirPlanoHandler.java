package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.PlanoDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Handler responsável por excluir um plano de assinatura (/planos/excluir).
 */
public class ExcluirPlanoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"POST".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            Map<String, String> dados = WebUtils.lerFormulario(requisicao);
            String idTexto = dados.get("idPlano");

            if (idTexto == null || !idTexto.matches("[1-9][0-9]{0,8}")) {
                WebUtils.responderTexto(requisicao, 400, "ID de plano inválido.");
                return;
            }

            int id = Integer.parseInt(idTexto);
            boolean excluido = new PlanoDAO().excluir(id);

            if (!excluido) {
                WebUtils.responderTexto(requisicao, 404, "Plano não encontrado.");
                return;
            }

            WebUtils.redirecionar(requisicao, "/pages/planos.html");

        } catch (SQLException erro) {
            if ("23503".equals(erro.getSQLState())) {
                WebUtils.responderTexto(
                        requisicao,
                        409,
                        "Não é possível excluir este plano pois existem matrículas vinculadas a ele. Altere o status para Inativo."
                );
            } else {
                erro.printStackTrace();
                WebUtils.responderTexto(requisicao, 500, "Erro interno ao excluir o plano.");
            }
        }
    }
}
