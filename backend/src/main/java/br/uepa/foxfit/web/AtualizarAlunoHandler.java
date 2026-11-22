package br.uepa.foxfit.web;

import br.uepa.foxfit.dao.AlunoDAO;
import br.uepa.foxfit.model.Aluno;
import br.uepa.foxfit.validation.ValidadorAluno;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class AtualizarAlunoHandler implements HttpHandler {

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

            int id = Integer.parseInt(
                    obrigatorio(dados, "idAluno")
            );

            AlunoDAO dao = new AlunoDAO();
            Aluno aluno = dao.buscarPorId(id);

            if (aluno == null) {
                responder(requisicao, 404, "Aluno não encontrado.");
                return;
            }

            // Atualiza os campos recebidos do formulário.
            aluno.setNomeCompleto(obrigatorio(dados, "nome"));
            aluno.setCpf(obrigatorio(dados, "cpf"));
            aluno.setDataNascimento(LocalDate.parse(
                    obrigatorio(dados, "dataNascimento")
            ));
            aluno.setTelefone(obrigatorio(dados, "telefone"));
            aluno.setEmail(obrigatorio(dados, "email"));
            aluno.setEndereco(obrigatorio(dados, "endereco"));


            String status = obrigatorio(dados, "status");

            if (!"ATIVO".equals(status) && !"INATIVO".equals(status)) {
                throw new IllegalArgumentException("Status inválido.");
            }


            aluno.setStatus(status);

            // Valida os dados antes de atualizar no banco.
            ValidadorAluno.validarTamanhos(aluno);

            boolean atualizado = dao.atualizar(aluno);


            if (!atualizado) {
                responder(requisicao, 404, "Aluno não encontrado.");
                return;
            }

            requisicao.getResponseHeaders().set(
                    "Location", "/pages/alunos.html"
            );

            requisicao.sendResponseHeaders(303, -1);
            requisicao.close();

        } catch (DateTimeParseException | IllegalArgumentException erro) {
            responder(requisicao, 400,
                    "Dados inválidos. Verifique o formulário.");

        } catch (SQLException erro) {
            if ("23505".equals(erro.getSQLState())) {
                responder(requisicao, 409,
                        "Já existe outro aluno com este CPF ou e-mail.");
            } else {
                erro.printStackTrace();
                responder(requisicao, 500,
                        "Erro interno ao atualizar aluno.");
            }
        }
    }

    private static String obrigatorio(
            Map<String, String> dados, String campo) {

        String valor = dados.get(campo);

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Campo obrigatório: " + campo
            );
        }

        return valor;
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
