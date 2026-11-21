package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.PlanoDAO;
import br.uepa.foxfit.model.Plano;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

/**
 * Handler responsável por atualizar um plano existente (/planos/atualizar).
 */
public class AtualizarPlanoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"POST".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            Map<String, String> dados = WebUtils.lerFormulario(requisicao);

            int id = Integer.parseInt(WebUtils.obrigatorio(dados, "idPlano"));

            PlanoDAO dao = new PlanoDAO();
            Plano plano = dao.buscarPorId(id);

            if (plano == null) {
                WebUtils.responderTexto(requisicao, 404, "Plano não encontrado.");
                return;
            }

            String nome = WebUtils.obrigatorio(dados, "nomePlano");
            BigDecimal valorMensal = new BigDecimal(
                    WebUtils.obrigatorio(dados, "valor").replace(",", ".")
            );
            int duracaoMeses = Integer.parseInt(WebUtils.obrigatorio(dados, "duracaoMeses"));
            String beneficios = WebUtils.obrigatorio(dados, "beneficios");
            String status = WebUtils.obrigatorio(dados, "status").toUpperCase(Locale.ROOT);

            if (valorMensal.compareTo(BigDecimal.ZERO) < 0 || duracaoMeses < 1 || duracaoMeses > 36) {
                throw new IllegalArgumentException("Valor ou duração inválidos.");
            }

            if (!status.equals("ATIVO") && !status.equals("INATIVO")) {
                throw new IllegalArgumentException("Status inválido.");
            }

            plano.setNome(nome);
            plano.setValorMensal(valorMensal);
            plano.setDuracaoMeses(duracaoMeses);
            plano.setBeneficios(beneficios);
            plano.setStatus(status);

            boolean atualizado = dao.atualizar(plano);

            if (!atualizado) {
                WebUtils.responderTexto(requisicao, 404, "Plano não encontrado.");
                return;
            }

            WebUtils.redirecionar(requisicao, "/pages/planos.html");

        } catch (IllegalArgumentException erro) {
            WebUtils.responderTexto(requisicao, 400, "Dados inválidos. Verifique os campos do plano.");

        } catch (SQLException erro) {
            erro.printStackTrace();
            WebUtils.responderTexto(requisicao, 500, "Erro interno ao atualizar o plano.");
        }
    }
}
