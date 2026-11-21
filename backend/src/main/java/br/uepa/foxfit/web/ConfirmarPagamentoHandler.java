package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.FinanceiroDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Handler responsável por dar baixa / confirmar pagamento de uma mensalidade (/financeiro/pagar).
 */
public class ConfirmarPagamentoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"POST".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            Map<String, String> dados = WebUtils.lerFormulario(requisicao);
            String idTexto = dados.get("idMensalidade");

            if (idTexto == null || !idTexto.matches("[1-9][0-9]{0,8}")) {
                WebUtils.responderTexto(requisicao, 400, "ID de mensalidade inválido.");
                return;
            }

            int id = Integer.parseInt(idTexto);
            boolean confirmado = new FinanceiroDAO().confirmarPagamento(id);

            if (!confirmado) {
                WebUtils.responderTexto(requisicao, 404, "Mensalidade não encontrada.");
                return;
            }

            WebUtils.redirecionar(requisicao, "/pages/financeiro.html");

        } catch (SQLException erro) {
            erro.printStackTrace();
            WebUtils.responderTexto(requisicao, 500, "Erro interno ao confirmar o pagamento.");
        }
    }
}
