package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.ProfessorDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class DesativarProfessorHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"POST".equals(requisicao.getRequestMethod())) {
            WebUtils.responderTexto(
                    requisicao, 405, "Método não permitido."
            );
            return;
        }

        try {
            String corpo = new String(
                    requisicao.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String idTexto = null;

            for (String parte : corpo.split("&")) {
                String[] campo = parte.split("=", 2);

                String chave = URLDecoder.decode(
                        campo[0], StandardCharsets.UTF_8
                );

                if ("idProfessor".equals(chave)) {
                    idTexto = URLDecoder.decode(
                            campo.length > 1 ? campo[1] : "",
                            StandardCharsets.UTF_8
                    );
                    break;
                }
            }

            if (idTexto == null || idTexto.isBlank()) {
                throw new IllegalArgumentException("ID obrigatório.");
            }

            int id = Integer.parseInt(idTexto);

            if (id <= 0) {
                throw new IllegalArgumentException("ID inválido.");
            }

            boolean desativado = new ProfessorDAO().desativar(id);

            if (!desativado) {
                WebUtils.responderTexto(
                        requisicao, 404,
                        "Professor não encontrado ou já inativo."
                );
                return;
            }

            requisicao.getResponseHeaders().set(
                    "Location", "/pages/professores.html"
            );

            requisicao.sendResponseHeaders(303, -1);
            requisicao.close();

        } catch (IllegalArgumentException erro) {
            WebUtils.responderTexto(
                    requisicao, 400, "ID de professor inválido."
            );

        } catch (SQLException erro) {
            erro.printStackTrace();

            WebUtils.responderTexto(
                    requisicao, 500,
                    "Erro interno ao desativar professor."
            );
        }
    }
}
