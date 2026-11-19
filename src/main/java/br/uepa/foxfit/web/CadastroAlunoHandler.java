package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.AlunoDAO;
import br.uepa.foxfit.model.Aluno;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CadastroAlunoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange requisicao) throws IOException {

        if (!"POST".equals(requisicao.getRequestMethod())) {
            responder(requisicao, 405, "Método não permitido.");
            return;
        }

        try {
            String corpo = new String(
                    requisicao.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            Map<String, String> dados = new HashMap<>();

            for (String parte : corpo.split("&")) {
                String[] campo = parte.split("=", 2);

                String chave = URLDecoder.decode(
                        campo[0], StandardCharsets.UTF_8
                );

                String valor = URLDecoder.decode(
                        campo.length > 1 ? campo[1] : "",
                        StandardCharsets.UTF_8
                );

                dados.put(chave, valor.trim());
            }

            Aluno aluno = new Aluno(
                    obrigatorio(dados, "nome"),
                    obrigatorio(dados, "cpf"),
                    LocalDate.parse(obrigatorio(dados, "dataNascimento")),
                    obrigatorio(dados, "telefone"),
                    obrigatorio(dados, "email"),
                    obrigatorio(dados, "endereco")
            );


            AlunoDAO alunoDAO = new AlunoDAO();
            alunoDAO.cadastrar(aluno);

            requisicao.getResponseHeaders().set(
                    "Location", "/pages/alunos.html"
            );

            requisicao.sendResponseHeaders(303, -1);
            requisicao.close();


        } catch (IllegalArgumentException erro) {
            responder(requisicao, 400,
                    "Dados inválidos. Verifique os campos do formulário.");

        } catch (SQLException erro) {
            if ("23505".equals(erro.getSQLState())) {
                responder(requisicao, 409,
                        "Já existe um aluno com este CPF ou e-mail.");
            } else {
                erro.printStackTrace();
                responder(requisicao, 500,
                        "Erro interno ao cadastrar aluno.");
            }
        }
    }

    private static String obrigatorio(
            Map<String, String> dados, String nome) {

        String valor = dados.get(nome);

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Campo obrigatório: " + nome
            );
        }

        return valor;
    }

    private static void responder(
            HttpExchange requisicao,
            int codigo,
            String mensagem) throws IOException {

        byte[] resposta = mensagem.getBytes(StandardCharsets.UTF_8);

        requisicao.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8"
        );

        requisicao.sendResponseHeaders(codigo, resposta.length);

        try (var saida = requisicao.getResponseBody()) {
            saida.write(resposta);
        }
    }
}
