package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.AlunoDAO;
import br.uepa.foxfit.model.Aluno;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

public class ListarAlunosHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"GET".equals(requisicao.getRequestMethod())) {
            responder(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            AlunoDAO dao = new AlunoDAO();
            List<Aluno> alunos = dao.listar();

            StringBuilder resposta = new StringBuilder();

            resposta.append("ALUNOS CADASTRADOS NO FOXFIT\n\n");

            for (Aluno aluno : alunos) {
                resposta.append("ID: ")
                        .append(aluno.getIdAluno())
                        .append(" | Nome: ")
                        .append(aluno.getNomeCompleto())
                        .append(" | Status: ")
                        .append(aluno.getStatus())
                        .append("\n");
            }

            resposta.append("\nTotal: ").append(alunos.size());

            responder(requisicao, 200, resposta.toString());

        } catch (SQLException erro) {
            erro.printStackTrace();
            responder(requisicao, 500, "Erro ao listar alunos.");
        }
    }

    private static void responder(
            HttpExchange requisicao,
            int codigo,
            String mensagem) throws IOException {

        byte[] dados = mensagem.getBytes(StandardCharsets.UTF_8);

        requisicao.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8"
        );

        requisicao.sendResponseHeaders(codigo, dados.length);

        try (var saida = requisicao.getResponseBody()) {
            saida.write(dados);
        }
    }
}
