package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.PlanoDAO;
import br.uepa.foxfit.model.Plano;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

/**
 * Handler responsável por receber o POST de cadastro de novos planos (/planos/cadastrar).
 */
public class CadastroPlanoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"POST".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            Map<String, String> dados = WebUtils.lerFormulario(requisicao);

            String nome = WebUtils.obrigatorio(dados, "nomePlano");
            BigDecimal valorMensal = new BigDecimal(
                    WebUtils.obrigatorio(dados, "valor").replace(",", ".")
            );
            int duracaoMeses = Integer.parseInt(WebUtils.obrigatorio(dados, "duracaoMeses"));
            String beneficios = WebUtils.obrigatorio(dados, "beneficios");

            if (valorMensal.compareTo(BigDecimal.ZERO) < 0 || duracaoMeses < 1 || duracaoMeses > 36) {
                throw new IllegalArgumentException("Valor ou duração fora do intervalo permitido.");
            }

            Plano plano = new Plano(nome, duracaoMeses, valorMensal, beneficios);

            new PlanoDAO().cadastrar(plano);

            WebUtils.redirecionar(requisicao, "/pages/planos.html");

        } catch (IllegalArgumentException erro) {
            WebUtils.responderTexto(requisicao, 400, "Dados inválidos. Verifique os campos do formulário de plano.");

        } catch (SQLException erro) {
            if ("23505".equals(erro.getSQLState())) {
                WebUtils.responderTexto(requisicao, 409, "Já existe um plano cadastrado com este código.");
            } else {
                erro.printStackTrace();
                WebUtils.responderTexto(requisicao, 500, "Erro interno ao cadastrar o plano.");
            }
        }
    }
}
