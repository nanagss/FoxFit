package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.AlunoDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class ExcluirAlunoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao)
            throws IOException {

        if (!"POST".equals(requisicao.getRequestMethod())) {
            responder(requisicao, 405, "Método não permitido.");
            return;
        }

        String corpo = new String(
                requisicao.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        if (!corpo.matches("idAluno=[1-9][0-9]{0,8}")) {
            responder(requisicao, 400, "ID inválido.");
            return;
        }

        int id = Integer.parseInt(
                corpo.substring("idAluno=".length())
        );

        try {
            AlunoDAO dao = new AlunoDAO();

            boolean excluido = dao.excluir(id);

            if (!excluido) {
                responder(requisicao, 404,
                        "Aluno não encontrado.");
                return;
            }

            requisicao.getResponseHeaders().set(
                    "Location", "/pages/alunos.html"
            );

            requisicao.sendResponseHeaders(303, -1);
            requisicao.close();

        } catch (SQLException erro) {

            if ("23503".equals(erro.getSQLState())) {
                responder(requisicao, 409,
                        "Não é possível excluir este aluno: " +
                                "ele possui registros vinculados. " +
                                "Considere alterar seu status para Inativo.");
            } else {
                erro.printStackTrace();
                responder(requisicao, 500,
                        "Erro interno ao excluir aluno.");
            }
        }
    }

    private static void responder(
            HttpExchange requisicao,
            int codigo,
            String mensagem) throws IOException {

        byte[] resposta = mensagem.getBytes(
                StandardCharsets.UTF_8
        );

        requisicao.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        requisicao.sendResponseHeaders(codigo, resposta.length);

        try (var saida = requisicao.getResponseBody()) {
            saida.write(resposta);
        }
    }
}
